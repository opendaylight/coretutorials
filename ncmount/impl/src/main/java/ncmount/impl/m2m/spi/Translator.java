package ncmount.impl.m2m.spi;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Translated between devices specific and generic proxy models
 */
public interface Translator {

    /**
     * Return true if this translator instance can translate into provided schema ctx
     */
    boolean isApplicable(@Nonnull final SchemaContext mountPoint);

    /**
     * Translates original instance id to new instance id e.g. adds "as-ios-xr" suffix to node id
     */
    YangInstanceIdentifier translateId(YangInstanceIdentifier mountPointId);

    /**
     * Perform read on a device specific transaction. Single read can be translated into multiple device
     * specific reads. That's why translator has access to read tx here.
     */
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> fromDeviceSpecific(
            @Nonnull final LogicalDatastoreType logicalDatastoreType,
            @Nonnull final YangInstanceIdentifier yangInstanceIdentifier,
            @Nonnull final DOMDataReadOnlyTransaction deviceSpecificTx)
            throws TranslationException;


    /**
     * Perform put on a device specific transaction. Single put can be translated into multiple device
     * specific op0erations (puts, merges, deletes etc.). That's why translator has access to write tx here.
     */
    void putDeviceSpecific(@Nonnull final LogicalDatastoreType logicalDatastoreType,
                           @Nonnull final YangInstanceIdentifier yangInstanceIdentifier,
                           @Nonnull final NormalizedNode<?, ?> normalizedNode,
                           @Nonnull final DOMDataWriteTransaction deviceSpecificTx)
            throws TranslationException;

    class TranslationException extends RuntimeException {
    }
}
