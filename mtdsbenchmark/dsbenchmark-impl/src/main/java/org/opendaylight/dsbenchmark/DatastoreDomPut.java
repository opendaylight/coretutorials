package org.opendaylight.dsbenchmark;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatastoreDomPut extends DatastoreDomAbstractWrite {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreDomPut.class);

    public DatastoreDomPut(StartTestInput input, DOMDataBroker domDataBroker, long startKey) {
        super(input, domDataBroker, startKey);
        LOG.info("Creating DatastoreBiPut, input: {}", input);
    }

    @Override
    protected void txOperation(DOMDataWriteTransaction tx,
            LogicalDatastoreType dst, YangInstanceIdentifier yid,
            MapEntryNode elem) {
        tx.put(LogicalDatastoreType.CONFIGURATION, yid, elem);
    }
}
