package org.opendaylight.dsbenchmark.simpletx;

import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.dsbenchmark.DatastoreAbstractWriter;
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

public class SimpletxDomWrite extends DatastoreAbstractWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SimpletxDomWrite.class);
    private final DOMDataBroker domDataBroker;

    // Inner List Qname identifiers for yang model's 'name' and 'value'
    private static final org.opendaylight.yangtools.yang.common.QName IL_NAME = QName.create(InnerList.QNAME, "name");
    private static final org.opendaylight.yangtools.yang.common.QName IL_VALUE = QName.create(InnerList.QNAME, "value");

    // Outer List Qname identifier for yang model's 'id'
    private static final org.opendaylight.yangtools.yang.common.QName OL_ID = QName.create(OuterList.QNAME, "id");
    
    private List<MapEntryNode> list;

    public SimpletxDomWrite(DOMDataBroker domDataBroker, StartTestInput.Operation oper,
                                    int outerListElem, int innerListElem, long putsPerTx ) {
        super(oper, outerListElem, innerListElem, putsPerTx);
        this.domDataBroker = domDataBroker;
        LOG.info("Created SimpletxDomWrite");
    }

    @Override
    public void createList() {
        list = DomListBuilder.buildOuterList(OL_ID, IL_NAME, IL_VALUE,
                                             this.outerListElem, this.innerListElem);
    }

    @Override
    public void writeList() {
        DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
        long writeCnt = 0;

        YangInstanceIdentifier pid = YangInstanceIdentifier.builder().node(TestExec.QNAME).node(OuterList.QNAME).build();
        for (MapEntryNode element : this.list) {
            YangInstanceIdentifier yid = pid.node(new NodeIdentifierWithPredicates(OuterList.QNAME, element.getIdentifier().getKeyValues()));

            if (oper == StartTestInput.Operation.PUT) {
                tx.put(LogicalDatastoreType.CONFIGURATION, yid, element);
            } else {
                tx.merge(LogicalDatastoreType.CONFIGURATION, yid, element);                
            }

            writeCnt++;

            if (writeCnt == writesPerTx) {
                try {
                    tx.submit().checkedGet();
                    txOk++;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Transaction failed", e);
                    txError++;
                }
                tx = domDataBroker.newWriteOnlyTransaction();
                writeCnt = 0;
            }
        }

        if (writeCnt != 0) {
            try {
                tx.submit().checkedGet();
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction failed", e);
            }
        }

    }

}
