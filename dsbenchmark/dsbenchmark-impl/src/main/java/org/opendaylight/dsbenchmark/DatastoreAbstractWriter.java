package org.opendaylight.dsbenchmark;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.StartTestInput;

public abstract class DatastoreAbstractWriter {
    protected final int outerListElem;
    protected final int innerListElem;
    protected final long writesPerTx;
    protected final StartTestInput.Operation oper;
    
    protected int txOk = 0;
    protected int txError = 0;


    public DatastoreAbstractWriter(StartTestInput.Operation oper,
                                   int outerListElem, int innerListElem, long writesPerTx) {
        this.outerListElem = outerListElem;
        this.innerListElem = innerListElem;
        this.writesPerTx = writesPerTx;
        this.oper = oper;
    }

    public abstract void createList();
    public abstract void writeList();
    
    public int getTxError() {
        return txError;
    }

    public int getTxOk() {
        return txOk;
    }

}
