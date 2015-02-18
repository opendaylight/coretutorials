package org.opendaylight.dsbenchmark.simpletx;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.dsbenchmark.BaListBuilder;
import org.opendaylight.dsbenchmark.DatastoreWrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpletxBaAbstractWrite implements DatastoreWrite {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxBaAbstractWrite.class);
    protected DataBroker dataBroker;
    protected List<OuterList> list;
    private long putsPerTx;
    protected int txOk = 0;
    protected int txError = 0;

    abstract protected void txOperation(WriteTransaction tx,
                                        LogicalDatastoreType dst, 
                                        InstanceIdentifier<OuterList> iid, 
                                        OuterList elem);

 public SimpletxBaAbstractWrite(StartTestInput input, DataBroker dataBroker) {
        this.putsPerTx = input.getPutsPerTx();
        this.dataBroker = dataBroker;
    }

     @Override
     public void createList(StartTestInput input) {
         list = BaListBuilder.buildOuterList(input.getOuterElements().intValue(), 
                                             input.getInnerElements().intValue());
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

}
