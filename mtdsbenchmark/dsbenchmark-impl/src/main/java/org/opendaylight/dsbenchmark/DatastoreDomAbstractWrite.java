package org.opendaylight.dsbenchmark;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatastoreDomAbstractWrite implements DatastoreWrite {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreDomAbstractWrite.class);

    // Inner List Qname identifiers for yang model's 'name' and 'value'
    private static final org.opendaylight.yangtools.yang.common.QName IL_NAME = QName.create(InnerList.QNAME, "name");
    private static final org.opendaylight.yangtools.yang.common.QName IL_VALUE = QName.create(InnerList.QNAME, "value");

    // Outer List Qname identifier for yang model's 'id'
    private static final org.opendaylight.yangtools.yang.common.QName OL_ID = QName.create(InnerList.QNAME, "id");

    // Statistics
    private long putsPerTx;
    protected int txOk = 0;
    protected int txError = 0;
    private long listBuildTime;

    private final DOMDataBroker domDataBroker;
    List<MapEntryNode> list;

    abstract protected void txOperation(DOMDataWriteTransaction tx,
                                        LogicalDatastoreType dst, 
                                        YangInstanceIdentifier yid, 
                                        MapEntryNode elem);

    public DatastoreDomAbstractWrite(StartTestInput input, DOMDataBroker domDataBroker, long startKey) {
        this.domDataBroker = domDataBroker;
        this.putsPerTx = input.getPutsPerTx();
        this.list = buildOuterList(input, startKey);
    }

    @Override
    public void writeList() {
        DOMDataWriteTransaction tx = domDataBroker.newWriteOnlyTransaction();
        long putCnt = 0;

        for (MapEntryNode element : this.list) {
            YangInstanceIdentifier yid = YangInstanceIdentifier.builder().node(TestExec.QNAME)
                                                .node(OuterList.QNAME)
                                                .nodeWithKey(OuterList.QNAME, element.getIdentifier().getKeyValues())
                                                .build();            
            txOperation(tx, LogicalDatastoreType.CONFIGURATION, yid, element);
            putCnt++;
            if (putCnt == putsPerTx) {
                try {
                    tx.submit().checkedGet();
                    txOk++;
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Transaction failed: {}", e.toString());
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

    private List<MapEntryNode> buildOuterList( StartTestInput input, long startKey ) {
        long startTime = System.nanoTime();

        List<MapEntryNode> outerList = new ArrayList<MapEntryNode>(input.getOuterElements().intValue());
        for( int j = 0; j < input.getOuterElements().intValue(); j++ ) {
            outerList.add(ImmutableNodes.mapEntryBuilder()
                                .withNodeIdentifier(new NodeIdentifierWithPredicates(OuterList.QNAME, OL_ID, startKey+j))
                                .withChild(ImmutableNodes.leafNode(OL_ID, startKey+j))
                                .withChild(buildInnerList(j, input.getInnerElements().intValue())) 
                                .build());
        }
        long endTime = System.nanoTime();
        listBuildTime = (endTime - startTime )/1000000;

        return outerList;
    }

    private MapNode buildInnerList( int index, int elements ) {
        CollectionNodeBuilder<MapEntryNode, MapNode> innerList = ImmutableNodes.mapNodeBuilder(InnerList.QNAME);

        for( int i = 0; i < elements; i++ ) {
            innerList.addChild(ImmutableNodes.mapEntryBuilder()
                                .withNodeIdentifier(new NodeIdentifierWithPredicates(InnerList.QNAME, IL_NAME, i))
                                .withChild(ImmutableNodes.leafNode(IL_NAME, i))
                                .withChild(ImmutableNodes.leafNode(IL_VALUE, "Item-"
                                                                             + String.valueOf(index)
                                                                             + "-"
                                                                             + String.valueOf(i)))
                                .build());
        }
        return innerList.build();
    }

    /**
     * @return the listBuildTime
     */
    public long getListBuildTime() {
        return listBuildTime;
    }

}
