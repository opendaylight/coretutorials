package org.opendaylight.dsbenchmark;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatastoreBaPut extends DatastoreBaAbstractWrite {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreBaPut.class);

    public DatastoreBaPut(StartTestInput input, DataBroker dataBroker) {
        super(input, dataBroker);
        LOG.info("Creating DatastoreBaPut, input: {}", input);
    }

    @Override
    protected void txOperation(WriteTransaction tx, 
                               LogicalDatastoreType dst,
                               InstanceIdentifier<OuterList> iid, 
                               OuterList element) {

        tx.put(LogicalDatastoreType.CONFIGURATION, iid, element);
    }

}
