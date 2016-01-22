package ncmount.impl.m2m;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import ncmount.impl.m2m.spi.Translator;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class TranslatingReadWriteTransaction implements DOMDataReadWriteTransaction {

    public TranslatingReadWriteTransaction(final DOMDataReadWriteTransaction domDataReadWriteTransaction, final Translator translator) {
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void delete(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier) {

    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return null;
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier) {
        return null;
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier) {
        return null;
    }

    @Override
    public void put(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier, final NormalizedNode<?, ?> normalizedNode) {

    }

    @Override
    public void merge(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier, final NormalizedNode<?, ?> normalizedNode) {

    }

    @Override
    public Object getIdentifier() {
        return null;
    }
}
