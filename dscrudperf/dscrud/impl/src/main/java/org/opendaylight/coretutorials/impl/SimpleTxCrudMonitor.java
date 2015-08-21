package org.opendaylight.coretutorials.impl;


import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Monitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.DoCrudInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.TestExecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.test.exec.OuterListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.test.exec.outer.list.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.LoggerFactory;

/**
 * This class ensures that reads can run concurrently but writes are semaphore protected via the guava monitor class
 * The semaphore is logically around the whole write/create logic as in actual implementations, decisions might be
 * made as a logical transaction so I have the semaphore at a logically higher layer (see doOper method below).
 * Dscrudbenchmark calls dscrud from many concurrent threads emulating a jetty server for example where each jetty
 * thread will block on a dscrud RPC.
 */
public class SimpleTxCrudMonitor {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SimpleTxCrudMonitor.class);

    private DataBroker dataBroker;
    private Monitor crudMonitor;
    public int txOk, txError, verifyOk, verifyError;

    public SimpleTxCrudMonitor(DataBroker dataBroker, Monitor crudMonitor) {

        this.dataBroker = dataBroker;
        this.crudMonitor = crudMonitor;

        if (readOuterList(0) == null) {
            createOuterList(0, 0);
        }

        LOG.info("Created SimpletxCrud");
    }

    static private List<InnerList> buildInnerList( int index, int elements ) {
        List<InnerList> innerList = new ArrayList<InnerList>( elements );

        final String itemStr = "Item-" + String.valueOf(index) + "-";
        for( int i = 0; i < elements; i++ ) {
            innerList.add(new InnerListBuilder()
                    .setKey( new InnerListKey( String.valueOf( i ) ) )
                    .setId(String.valueOf(i))
                    .setValue( itemStr + String.valueOf( i ) )
                    .build());
        }

        return innerList;
    }

    private OuterList readOuterList(int j) {

        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();

        InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                .child(OuterList.class, new OuterListKey(String.valueOf( j )));
        OuterList outerList = null;
        Optional<OuterList> optionalDataObject;
        CheckedFuture<Optional<OuterList>, ReadFailedException> submitFuture = tx.read(LogicalDatastoreType.OPERATIONAL, iid);
        try {
            optionalDataObject = submitFuture.checkedGet();
            if (optionalDataObject != null && optionalDataObject.isPresent()) {
                outerList = optionalDataObject.get();
            }
        } catch (ReadFailedException e) {
            LOG.error("failed to ....", e);
        } finally {
            tx.close();
        }
        //LOG.info("read: outer: {}", j);
        return outerList;
    }

    private InnerList readInnerList(int j, int k) {


        ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();

        InstanceIdentifier<InnerList> iid = InstanceIdentifier.create(TestExec.class)
                .child(OuterList.class, new OuterListKey(String.valueOf(j)))
                .child(InnerList.class, new InnerListKey(String.valueOf( k )));
        InnerList innerList = null;

        /**
         * Read without the need for a future
         */
        try {
            Optional<InnerList> optionalDataObject = tx.read(LogicalDatastoreType.OPERATIONAL, iid).checkedGet();
            if (optionalDataObject != null && optionalDataObject.isPresent()) {
                innerList = optionalDataObject.get();
                if (!innerList.getId().contentEquals(String.valueOf( k )) || !innerList.getValue().contentEquals(String.valueOf(k))) {
                    return null;
                }
            }
        }  catch (ReadFailedException e) {
            LOG.warn("failed to ....", e);
        } finally {
            tx.close();
        }

        /**
         * Not using futures as the concurrency is done via the threads that call the RPC.  This emulates many jetty threads
         * calling a jetty server who in turn block on the rpc call which runs in the same thread context as the calling
         * thread.
         */
        /*
        Optional<InnerList> optionalDataObject;
        CheckedFuture<Optional<InnerList>, ReadFailedException> submitFuture = tx.read(LogicalDatastoreType.OPERATIONAL, iid);
        try {
            optionalDataObject = submitFuture.checkedGet();
            if (optionalDataObject != null && optionalDataObject.isPresent()) {
                innerList = optionalDataObject.get();
                if (!innerList.getId().contentEquals(String.valueOf( k )) || !innerList.getValue().contentEquals(String.valueOf(k))) {
                    return null;
                }
            }
        } catch (ReadFailedException e) {
            LOG.warn("failed to ....", e);
        } finally {
            tx.close();
        }
        */
        //LOG.info("read: outer: {}, inner: {}", j, k);

        return innerList;
    }

    private  void createOuterList(int j, int k) {

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        InstanceIdentifier<OuterList> ol_iid;
        List<InnerList> innerListList;
        if (k == 0) {
            innerListList = Collections.<InnerList>emptyList();
            TestExec data = new TestExecBuilder()
                    .setOuterList(Collections.<OuterList>emptyList())
                    .build();
            InstanceIdentifier<TestExec> TEST_EXEC_IID = InstanceIdentifier.builder(TestExec.class).build();
            tx.put(LogicalDatastoreType.OPERATIONAL, TEST_EXEC_IID, data);

        } else {
            innerListList = this.buildInnerList(j, k);
        }

        OuterList outerList = new OuterListBuilder()
                .setId(String.valueOf( j ))
                .setInnerList(innerListList)
                .setKey(new OuterListKey(String.valueOf( j )))
                .setBogus1(Collections.<Bogus1>emptyList())
                .setName(String.valueOf( j ))
                .setBogus2(Collections.<Bogus2>emptyList())
                .build();
        ol_iid = InstanceIdentifier.create(TestExec.class)
                .child(OuterList.class, outerList.getKey());
        tx.put(LogicalDatastoreType.OPERATIONAL, ol_iid, outerList);

        if (j != 0) {
            // add an inner list for outerList 0
            InnerList innerList = new InnerListBuilder()
                    .setKey(new InnerListKey(String.valueOf(j)))
                    .setId(String.valueOf(j))
                    .setValue(String.valueOf(j))
                    .build();
            InstanceIdentifier<InnerList> il_iid = InstanceIdentifier.create(TestExec.class)
                    .child(OuterList.class, new OuterListKey(String.valueOf(0)))
                    .child(InnerList.class, innerList.getKey());
            tx.put(LogicalDatastoreType.OPERATIONAL, il_iid, innerList);
        }
        try {
            tx.submit().checkedGet();
            txOk++;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
            txError++;
        }
        //LOG.info("write: outer: {}, inner: {}", j, k);

    }

    private void deleteOuterList(int j) {

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        InstanceIdentifier<OuterList> ol_iid;


        ol_iid = InstanceIdentifier.create(TestExec.class)
                .child(OuterList.class, new OuterListKey(String.valueOf( j )));
        tx.delete(LogicalDatastoreType.OPERATIONAL, ol_iid);

        InstanceIdentifier<InnerList> il_iid = InstanceIdentifier.create(TestExec.class)
                .child(OuterList.class, new OuterListKey(String.valueOf( 0 )))
                .child(InnerList.class, new InnerListKey(String.valueOf( j )));
        tx.delete(LogicalDatastoreType.OPERATIONAL, il_iid);

        try {
            tx.submit().checkedGet();
            txOk++;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
            txError++;
        }
        //LOG.info("delete: outer: {}", j);

    }


    public void doOper(DoCrudInput.Operation oper, int outerListId, int numInnerListElements) {

        txOk = 0;
        txError = 0;
        verifyError = 0;
        verifyOk = 0;

        switch (oper) {
            case CREATE:
                crudMonitor.enter();
                try {
                    if (readInnerList(0, outerListId) == null && readOuterList(outerListId) == null) {
                        createOuterList(outerListId, numInnerListElements);
                    } else {
                        LOG.error("CREATE: {} not found", outerListId);
                        txError++;
                    }
                } finally {
                    crudMonitor.leave();
                }
                break;
            case READ:
                readOuterList(outerListId);
                break;
            case DELETE:
                crudMonitor.enter();
                try {
                    if (readInnerList(0, outerListId) != null && readOuterList(outerListId) != null) {
                        deleteOuterList(outerListId);
                    } else {
                        LOG.error("DELETE: {} not found", outerListId);
                        txError++;
                    }
                } finally {
                    crudMonitor.leave();
                }
                break;
        }
    }
}
