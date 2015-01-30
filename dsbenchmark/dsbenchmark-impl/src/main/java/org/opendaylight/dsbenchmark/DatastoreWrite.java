package org.opendaylight.dsbenchmark;

public interface DatastoreWrite {
    void writeList();
    public int getTxError();
    public int getTxOk();
    public long getListBuildTime();
}
