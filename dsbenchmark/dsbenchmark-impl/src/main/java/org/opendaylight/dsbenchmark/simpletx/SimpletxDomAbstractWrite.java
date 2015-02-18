package org.opendaylight.dsbenchmark.simpletx;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.dsbenchmark.DatastoreWrite;
import org.opendaylight.dsbenchmark.DomListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpletxDomAbstractWrite implements DatastoreWrite {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxDomAbstractWrite.class);

    // Inner List Qname identifiers for yang model's 'name' and 'value'
    private static final org.opendaylight.yangtools.yang.common.QName IL_NAME = QName.create(InnerList.QNAME, "name");
    private static final org.opendaylight.yangtools.yang.common.QName IL_VALUE = QName.create(InnerList.QNAME, "value");

    // Outer List Qname identifier for yang model's 'id'
    private static final org.opendaylight.yangtools.yang.common.QName OL_ID = QName.create(OuterList.QNAME, "id");

    // Statistics
    private long putsPerTx;
    protected int txOk = 0;
    protected int txError = 0;

    private final DOMDataBroker domDataBroker;
    List<MapEntryNode> list;

    abstract protected void txOperation(DOMDataWriteTransaction tx,
                                        LogicalDatastoreType dst, 
                                        YangInstanceIdentifier yid, 
                                        MapEntryNode elem);

    public SimpletxDomAbstractWrite(StartTestInput input, DOMDataBroker domDataBroker) {
        this.domDataBroker = domDataBroker;
        this.putsPerTx = input.getPutsPerTx();
    }

    @Override
    public void createList(StartTestInput input) {
        list = DomListBuilder.buildOuterList(OL_ID, IL_NAME, IL_VALUE,
                                             input.getOuterElements().intValue(),
                                             input.getInnerElements().intValue());
    }

    @Override
    public void writeList() {
        DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
        long putCnt = 0;

        YangInstanceIdentifier pid = YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();
        for (MapEntryNode element : this.list) {
            YangInstanceIdentifier yid = pid.node(new NodeIdentifierWithPredicates(OuterList.QNAME, element.getIdentifier().getKeyValues()));
            txOperation(tx, LogicalDatastoreType.CONFIGURATION, yid, element);
            putCnt++;
            if (putCnt == putsPerTx) {
                try {
                    tx.submit().checkedGet();
                    txOk++;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Transaction failed", e);
                    txError++;
                }
                tx = domDataBroker.newWriteOnlyTransaction();
                putCnt = 0;
            }
        }

        if (putCnt != 0) {
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction failed", e);
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
