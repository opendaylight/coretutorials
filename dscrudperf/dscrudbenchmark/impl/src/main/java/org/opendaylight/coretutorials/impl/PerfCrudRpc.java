package org.opendaylight.coretutorials.impl;

import static java.lang.Thread.sleep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.DoCrudInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.DoCrudInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.DoCrudOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.DscrudService;

import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfCrudRpc {

    private static final Logger LOG = LoggerFactory.getLogger(PerfCrudRpc.class);
    private DscrudService dscrudService;
    public long createsPerSec, retrievesPerSec, crudsPerSec, deletesPerSec;
    private ExecutorService executor;
    private Integer nextQueueId = 0;
    private Integer numSuccessful = 0;
    private Integer numComplete = 0;
    private static final int innerElements = 30;

    private synchronized Integer getNextQ() {
        return nextQueueId++;
    }
    private synchronized void incNumSuccessful() {
        ++numSuccessful;
    }
    private synchronized void incNumComplete() {
        ++numComplete;
    }

    public PerfCrudRpc(DscrudService dscrudService) {
        this.dscrudService = dscrudService;
        executor = null;
    }

    private ArrayList<ArrayList<Integer>> resourceIdQueues;
    private void buildResourceIdQueues(int numQueues, int numResources) {
        resourceIdQueues = new ArrayList<ArrayList<Integer>>(numQueues);
        for (int i = 0; i < numQueues; ++i) {
            ArrayList<Integer> resourceArray = new ArrayList<Integer>(numResources/numQueues+1);
            resourceIdQueues.add(resourceArray);
        }
        for (int i = 0; i < numResources; i++) {
            int q = i%numQueues;
            ArrayList<Integer> resourceArray = resourceIdQueues.get(q);
            resourceArray.add(i+1);
        }
    }

    /**
     * Run a performance test that create 'numResources' and records how long it took, then it will retrieve
     * each of the created resources, then update each of the resources, then finally delete each of the originally
     * created resources.  This test creates resources under a special cseBase that has been specially designed
     * to hold resoources for this performance test.  The other cseBase's in the system are unaffected.
     *
     * I was thinking that when one deploys this feature, they might want to have some notion of how well it will
     * perform in their environment.  Conceivably, an administration/diagnostic function could be implemented that
     * would invoke the rpc with some number of resources, and the operator could know what performance to expect.
     * @param numResources
     * @return
     */
    public boolean runPerfTest(int numResources, int numThreads) {

        int totalSuccessful = 0;
        totalSuccessful += createTest(numResources, numThreads);
        totalSuccessful += retrieveTest(numResources, numThreads);
        totalSuccessful += deleteTest(numResources, numThreads);

        return (numResources*3) == totalSuccessful;
    }


    private int createTest(int numResources, final int numThreads) {

        long startTime, endTime, delta;
        nextQueueId = 0;
        numComplete = 0;
        numSuccessful = 0;

        executor = Executors.newFixedThreadPool(numThreads);

        buildResourceIdQueues(numThreads, numResources);

        startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runCreateTests(getNextQ());
                }
            });
        }

        while (numComplete != numResources) {
            try {
                sleep(1, 0);
            } catch (InterruptedException e) {
                LOG.error("sleep error: {}", e);
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        createsPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to create ... num/total: {}/{}, delta: {}ns, ops/s: {}", numSuccessful, numResources, delta, createsPerSec);

        executor.shutdown(); // kill the threads

        return numSuccessful;
    }

    private void runCreateTests(Integer q) {
        ArrayList<Integer> resourceIdArray = resourceIdQueues.get(q);
        for (Integer resourceId : resourceIdArray) {
            createOneTest(resourceId);
            incNumComplete();
        }
    }

    private void runRetrieveTests(Integer q) {
        ArrayList<Integer> resourceIdArray = resourceIdQueues.get(q);
        for (Integer resourceId : resourceIdArray) {
            retrieveOneTest(resourceId);
            incNumComplete();
        }
    }

    private void runDeleteTests(Integer q) {
        ArrayList<Integer> resourceIdArray = resourceIdQueues.get(q);
        for (Integer resourceId : resourceIdArray) {
            deleteOneTest(resourceId);
            incNumComplete();
        }
    }

    private boolean createOneTest(Integer resourceId) {

        DoCrudInput input = new DoCrudInputBuilder()
                .setOperation(DoCrudInput.Operation.CREATE)
                .setOuterElementId((long)resourceId)
                .setInnerElements((long)innerElements)
                .build();
        boolean success = true;
        try {
            RpcResult<DoCrudOutput> rpcResult = dscrudService.doCrud(input).get();
            success = rpcResult.getResult().getTxError() == 0 &&
                    rpcResult.getResult().getVerifyError() == 0;
            if (!success) {
                LOG.error("CREATE: resourceId: {}, txError: {}, verifyError: {}",
                        resourceId, rpcResult.getResult().getTxError(), rpcResult.getResult().getVerifyError());
            } else {
                incNumSuccessful();
            }
        } catch (Exception e) {
            LOG.error("dscrudService: RPC exception");
        }
        return success;
    }

    private boolean retrieveOneTest(Integer resourceId) {
        DoCrudInput input = new DoCrudInputBuilder()
                .setOperation(DoCrudInput.Operation.READ)
                .setOuterElementId((long)resourceId)
                .setInnerElements((long)innerElements)
                .build();
        boolean success = true;
        try {
            RpcResult<DoCrudOutput> rpcResult = dscrudService.doCrud(input).get();
            success = rpcResult.getResult().getTxError() == 0 &&
                    rpcResult.getResult().getVerifyError() == 0;
            if (!success) {
                LOG.error("RETRIEVE: resourceId: {}, txError: {}, verifyError: {}",
                        resourceId, rpcResult.getResult().getTxError(), rpcResult.getResult().getVerifyError());
            } else {
                incNumSuccessful();
            }
        } catch (Exception e) {
            LOG.error("dscrudService: RPC exception");
        }
        return success;
    }


    private int retrieveTest(int numResources, final int numThreads) {

        long startTime, endTime, delta;
        nextQueueId = 0;
        numComplete = 0;
        numSuccessful = 0;

        executor = Executors.newFixedThreadPool(numThreads);

        buildResourceIdQueues(numThreads, numResources);

        startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runRetrieveTests(getNextQ());
                }
            });
        }
        while (numComplete != numResources) {
            try {
                sleep(1, 0);
            } catch (InterruptedException e) {
                LOG.error("sleep error: {}", e);
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        retrievesPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to retrieve ... num/total: {}/{}, delta: {}ns, ops/s: {}", numSuccessful, numResources, delta, retrievesPerSec);

        executor.shutdown(); // kill the threads

        return numSuccessful;
    }

    private boolean deleteOneTest(Integer resourceId) {
        DoCrudInput input = new DoCrudInputBuilder()
                .setOperation(DoCrudInput.Operation.DELETE)
                .setOuterElementId((long)resourceId)
                .setInnerElements((long)innerElements)
                .build();
        boolean success = true;
        try {
            RpcResult<DoCrudOutput> rpcResult = dscrudService.doCrud(input).get();
            success = rpcResult.getResult().getTxError() == 0 &&
                    rpcResult.getResult().getVerifyError() == 0;
            if (!success) {
                LOG.error("DELETE: resourceId: {}, txError: {}, verifyError: {}",
                        resourceId, rpcResult.getResult().getTxError(), rpcResult.getResult().getVerifyError());
            } else {
                incNumSuccessful();
            }
        } catch (Exception e) {
            LOG.error("dscrudService: RPC exception");
        }
        return success;

    }

    private int deleteTest(int numResources, final int numThreads) {

        long startTime, endTime, delta;
        nextQueueId = 0;
        numComplete = 0;
        numSuccessful = 0;

        executor = Executors.newFixedThreadPool(numThreads);

        buildResourceIdQueues(numThreads, numResources);

        startTime = System.nanoTime();

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    runDeleteTests(getNextQ());
                }
            });
        }
        while (numComplete != numResources) {
            try {
                sleep(1, 0);
            } catch (InterruptedException e) {
                LOG.error("sleep error: {}", e);
            }
        }
        endTime = System.nanoTime();
        delta = (endTime-startTime);
        deletesPerSec = nPerSecond(numResources, delta);
        LOG.info("Time to delete ... num/total: {}/{}, delta: {}ns, ops/s: {}", numSuccessful, numResources, delta, deletesPerSec);

        executor.shutdown(); // kill the threads

        return numSuccessful;
    }

    private long nPerSecond(int num, long delta) {

        double secondsTotal = (double)delta / (double)1000000000;
        return (long) (((double)num / secondsTotal));


    }
}
