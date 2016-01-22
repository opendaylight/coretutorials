package ncmount.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
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
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150107.$YangModuleInfoImpl;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Customizable MountPoint factory that automatically creates proxy mountpoints. These proxies allow users
 * to introduce model-to-model translation
 */
public final class TranslatingMountPointFactory implements MountPointService.MountPointListener, AutoCloseable {

    private final ListenerRegistration<TranslatingMountPointFactory> translatingMountPointFactoryListenerRegistration;
    private final MountPointService mountService;
    private final DOMMountPointService domMountService;
    private final SchemaContext proxySchemaContext;
    private final InstanceIdTranslator idTranslator;
    private final Translator translator;
    private final Map<InstanceIdentifier<?>, ObjectRegistration<DOMMountPoint>> proxies = Maps.newHashMap();
    private final Bi2Ba bi2ba;

    private static final Function<Exception, ReadFailedException> READ_FAILED_EXCEPTION_FUNCTION = new Function<Exception, ReadFailedException>() {
        @Nullable
        @Override
        public ReadFailedException apply(@Nullable final Exception input) {
            return input instanceof ReadFailedException ?
                    ((ReadFailedException) input) :
                    new ReadFailedException("Read failed in proxy", input);
        }
    };

    /**
     * @param proxySchemaContext - Generic schema context
     */
    public TranslatingMountPointFactory(final MountPointService mountService, final DOMMountPointService domMountService,
                                        final SchemaContext proxySchemaContext, final InstanceIdTranslator idTranslator,
                                        final Translator translator) {
        this.mountService = mountService;
        this.domMountService = domMountService;
        this.proxySchemaContext = proxySchemaContext;
        this.idTranslator = idTranslator;
        this.translator = translator;
        this.translatingMountPointFactoryListenerRegistration = mountService.registerListener(null, this);
        this.bi2ba = new Bi2Ba();
    }

    @Override
    public synchronized void onMountPointCreated(final InstanceIdentifier<?> instanceIdentifier) {
        final MountPoint mountPoint = mountService.getMountPoint(instanceIdentifier).get();
        // Get mount point specific schema context. Only DOMMOuntPoints provide it
        final SchemaContext schemaContext = domMountService.getMountPoint(bi2ba.toBI(instanceIdentifier)).get().getSchemaContext();

        // Ignore mountpoints that cannot be translated
        if(!translator.isApplicable(schemaContext)) {
            return;
        }

        final InstanceIdentifier<?> newId = idTranslator.apply(mountPoint.getIdentifier());

        final DOMMountPointService.DOMMountPointBuilder translatingMountPointProxy = domMountService.createMountPoint(bi2ba.toBI(newId));
        translatingMountPointProxy.addInitialSchemaContext(proxySchemaContext);

        final Optional<DataBroker> dataBrokerService = mountPoint.getService(DataBroker.class);
        if(dataBrokerService.isPresent()) {
            translatingMountPointProxy.addService(DOMDataBroker.class, new TranslatingDOMBrokerProxy(dataBrokerService.get(), translator, bi2ba));
        }

        // Same for RPC

        // Same for notifications

        final ObjectRegistration<DOMMountPoint> register = translatingMountPointProxy.register();
        // TODO put node into DS e.g. for netconf add ndoe into netconf-topology. Encapsulate! that functionality into SPI
        proxies.put(instanceIdentifier, register);
    }

    @Override
    public synchronized void onMountPointRemoved(final InstanceIdentifier<?> instanceIdentifier) {
        if(proxies.containsKey(instanceIdentifier)) {
            try {
                proxies.get(instanceIdentifier).close();
            } catch (Exception e) {
//                LOG.warn(...)
                throw new RuntimeException("Unable to remove translating mountpoint proxy for " + instanceIdentifier, e);
            }
            proxies.remove(instanceIdentifier);
        }
    }

    @Override
    public synchronized void close() throws Exception {
        translatingMountPointFactoryListenerRegistration.close();
        for (Map.Entry<InstanceIdentifier<?>, ObjectRegistration<DOMMountPoint>> proxyMountPointEntry : proxies.entrySet()) {
            proxyMountPointEntry.getValue().close();
        }
        proxies.clear();
        bi2ba.close();
    }

    /******** SPI ********/

    /**
     * Translates original instance id to new instance id e.g. adds "as-ios-xr" suffix to node id
     */
    public interface InstanceIdTranslator extends Function<InstanceIdentifier<?>, InstanceIdentifier<?>> {}

    /**
     * Translated between devices specific and generic proxy models
     */
    public interface Translator {

        /**
         * Return true if this translator instance can translate into provided schema ctx
         */
        boolean isApplicable(@Nonnull final SchemaContext mountPoint);

        // TODO add device specific schema context to below methods, so the translator knows what's the schema
        // context of real device ... If it needs it

        /**
         * Perform model-to-model translation from generic models, to device specific
         */
        @Nonnull
        InstanceIdentifier<? extends DataObject> toDeviceSpecific(
                @Nonnull final InstanceIdentifier<? extends DataObject> instanceIdentifier);

        /**
         * Perform model-to-model translation to generic models, from device specific
         */
//        @Nonnull
//        InstanceIdentifier<? extends DataObject> fromDeviceSpecific(
//                @Nonnull final InstanceIdentifier<? extends DataObject> instanceIdentifier);


        /**
         * Perform model-to-model translation from generic models, to device specific
         */
        @Nonnull
        Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> fromDeviceSpecific(
                @Nonnull final InstanceIdentifier<? extends DataObject> translatedIId,
                @Nonnull final  DataObject dataObject);


//        @Nonnull
//        Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> toDeviceSpecific(
//                @Nonnull final InstanceIdentifier<? extends DataObject> translatedIId,
//                @Nonnull final  DataObject dataObject);

    }

    /******** SPI - end ********/

    private static final class TranslatingDOMBrokerProxy implements DOMDataBroker {

        private final DataBroker dataBrokerService;
        private final Translator translator;
        private final Bi2Ba bi2ba;

        public TranslatingDOMBrokerProxy(final DataBroker dataBrokerService, final Translator translator, final Bi2Ba bi2ba) {
            this.dataBrokerService = dataBrokerService;
            this.translator = translator;
            this.bi2ba = bi2ba;
        }

        @Override
        public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
            return new TranslatingReadOnlyTransaction(dataBrokerService.newReadOnlyTransaction(), translator, bi2ba);
        }

        @Override
        public DOMDataReadWriteTransaction newReadWriteTransaction() {
            return null;
        }

        @Override
        public DOMDataWriteTransaction newWriteOnlyTransaction() {
            return null;
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

    private static final class TranslatingReadOnlyTransaction implements DOMDataReadOnlyTransaction {

        private ReadOnlyTransaction deviceSpecificTx;
        private Translator translator;
        private Bi2Ba bi2ba;

        public TranslatingReadOnlyTransaction(final ReadOnlyTransaction readOnlyTransaction, final Translator translator, final Bi2Ba bi2ba) {
            this.deviceSpecificTx = readOnlyTransaction;
            this.translator = translator;
            this.bi2ba = bi2ba;
        }

        @Override
        public void close() {
            deviceSpecificTx.close();
        }

        @Nonnull
        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType logicalDatastoreType,
                                                                                       final YangInstanceIdentifier yangInstanceIdentifier) {
            final InstanceIdentifier<? extends DataObject> instanceIdentifier = bi2ba.toBA(yangInstanceIdentifier);
            final InstanceIdentifier<? extends DataObject> translatedIId = translator.toDeviceSpecific(instanceIdentifier);

            final CheckedFuture<? extends Optional<? extends DataObject>, ReadFailedException> read = deviceSpecificTx.read(logicalDatastoreType, translatedIId);
            return Futures.makeChecked(Futures.transform(read, new Function<Optional<? extends DataObject>, Optional<NormalizedNode<?, ?>>>() {

                @Override
                public Optional<NormalizedNode<?, ?>> apply(final Optional<? extends DataObject> input) {
                    if (input.isPresent()) {
                        final DataObject dataObject = input.get();
                        final Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> translated = translator.fromDeviceSpecific(translatedIId, dataObject);
                        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> bi = bi2ba.toBI(translated.getKey(), translated.getValue());
                        return Optional.<NormalizedNode<?, ?>>of(bi.getValue());
                    } else {
                        return Optional.absent();
                    }
                }
            }), READ_FAILED_EXCEPTION_FUNCTION);
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
            }), READ_FAILED_EXCEPTION_FUNCTION);
        }

        @Nonnull
        @Override
        public Object getIdentifier() {
            return deviceSpecificTx.getIdentifier();
        }
    }

    /**
     * TODO implement
     */
    private static final class Bi2Ba implements AutoCloseable {

        @Nonnull
        private YangInstanceIdentifier toBI(final InstanceIdentifier<? extends DataObject> newId) {
            // translate from BA to BI
            return null;
        }

        @Nonnull
        private Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toBI(final InstanceIdentifier<? extends DataObject> newId, DataObject data) {
            // translate from BA to BI
            return null;
        }

        @Nonnull
        private Map.Entry<InstanceIdentifier<?>, DataObject> toBA(final YangInstanceIdentifier newId, NormalizedNode<?,?> data) {
            // translate from BA to BI
            return null;
        }

        @Nonnull
        private InstanceIdentifier<? extends DataObject>  toBA(final YangInstanceIdentifier newId) {
            // translate from BA to BI
            return null;
        }

        @Override
        public void close() throws Exception {

        }
    }

    /****************** EXAMPLE usage *****************/

    public static void main(String[] args) {
        // Comes from MD-SAL
        final MountPointService mountService = null;
        // We need also BI version of Mountpoint service, because the translating proxies need to be BI not BA in order
        // to be supported throughout ODL
        final DOMMountPointService domMountService = null;

        // We can create proxy schema context from generated binding classes
        // Here we are using xr models from module present here in ncmount "xrmodels"
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(Lists.newArrayList(
                $YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.$YangModuleInfoImpl.getInstance(),
        ));
        final SchemaContext proxySchemaContext = moduleInfoBackedContext.tryToCreateSchemaContext().get();

        // Since we create mount point proxies, we need instance ID for them ... and we need to ask user
        // to create a new InstanceId (ideally based on the old one) e.g. mounted node with id xrvr1 could be xrvr1-as-xr
        final InstanceIdTranslator idTranslator = new InstanceIdTranslator() {

            // TODO finish impl
            @Override
            public InstanceIdentifier<?> apply(final InstanceIdentifier<?> input) {
//                if(isInTopology(input)) {
//                    return newInstanceIDwithSuffix
//                }

                return null;
            }
        };

        final Translator translator = new Translator() {

            @Override
            public boolean isApplicable(@Nonnull final SchemaContext deviceSpecificSchema) {
                boolean is = true;
                is &= deviceSpecificSchema.findModuleByName("ietf-interfaces", QName.parseRevision("2012-12-12")) != null;
                is &= deviceSpecificSchema.findModuleByName("ietf-interfaces-oper", QName.parseRevision("2012-12-12")) != null;

                return is;
            }

            @Nonnull
            @Override
            public InstanceIdentifier<? extends DataObject> toDeviceSpecific(
                    @Nonnull final InstanceIdentifier<? extends DataObject> instanceIdentifier) {
                // Put Yang2Yang translation here
                return null;
            }

            @Nonnull
            @Override
            public Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> fromDeviceSpecific(
                    @Nonnull final InstanceIdentifier<? extends DataObject> translatedIId, @Nonnull final DataObject dataObject) {
                // Put Yang2Yang translation here
                return null;
            }
        };

        // Initialize automatic MountPoint proxy factory for proxies with translation from Cisco-IOS models into ietf models
        new TranslatingMountPointFactory(mountService, domMountService, proxySchemaContext, idTranslator, translator);
    }
}
