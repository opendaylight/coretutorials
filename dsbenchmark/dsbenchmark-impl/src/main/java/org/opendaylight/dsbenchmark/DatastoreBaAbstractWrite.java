package org.opendaylight.dsbenchmark;

import java.util.ArrayList;
import java.util.List;

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
        long putCnt = 0;

        for (OuterList element : this.list) {
            InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                                                    .child(OuterList.class, element.getKey());
            txOperation(tx, LogicalDatastoreType.CONFIGURATION, iid, element);
            putCnt++;
            if (putCnt == putsPerTx) {
                Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        txOk++;
                    }
                    @Override
                    public void onFailure(final Throwable t) {
                        LOG.error("1-Transaction error: {}", t);
                        txError++;
                    }
                });
                tx = chain.newWriteOnlyTransaction();
                putCnt = 0;
            }
        }

        if (putCnt != 0) {
            Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    txOk++;
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("2-Transaction error: {}", t);
                    txError++;
                }
            });
        }
        LOG.info("All transactions submitted, txCnt {}", (txOk + txError));
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
