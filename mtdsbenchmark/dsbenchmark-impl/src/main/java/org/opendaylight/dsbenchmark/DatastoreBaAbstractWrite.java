package org.opendaylight.dsbenchmark;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
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

public abstract class DatastoreBaAbstractWrite implements DatastoreWrite {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreBaAbstractWrite.class);
    protected DataBroker dataBroker;
    protected List<OuterList> list;
    private long putsPerTx;
    protected int txOk = 0;
    protected int txError = 0;
    private long listBuildTime;

    abstract protected void txOperation(WriteTransaction tx,
                                        LogicalDatastoreType dst, 
                                        InstanceIdentifier<OuterList> iid, 
                                        OuterList elem);

 public DatastoreBaAbstractWrite(StartTestInput input, DataBroker dataBroker, long startKey) {
        this.putsPerTx = input.getPutsPerTx();
        this.dataBroker = dataBroker;
        this.list = buildOuterList(input, startKey);
    }

    @Override
    public void writeList() {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        long putCnt = 0;

        for (OuterList element : this.list) {
            InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                                                    .child(OuterList.class, element.getKey());
            txOperation(tx, LogicalDatastoreType.CONFIGURATION, iid, element);
            putCnt++;
            if (putCnt == putsPerTx) {
                try {
                    tx.submit().checkedGet();
                    txOk++;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Transaction failed: {}", e.toString());
                    txError++;
                }
                tx = dataBroker.newWriteOnlyTransaction();
                putCnt = 0;
            }
        }

        if (putCnt != 0) {
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction failed: {}", e.toString());
            }
        }
    }

    @Override
    public int getTxError() {
        return txError;
    }

    @Override
    public int getTxOk() {
        return txOk;
    }

    private List<OuterList> buildOuterList( StartTestInput input, long startKey ) {
        long startTime = System.nanoTime();

        List<OuterList> outerList = new ArrayList<OuterList>(input.getOuterElements().intValue());
        for( int j = 0; j < input.getOuterElements().intValue(); j++ ) {
            outerList.add(new OuterListBuilder()
                                .setId( j )
                                .setInnerList( buildInnerList( j, input.getInnerElements().intValue()) )
                                .setKey(new OuterListKey((int) startKey +  j ))
                                .build() );
        }
        long endTime = System.nanoTime();
        listBuildTime =  (endTime - startTime )/1000000;

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

    /**
     * @return the listBuildTime
     */
    public long getListBuildTime() {
        return listBuildTime;
    }

}
