package ncmount.impl.m2m;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import javax.annotation.Nonnull;
import ncmount.impl.m2m.spi.Translator;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class TranslatingReadOnlyTransaction implements DOMDataReadOnlyTransaction {

    private DOMDataReadOnlyTransaction deviceSpecificTx;
    private Translator translator;

    public TranslatingReadOnlyTransaction(final DOMDataReadOnlyTransaction domDataReadOnlyTransaction, final Translator translator) {
        this.deviceSpecificTx = domDataReadOnlyTransaction;
        this.translator = translator;
    }

    @Override
    public void close() {
        deviceSpecificTx.close();
    }

    @Nonnull
    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType logicalDatastoreType,
                                                                                   final YangInstanceIdentifier yangInstanceIdentifier) {
//            final YangInstanceIdentifier translatedIId = translator.toDeviceSpecific(yangInstanceIdentifier);
//
//            final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read = deviceSpecificTx.read(logicalDatastoreType, translatedIId);
//            return Futures.makeChecked(Futures.transform(read, new Function<Optional<NormalizedNode<?, ?>>, Optional<NormalizedNode<?, ?>>>() {
//
//                @Override
//                public Optional<NormalizedNode<?, ?>> translateId(final Optional<NormalizedNode<?, ?>> input) {
//                    if (input.isPresent()) {
//                        final NormalizedNode<?, ?> dataObject = input.get();
//                        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> translated = translator.fromDeviceSpecific(translatedIId, dataObject);
//                        return Optional.<NormalizedNode<?, ?>>of(translated.getValue());
//                    } else {
//                        return Optional.absent();
//                    }
//                }
//            }), READ_FAILED_EXCEPTION_FUNCTION);

        return translator.fromDeviceSpecific(logicalDatastoreType, yangInstanceIdentifier, deviceSpecificTx);
    }

    @Nonnull
    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType logicalDatastoreType,
                                                              final YangInstanceIdentifier yangInstanceIdentifier) {
        return Futures.makeChecked(Futures.transform(read(logicalDatastoreType, yangInstanceIdentifier), new Function<Optional<NormalizedNode<?, ?>>, Boolean>() {
            @Override
            public Boolean apply(final Optional<NormalizedNode<?, ?>> input) {
                return input.isPresent();
            }
        }), TranslatingMountPointFactory.READ_FAILED_EXCEPTION_FUNCTION);
    }

    @Nonnull
    @Override
    public Object getIdentifier() {
        return deviceSpecificTx.getIdentifier();
    }
}
