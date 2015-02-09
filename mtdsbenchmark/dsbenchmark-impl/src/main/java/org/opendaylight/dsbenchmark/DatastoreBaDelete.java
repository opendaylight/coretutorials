package org.opendaylight.dsbenchmark;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatastoreBaDelete implements DatastoreWrite {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreBaDelete.class);
    private DataBroker dataBroker;
    private long putsPerTx;
    private long outerElements;
    private int txOk = 0;
    private int txError = 0;
    private long listBuildTime;
    private long startKey;

    public DatastoreBaDelete(StartTestInput input, DataBroker dataBroker, long start) {
        LOG.info("Creating DatastoreDelete, input: {}", input );
        this.putsPerTx = input.getPutsPerTx();
        this.outerElements = input.getOuterElements();
        this.dataBroker = dataBroker;
        this.startKey = start;
        StartTestInputBuilder inputBuilder = new StartTestInputBuilder();

        inputBuilder.setPutsPerTx(input.getOuterElements());
        inputBuilder.setOuterElements(input.getOuterElements());
        inputBuilder.setInnerElements(input.getInnerElements());
        DatastoreBaPut dd = new DatastoreBaPut(inputBuilder.build(), dataBroker, start);
        dd.writeList();
        this.listBuildTime = dd.getListBuildTime();
    }

    @Override
    public void writeList() {
            WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
            long putCnt = 0;

            for (long l = 0; l < outerElements; l++) {
                InstanceIdentifier<OuterList> iid = InstanceIdentifier.create(TestExec.class)
                                                        .child(OuterList.class, new OuterListKey((int)(startKey+l)));
                tx.delete(LogicalDatastoreType.CONFIGURATION, iid);
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

    @Override
    public long getListBuildTime() {
        return listBuildTime;
    }

}
