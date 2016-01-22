package ncmount.impl.m2m;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import ncmount.impl.m2m.spi.Translator;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class TranslatingWriteOnlyTransaction implements DOMDataWriteTransaction {

    private DOMDataWriteTransaction deviceSpecificTx;
    private Translator translator;

    public TranslatingWriteOnlyTransaction(final DOMDataWriteTransaction domDataWriteTransaction, final Translator translator) {
        this.deviceSpecificTx = domDataWriteTransaction;
        this.translator = translator;
    }

    @Override
    public void put(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier, final NormalizedNode<?, ?> normalizedNode) {
        translator.putDeviceSpecific(logicalDatastoreType, yangInstanceIdentifier, normalizedNode, deviceSpecificTx);
    }

    @Override
    public void merge(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier, final NormalizedNode<?, ?> normalizedNode) {

    }

    @Override
    public boolean cancel() {
        return deviceSpecificTx.cancel();
    }

    @Override
    public void delete(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier) {

    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return deviceSpecificTx.submit();
    }

    @Deprecated
    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        throw new UnsupportedOperationException("Deprecated, use submit");
    }

    @Override
    public Object getIdentifier() {
        return deviceSpecificTx.getIdentifier();
    }
}
