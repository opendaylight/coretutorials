package org.opendaylight.dsbenchmark;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;

public interface DatastoreWrite {
    public void writeList();
    public void createList(StartTestInput input);
    public int getTxError();
    public int getTxOk();
}
