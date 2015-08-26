package ncmount.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.WriteRoutesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150107.InterfaceConfigurations;

public class NcmountDomProvider implements Provider, AutoCloseable, DOMRpcImplementation, DOMDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(NcmountDomProvider.class);

    public static final YangInstanceIdentifier NETCONF_TOPO_IID;
    private static final DOMRpcIdentifier SHOW_NODE_RPC_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create(ShowNodeInput.QNAME, "show-node")));
    private static final DOMRpcIdentifier LIST_NODES_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create(ListNodesOutput.QNAME, "list-nodes")));
    private static final DOMRpcIdentifier WRITE_NODES_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create(WriteRoutesInput.QNAME, "write-routes")));

    static {
        final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder builder =
                org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder();
        builder
        .node(NetworkTopology.QNAME)
        .node(Topology.QNAME)
        .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), TopologyNetconf.QNAME.getLocalName());

        NETCONF_TOPO_IID = builder.build();
    }

    private DOMMountPointService mountPointService;
    private DOMDataBroker globalDomDataBroker;

    @Override
    public void onSessionInitiated(final Broker.ProviderSession providerSession) {
        // get the DOM versions of MD-SAL services
        this.globalDomDataBroker = providerSession.getService(DOMDataBroker.class);
        this.mountPointService = providerSession.getService(DOMMountPointService.class);

        final DOMRpcProviderService service = providerSession.getService(DOMRpcProviderService.class);
        service.registerRpcImplementation(this, SHOW_NODE_RPC_ID, LIST_NODES_ID, WRITE_NODES_ID);

        final YangInstanceIdentifier nodeIid = YangInstanceIdentifier.builder(NETCONF_TOPO_IID).node(Node.QNAME).build();

        LOG.info("NcmountDomProvider is registered");

        this.globalDomDataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                nodeIid,
                this,
                AsyncDataBroker.DataChangeScope.SUBTREE);
    }


    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        // Deprecated, not using
        return Collections.emptySet();
    }

    @Override
    public void close() throws Exception {
        // TODO close resources
        this.globalDomDataBroker = null;
        this.mountPointService = null;
    }

    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final DOMRpcIdentifier domRpcIdentifier, final NormalizedNode<?, ?> normalizedNode) {

        // I think, we can replace following with the switch
        // statement http://stackoverflow.com/questions/10240538/use-string-in-switch-case-in-java
        if(domRpcIdentifier.equals(SHOW_NODE_RPC_ID)) {
            return showNode(normalizedNode);
        }

        if (domRpcIdentifier.equals(LIST_NODES_ID)) {
            return listNodes();
        }

        if (domRpcIdentifier.equals(WRITE_NODES_ID)) {
            return writeNode(normalizedNode);
        }

        return null;
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> showNode(final NormalizedNode<?, ?> normalizedNode) {
        // TODO handle

        // Normalized node is the generic supertype for all normalized nodes e.g. container node, map node etc.
        // Its really important to know the normalized node structure when trying to parse/itarate over normalized nodes

        String node_name = (String) ((DataContainerNode<?>) normalizedNode).getChild(new NodeIdentifier(QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "node-name"))).get().getValue();
        YangInstanceIdentifier iid = YangInstanceIdentifier.builder(NETCONF_TOPO_IID).node(Node.QNAME).nodeWithKey(Node.QNAME, QName.create(Node.QNAME, "node-id"), node_name).build();

        Optional<DOMMountPoint> optionalDomMountPoint = mountPointService.getMountPoint(iid);

        if(optionalDomMountPoint.isPresent()) {
            DOMMountPoint domMountPoint = optionalDomMountPoint.get();

            // get the data broker for the mounted point.
            DOMDataBroker dataBroker = domMountPoint.getService(DOMDataBroker.class).get();

            final DOMDataReadOnlyTransaction rtx = dataBroker.newReadOnlyTransaction();
            
            YangInstanceIdentifier inter = YangInstanceIdentifier.builder().node(InterfaceConfigurations.QNAME).build();
            Optional<NormalizedNode<?, ?>> readInfo = null;
            try {
                readInfo = rtx.read(LogicalDatastoreType.CONFIGURATION, inter).checkedGet();

            } catch (ReadFailedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            domMountPoint.getService(DOMRpcService.class).get().invokeRpc(null, null);
            domMountPoint.getService(DOMNotificationService.class);
        }
        return null;
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> listNodes() {
        LOG.info(" invoked RPC List-Node");

        QName RPC_OUTPUT_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "output");
        QName NC_CONFIG_NODES = QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "nc-config-nodes");
        QName NC_OPER_NODES = QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "nc-oper-nodes");
        DOMDataReadOnlyTransaction rtx = this.globalDomDataBroker.newReadOnlyTransaction();
        NormalizedNode<?, ?> ncNodes = null;
        NodeIdentifier nodeid = new NodeIdentifier(Node.QNAME);

        // read the config datastore for the available nodes.
        try {
            ncNodes = rtx.read(LogicalDatastoreType.CONFIGURATION, NETCONF_TOPO_IID).checkedGet().get();
        } catch (ReadFailedException e) {
            return Futures.immediateFailedCheckedFuture((DOMRpcException)
                    new DOMRpcException("rpc invocation not implemented yet") {
            });
        }

        // created the leaf node to represent "leaf-list nc-config-nodes"
        ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafListBuilder = Builders.leafSetBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NC_CONFIG_NODES));

        if (ncNodes instanceof MapEntryNode) {

            // get all the nodes from the ncNode
            DataContainerChild<? extends PathArgument, ?> data = ((MapEntryNode) ncNodes).getChild(nodeid).get();

            if (data instanceof MapNode) {
                for (MapEntryNode node : ((MapNode) data).getValue()) {

                    for(DataContainerChild<? extends PathArgument, ?> nodeChild : node.getValue()) {
                        if (nodeChild instanceof LeafNode) {
                            LOG.info("leaf node info : {}", nodeChild.getValue());
                            // add the node value to the leaf-list.
                            leafListBuilder.withChildValue(nodeChild.getValue());
                        }
                    }
                }
            }
        }

        // read and create the leaf node to represent "leaf-list nc-oper-nodes"
        NormalizedNode<?, ?> topology = null;

        try {
            topology = rtx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID).checkedGet().get();
            LOG.info("NETCONF_TOPO_IID : {}", topology);
        } catch (ReadFailedException e) {
            LOG.warn(e.toString());
        }

        // created the leaf node to represent "leaf-list nc-config-nodes"
        ListNodeBuilder<Object, LeafSetEntryNode<Object>> ncOperLeafListBuilder = Builders.leafSetBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NC_OPER_NODES));

        LOG.info("openr: {}", topology);
        DataContainerChild<? extends PathArgument, ?> nodeList = ((MapEntryNode) topology).getChild(new NodeIdentifier(Node.QNAME)).get();

        for (MapEntryNode operNode : ((MapNode)nodeList).getValue()) {

            // pick the leaf node with local name "node-id"
            String nodeId = ((String) operNode
                .getChild(new NodeIdentifier(QName.create(Topology.QNAME, "node-id"))).get().getValue());
            LOG.info("oper node id: {}", nodeId);

            final Optional<DataContainerChild<? extends PathArgument, ?>> netconfNode = operNode.getChild(
                // TODO the augmentation identifier could be extracted into a static constant
                new YangInstanceIdentifier.AugmentationIdentifier(Sets.newHashSet(
                    toQNames(NetconfNode.QNAME, "connection-status", "connected-message", "pass-through", "port",
                        "host", "available-capabilities", "unavailable-capabilities"))));

            if(!netconfNode.isPresent()) {
                // Skipping non netconf nodes, even though this should not happen, since we are querying netconf topology
                LOG.debug("Skipping non netconf node {}", nodeId);
                continue;
            }

            final AugmentationNode netconfNodeParameters = (AugmentationNode) netconfNode.get();
            final LeafNode<?> connectionStatus = ((LeafNode<?>) netconfNodeParameters
                .getChild(new NodeIdentifier(toQName(NetconfNode.QNAME, "connection-status"))).get());

            if("connected".equals(connectionStatus.getValue())) {
                final ContainerNode availableCapabilities = ((ContainerNode) netconfNodeParameters
                    .getChild(new NodeIdentifier(toQName(NetconfNode.QNAME, "available-capabilities"))).get());
                final LeafSetNode<?> availableCapability = (LeafSetNode<?>) availableCapabilities
                    .getChild(new NodeIdentifier(toQName(NetconfNode.QNAME, "available-capability"))).get();

                LOG.warn("Capabilities of {} : {}", nodeId,
                    Collections2.transform(availableCapability.getValue(), new Function<LeafSetEntryNode<?>, Object>() {
                        @Nullable @Override public Object apply(final LeafSetEntryNode<?> input) {
                            return input.getValue();

                        }
                    }));
            }

            // include the node in the nc_oper_leaf_list
            ncOperLeafListBuilder.withChildValue(nodeId);
        }

        // pack the output and return.
        ContainerNode resultNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(RPC_OUTPUT_QNAME))
                .withChild(leafListBuilder.build())
                .withChild(ncOperLeafListBuilder.build())
                .build();
        LOG.info("return the value : {}", resultNode);

        return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult(resultNode));
    }

    private static Collection<QName> toQNames(final QName baseQName, String... localNames) {
        return Collections2.transform(Arrays.asList(localNames), new Function<String, QName>() {
            @Override public QName apply(final String input) {
                return QName.cachedReference(QName.create(baseQName, input));
            }
        });
    }

    private static QName toQName(final QName baseQName, String localName) {
        return QName.cachedReference(QName.create(baseQName, localName));
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> writeNode(final NormalizedNode<?, ?> normalizedNode) {
        // TODO: write the implementation for the Write-Node.
        LOG.info("invoked RPC Write-Node");
        return null;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        // TODO the data change has to be handled in the same way as in NcmountProvider
        LOG.info("data changeed: {}", change);

        //        for ( Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry : change.getCreatedData().entrySet()) {
        //            if (entry.getKey().toString() == NetconfNode.QNAME.toString()) {
        //
        //                // Not much can be done at this point, we need UPDATE event with
        //                // state set to connected
        //            }
        //            LOG.info("NETCONF Node: {} was created, key : {} , value : {}", entry, entry.getKey(), entry.getValue());
        //
        //        }
            }
}
