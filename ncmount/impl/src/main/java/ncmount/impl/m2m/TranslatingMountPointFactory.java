package ncmount.impl.m2m;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import ncmount.impl.m2m.spi.Translator;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Customizable MountPoint factory that automatically creates proxy mountpoints. These proxies allow users
 * to introduce model-to-model translation
 */
public final class TranslatingMountPointFactory implements AutoCloseable, MountProvisionListener {

    private final ListenerRegistration<MountProvisionListener> translatingMountPointFactoryListenerRegistration;
    private final DOMMountPointService domMountService;
    private final SchemaContext proxySchemaContext;
    private final Translator translator;
    private final Map<YangInstanceIdentifier, ObjectRegistration<DOMMountPoint>> proxies = Maps.newHashMap();

    // TODO allow multiple translators and pick appropriate translators (allow multiple) for each mountpoint

    /**
     * @param proxySchemaContext - Generic schema context
     */
    public TranslatingMountPointFactory(final DOMMountPointService domMountService,
                                        final SchemaContext proxySchemaContext, final Translator translator) {
        this.domMountService = domMountService;
        this.proxySchemaContext = proxySchemaContext;
        this.translator = translator;
        this.translatingMountPointFactoryListenerRegistration = domMountService.registerProvisionListener(this);
    }

    @Override
    public synchronized void close() throws Exception {
        translatingMountPointFactoryListenerRegistration.close();
        for (Map.Entry<YangInstanceIdentifier, ObjectRegistration<DOMMountPoint>> proxyMountPointEntry : proxies.entrySet()) {
            proxyMountPointEntry.getValue().close();
        }
        proxies.clear();
    }

    @Override
    public void onMountPointCreated(final YangInstanceIdentifier yangInstanceIdentifier) {
        // Get mount point specific schema context. Only DOMMOuntPoints provide it
        final DOMMountPoint mountPoint = domMountService.getMountPoint(yangInstanceIdentifier).get();
        final SchemaContext schemaContext = mountPoint.getSchemaContext();

        // Ignore mountpoints that cannot be translated
        if(!translator.isApplicable(schemaContext)) {
            return;
        }

        final YangInstanceIdentifier newId = translator.translateId(mountPoint.getIdentifier());
        final DOMMountPointService.DOMMountPointBuilder translatingMountPointProxy = domMountService.createMountPoint(newId);
        translatingMountPointProxy.addInitialSchemaContext(proxySchemaContext);

        final Optional<DOMDataBroker> dataBrokerService = mountPoint.getService(DOMDataBroker.class);
        if(dataBrokerService.isPresent()) {
            translatingMountPointProxy.addService(DOMDataBroker.class, new TranslatingDOMBrokerProxy(dataBrokerService.get(), translator));
        }

        // TODO Same for RPC

        // TODO Same for notifications

        final ObjectRegistration<DOMMountPoint> register = translatingMountPointProxy.register();
        // TODO put node into DS e.g. for netconf add node into netconf-topology. Encapsulate! that functionality into SPI
        proxies.put(newId, register);
    }

    @Override
    public void onMountPointRemoved(final YangInstanceIdentifier yangInstanceIdentifier) {
        if(proxies.containsKey(yangInstanceIdentifier)) {
            try {
                proxies.get(yangInstanceIdentifier).close();
            } catch (Exception e) {
//                LOG.warn(...)
                throw new RuntimeException("Unable to remove translating mountpoint proxy for " + yangInstanceIdentifier, e);
            }
            proxies.remove(yangInstanceIdentifier);
        }

    }

    private static final class TranslatingDOMBrokerProxy implements DOMDataBroker {

        private DOMDataBroker domDataBroker;
        private Translator translator;

        public TranslatingDOMBrokerProxy(final DOMDataBroker domDataBroker, final Translator translator) {
            this.domDataBroker = domDataBroker;
            this.translator = translator;
        }

        @Override
        public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
            return new TranslatingReadOnlyTransaction(domDataBroker.newReadOnlyTransaction(), translator);
        }

        @Override
        public DOMDataReadWriteTransaction newReadWriteTransaction() {
            return new TranslatingReadWriteTransaction(domDataBroker.newReadWriteTransaction(), translator);
        }

        @Override
        public DOMDataWriteTransaction newWriteOnlyTransaction() {
            return new TranslatingWriteOnlyTransaction(domDataBroker.newWriteOnlyTransaction(), translator);
        }

        @Override
        public ListenerRegistration<DOMDataChangeListener> registerDataChangeListener(final LogicalDatastoreType logicalDatastoreType, final YangInstanceIdentifier yangInstanceIdentifier, final DOMDataChangeListener domDataChangeListener, final DataChangeScope dataChangeScope) {
            throw new UnsupportedOperationException("Data change listener not supported");
        }

        @Override
        public DOMTransactionChain createTransactionChain(final TransactionChainListener transactionChainListener) {
            throw new UnsupportedOperationException("Tx chain not supported");
        }

        @Nonnull
        @Override
        public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
            return Collections.emptyMap();
        }

    }

}
