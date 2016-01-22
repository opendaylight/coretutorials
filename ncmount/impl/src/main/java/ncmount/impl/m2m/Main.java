package ncmount.impl.m2m;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import ncmount.impl.m2m.spi.Translator;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150107.$YangModuleInfoImpl;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class Main {

    static final Function<Exception, ReadFailedException> READ_FAILED_EXCEPTION_FUNCTION = new Function<Exception, ReadFailedException>() {
        @Nullable
        @Override
        public ReadFailedException apply(@Nullable final Exception input) {
            return input instanceof ReadFailedException ?
                    ((ReadFailedException) input) :
                    new ReadFailedException("Read failed in proxy", input);
        }
    };


    /****************** EXAMPLE usage *****************/
    /*
        This initializes TranslatingMountPointFactory and adds a single translator to it.
        The translator is very simple and for a single specific Node, performs a trivial translation.
        Other operations are sent directly to the device.
     */

    public static void main(String[] args) {
        // Comes from MD-SAL
        // We need also BI version of Mountpoint service, because the translating proxies need to be BI not BA in order
        // to be supported throughout ODL
        final DOMMountPointService domMountService = null;

        // We can create proxy schema context from generated binding classes
        // Here we are using xr models from module present here in ncmount "xrmodels"
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(Lists.newArrayList(
                $YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107.$YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.$YangModuleInfoImpl.getInstance()
        ));
        final SchemaContext proxySchemaContext = moduleInfoBackedContext.tryToCreateSchemaContext().get();

        // Since we create mount point proxies, we need instance ID for them ... and we need to ask user
        // to create a new InstanceId (ideally based on the old one) e.g. mounted node with id xrvr1 could be xrvr1-as-xr

        final Translator translator = new ExampleTranslator();

        // Initialize automatic MountPoint proxy factory for proxies with translation from Cisco-IOS models into ietf models
        new TranslatingMountPointFactory(domMountService, proxySchemaContext, translator);
    }

    private static class ExampleTranslator implements Translator {

        @Override
        public boolean isApplicable(@Nonnull final SchemaContext deviceSpecificSchema) {
            boolean is = true;
            is &= deviceSpecificSchema.findModuleByName("ietf-interfaces", QName.parseRevision("2012-12-12")) != null;
            is &= deviceSpecificSchema.findModuleByName("ietf-interfaces-oper", QName.parseRevision("2012-12-12")) != null;

            return is;
        }

        @Override
        public YangInstanceIdentifier translateId(final YangInstanceIdentifier mountPointId) {
//                if(isFromTopology(input)) {
//                    return newInstanceIDwithSuffix
//                }

            return null;
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> fromDeviceSpecific(
                @Nonnull final LogicalDatastoreType logicalDatastoreType,
                @Nonnull final YangInstanceIdentifier yangInstanceIdentifier,
                @Nonnull final DOMDataReadOnlyTransaction deviceSpecificTx) throws TranslationException {

            /**
             * If we're putting specific Node than perform translation on YID and returned data otherwise direct read
             */
            if(isNode(yangInstanceIdentifier)) {
                final YangInstanceIdentifier deviceSpecificId = NodeTranslation.to(yangInstanceIdentifier);
                final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
                        deviceSpecificTx.read(logicalDatastoreType, deviceSpecificId);
                return Futures.makeChecked(Futures.transform(read, new Function<Optional<NormalizedNode<?,?>>, Optional<NormalizedNode<?, ?>>>() {

                    @Override
                    public Optional<NormalizedNode<?, ?>> apply(final Optional<NormalizedNode<?, ?>> input) {
                        if(input.isPresent()) {
                            final NormalizedNode<?, ?> normalizedNode = input.get();
                            return Optional.<NormalizedNode<?, ?>>of(NodeTranslation.from(((MapEntryNode) normalizedNode)));
                        } else {
                            return input;
                        }
                    }
                }), READ_FAILED_EXCEPTION_FUNCTION);
            } else {
                return deviceSpecificTx.read(logicalDatastoreType, yangInstanceIdentifier);
            }
        }

        @Override
        public void putDeviceSpecific(@Nonnull final LogicalDatastoreType logicalDatastoreType,
                                      @Nonnull final YangInstanceIdentifier yangInstanceIdentifier,
                                      @Nonnull final NormalizedNode<?, ?> normalizedNode,
                                      @Nonnull final DOMDataWriteTransaction deviceSpecificTx)
                throws TranslationException {
            /**
             * If we're putting specific Node than perform translation otherwise direct put
             */
            if(isNode(yangInstanceIdentifier)) {
                final MapEntryNode deviceSpecificData = NodeTranslation.to(((MapEntryNode) normalizedNode));
                final YangInstanceIdentifier deviceSpecificId = NodeTranslation.to(yangInstanceIdentifier);
                deviceSpecificTx.put(logicalDatastoreType, deviceSpecificId, deviceSpecificData);
            } else {
                deviceSpecificTx.put(logicalDatastoreType, yangInstanceIdentifier, normalizedNode);
            }
        }

        public boolean isNode(final YangInstanceIdentifier yangInstanceIdentifier) {
            // FIXME
            return true;
        }
    }

    /************************* This is how the generated model2model translating code might look like ****/

    // TODO THis is example how generated model2model translation code could look like
    // It assumes that 1 node is mapped always to 1 node
    // However, there might be much more complicated mappings e.g. Put in one model might result into N operations of
    // different types even delete. So the generated code should not assume 1-1 mapping, but for 1 operation return
    // (or perform) list of necessary operations (whatever they might be).

    static class NodeTranslation {

        static MapEntryNode from(MapEntryNode legacyNode) {
            final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> builder
                    = Builders.mapEntryBuilder(/*schema node of the new node can go here*/);
            final YangInstanceIdentifier.NodeIdentifier childPath = new YangInstanceIdentifier.NodeIdentifier(
                    QName.create("namespace", "newLeaf"));
            final YangInstanceIdentifier.NodeIdentifier legacyChildPath = new YangInstanceIdentifier.NodeIdentifier(
                    QName.create("namespace", "legacyLeaf"));

            builder.withChild(
                    Builders.leafBuilder()
                            .withNodeIdentifier(childPath)
                            // here we set the value from legacy node with legacy name into new Leaf
                            .withValue(legacyNode.getChild(legacyChildPath))
                            .build());

            return builder.build();
        }

        static MapEntryNode to(MapEntryNode newNode) {
            // TODO should be exact opposite of from
            return null;
        }

        static YangInstanceIdentifier to(YangInstanceIdentifier newIdentifier) {
            // Schema structure did not change, yang ID is the same in legacy and new
            return newIdentifier;
        }

        static YangInstanceIdentifier from(YangInstanceIdentifier legacyIdentifier) {
            // Schema structure did not change, yang ID is the same in legacy and new
            return legacyIdentifier;
        }

    }
}
