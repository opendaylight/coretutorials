package org.opendaylight.dsbenchmark;

import java.util.*;
import java.util.concurrent.*;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.DsbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatus.ExecStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;

/**
 * Created by ragbhat on 2/3/15.
 */
public class DsbenchmarkExecutor {
    private static long poolSize;
    public DsbenchmarkExecutor(long size) {
        poolSize = size;
    }

    public StartTestOutput execute(StartTestInput input, DataBroker dataBroker, DOMDataBroker domDataBroker) {
        StartTestOutputBuilder output =  new StartTestOutputBuilder();
        long startTime, endTime;

        //Initialize the Output
        output.setListBuildTime(0L);
        output.setTxOk(0L);
        output.setTxError(0L);

        //Setup a theread pool
        ExecutorService executorPool=Executors.newFixedThreadPool((int) poolSize);
        startTime = System.nanoTime();

        //Add Tasks to thread pool. Each thread works on its own section of the list
        //Outer list will have OuterElements * Thread Count number of elements in total
        Collection<DsbenchmarkExecutorTask> collection = new ArrayList<DsbenchmarkExecutorTask>( );
        for(int i=0; i< poolSize; i++){
            DsbenchmarkExecutorTask task = new DsbenchmarkExecutorTask(input, i * input.getOuterElements(), dataBroker, domDataBroker);
            collection.add(task);
        }
        try {
            List< Future< StartTestOutput > > list = executorPool.invokeAll(collection);
            //Get the output from each thread and combine the results
            for(Future< StartTestOutput > future : list){
                StartTestOutput tout = future.get();
                output.setListBuildTime(output.getListBuildTime()+tout.getListBuildTime());
                output.setTxOk(output.getTxOk()+ tout.getTxOk());
                output.setTxError(output.getTxError() + tout.getTxError());
            }
            endTime = System.nanoTime();

            output.setStatus(StartTestOutput.Status.OK);
            output.setExecTime((endTime - startTime) / 1000000);

        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            executorPool.shutdown();
        }
        return output.build();
    }
}

