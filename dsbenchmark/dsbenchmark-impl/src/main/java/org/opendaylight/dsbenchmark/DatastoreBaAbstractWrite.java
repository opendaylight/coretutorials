package org.opendaylight.dsbenchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public abstract class DatastoreBaAbstractWrite implements DatastoreWrite, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreBaAbstractWrite.class);
    private final BindingTransactionChain chain;
    private final long putsPerTx;
    protected final DataBroker dataBroker;
    protected List<OuterList> list;
    protected int txOk = 0;
    protected int txError = 0;

    // Shared instance for reuse
    private final FutureCallback<Void> txCallback = new FutureCallback<Void>() {
        @Override
        public void onSuccess(final Void result) {
            txOk++;
        }
        @Override
        public void onFailure(final Throwable t) {
            LOG.error("Transaction error: {}", t);
            txError++;
        }
    };

    abstract protected void txOperation(WriteTransaction tx,
                                        LogicalDatastoreType dst, 
                                        InstanceIdentifier<OuterList> iid, 
                                        OuterList elem);

    public DatastoreBaAbstractWrite(StartTestInput input, DataBroker dataBroker) {
        this.putsPerTx = input.getPutsPerTx();
        this.dataBroker = dataBroker;
        this.chain = dataBroker.createTransactionChain(this);
    }

    @Override
    public void createList(StartTestInput input) {
         list = buildOuterList(input.getOuterElements().intValue(), input.getInnerElements().intValue());
    }

    @Override
    public void writeList() {
        WriteTransaction tx = chain.newWriteOnlyTransaction();
        CheckedFuture<Void, TransactionCommitFailedException> lastFuture = null;
        long putCnt = 0;

        // Shared parent instance identifier
        final InstanceIdentifier<TestExec> pid = InstanceIdentifier.create(TestExec.class);
        for (OuterList element : this.list) {
            InstanceIdentifier<OuterList> iid = pid.child(OuterList.class, element.getKey());
            txOperation(tx, LogicalDatastoreType.CONFIGURATION, iid, element);
            putCnt++;
            if (putCnt == putsPerTx) {
                lastFuture = tx.submit();
                Futures.addCallback(lastFuture, txCallback);
                tx = chain.newWriteOnlyTransaction();
                putCnt = 0;
            }
        }

        if (putCnt != 0) {
            lastFuture = tx.submit();
            Futures.addCallback(lastFuture, txCallback);
        }

        LOG.info("All transactions submitted, synchronizing on last transaction");
        if (lastFuture != null) {
            try {
                // This is not a complete guarantee all callback complete, but better
                // than nothing. The issue is that a Future's value is set before
                // callbacks are dispatched, so we still have a subtle race.
                lastFuture.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Failed to complete last transaction within timeout", e);
            }
        }

        LOG.info("Transaction completed, txCnt {}", (txOk + txError));
    }

    @Override
    public int getTxError() {
        return txError;
    }

    @Override
    public int getTxOk() {
        return txOk;
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> chain,
            AsyncTransaction<?, ?> transaction, Throwable cause) {
        LOG.error("Broken chain in DatastoreBaAbstractWrite, transaction {}, cause {}",
                transaction.getIdentifier(), cause);
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> arg0) {
        LOG.info("DatastoreBaAbstractWrite closed successfully");
        
    }

    private List<OuterList> buildOuterList(int outerElements, int innerElements) {
        List<OuterList> outerList = new ArrayList<OuterList>(outerElements);
        for (int j = 0; j < outerElements; j++) {
            outerList.add(new OuterListBuilder()
                                .setId( j )
                                .setInnerList(buildInnerList(j, innerElements))
                                .setKey(new OuterListKey( j ))
                                .build());
        }

        return outerList;
    }

    private List<InnerList> buildInnerList( int index, int elements ) {
        List<InnerList> innerList = new ArrayList<InnerList>( elements );

        for( int i = 0; i < elements; i++ ) {
            innerList.add(new InnerListBuilder()
                                .setKey( new InnerListKey( i ) )
                                .setName(i)
                                .setValue( "Item-"
                                           + String.valueOf( index )
                                           + "-"
                                           + String.valueOf( i ) )
                                .build());
        }

        return innerList;
    }

}
