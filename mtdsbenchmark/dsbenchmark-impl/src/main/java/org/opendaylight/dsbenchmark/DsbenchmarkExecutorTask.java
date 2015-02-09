package org.opendaylight.dsbenchmark;

import java.util.concurrent.*;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.DsbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestExecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatus.ExecStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.TestStatusBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;

/**
 * Created by ragbhat on 2/3/15.
 */
public class DsbenchmarkExecutorTask  implements Callable< StartTestOutput > {

    StartTestInput testInput;
    DataBroker dataBroker;
    DOMDataBroker domDataBroker;
    long startKey;

    public DsbenchmarkExecutorTask(StartTestInput input, long start, DataBroker dBroker, DOMDataBroker ddBroker){
        testInput = input;
        dataBroker = dBroker;
        domDataBroker = ddBroker;
        startKey = start;
    }

    public StartTestOutput call() {
        // Get the appropriate writer based on operation type and data format
        DatastoreWrite dsWriter = getDatastoreWrite(testInput, startKey);
        dsWriter.writeList();
        StartTestOutput output = new StartTestOutputBuilder()
                .setStatus(StartTestOutput.Status.OK)
                .setListBuildTime(dsWriter.getListBuildTime())
                .setTxOk((long)dsWriter.getTxOk())
                .setTxError((long)dsWriter.getTxError())
                .build();

        return output;
    }

    private DatastoreWrite getDatastoreWrite(StartTestInput input, long startKey ) {
        StartTestInput.Operation oper = input.getOperation();
        StartTestInput.DataFormat format = input.getDataFormat();

        final DatastoreWrite retVal;
        if ( oper == StartTestInput.Operation.DUMP) {
            retVal = new DatastoreBaDump(input, dataBroker, startKey);
        } else if ( oper == StartTestInput.Operation.PUT ) {
            retVal = (format ==  StartTestInput.DataFormat.BINDINGAWARE ?
                    new DatastoreBaPut(input, dataBroker, startKey) :
                    new DatastoreDomPut(input, domDataBroker, startKey));
        }
        else if ( oper == StartTestInput.Operation.MERGE ) {
            retVal = (format ==  StartTestInput.DataFormat.BINDINGAWARE ?
                    new DatastoreBaMerge(input, dataBroker, startKey) :
                    new DatastoreDomMerge(input, domDataBroker, startKey));
        }
        else if ( oper == StartTestInput.Operation.DELETE ) {
            retVal = new DatastoreBaDelete(input, dataBroker, startKey);
        } else {
            throw new IllegalArgumentException("Unsupported test type");
        }

        return retVal;

    }

}
