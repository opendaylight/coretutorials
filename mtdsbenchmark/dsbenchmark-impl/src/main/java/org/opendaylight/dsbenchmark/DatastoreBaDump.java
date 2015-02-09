package org.opendaylight.dsbenchmark;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatastoreBaDump extends DatastoreBaAbstractWrite{
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreBaDump.class);
    private static final InstanceIdentifier<TestExec> TEST_EXEC_IID = InstanceIdentifier.builder(TestExec.class).build();

    public DatastoreBaDump(StartTestInput input, DataBroker dataBroker, long startKey) {
        super(input, dataBroker, startKey);
        LOG.info("Creating DatastoreDump, input: {}", input );
    }

    @Override
    public void writeList() {
        TestExec testData = new TestExecBuilder()
        .setOuterList(list)
        .build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, TEST_EXEC_IID, testData);

        try {
            tx.submit().checkedGet();
            txOk++;
        } catch (TransactionCommitFailedException e) {
            txError++;
            LOG.error("Test transaction failed");
            throw new IllegalStateException(e);
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
    protected void txOperation(WriteTransaction tx, 
                               LogicalDatastoreType dst,
                               InstanceIdentifier<OuterList> iid,
                               OuterList elem) {
        // Just a dummy function - should never be called
        LOG.error("Dummy function called.");
        
    }
}
