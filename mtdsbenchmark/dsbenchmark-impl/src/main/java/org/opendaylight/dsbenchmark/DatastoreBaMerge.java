package org.opendaylight.dsbenchmark;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatastoreBaMerge extends DatastoreBaAbstractWrite {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreBaMerge.class);

    public DatastoreBaMerge(StartTestInput input, DataBroker dataBroker, long startKey) {
        super(input, dataBroker, startKey);
        LOG.info("Creating DatastoreBaMerge, input: {}", input );
    }

    @Override
    protected void txOperation(WriteTransaction tx,
                               LogicalDatastoreType dst, 
                               InstanceIdentifier<OuterList> iid, 
                               OuterList element) {

        tx.merge(LogicalDatastoreType.CONFIGURATION, iid, element);
    }

}
