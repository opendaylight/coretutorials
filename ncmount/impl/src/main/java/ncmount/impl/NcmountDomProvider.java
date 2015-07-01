package ncmount.impl;

import io.netty.util.internal.MpscLinkedQueueNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.show.node.output.IfCfgData;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

public class NcmountDomProvider implements Provider, AutoCloseable, DOMRpcImplementation, DOMDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(NcmountDomProvider.class);

    public static final YangInstanceIdentifier NETCONF_TOPO_IID;
    private static final DOMRpcIdentifier SHOW_NODE_RPC_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create(ShowNodeInput.QNAME, "show-node"))); 
    private static final DOMRpcIdentifier LIST_NODES_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "list-nodes")));
    private static final DOMRpcIdentifier WRITE_NODES_ID = DOMRpcIdentifier.create(SchemaPath.create(true, QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "write-routes")));

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

        YangInstanceIdentifier iid = null;

        mountPointService.getMountPoint(iid).get().getService(DOMDataBroker.class).get().newReadOnlyTransaction();
        mountPointService.getMountPoint(iid).get().getService(DOMRpcService.class).get().invokeRpc(null, null);
        mountPointService.getMountPoint(iid).get().getService(DOMNotificationService.class);
        return null;
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> listNodes() {
        // TODO: write the implementation for the List-Node.
        LOG.info(" invoked RPC List-Node");

        QName RPC_OUTPUT_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "output");
        QName NC_CONFIG_NODES = QName.create("urn:opendaylight:params:xml:ns:yang:ncmount", "2015-01-05", "nc-config-nodes");
        Collection<DataContainerChild<? extends PathArgument, ?>> result = new ArrayList<>();   	
        DOMDataReadOnlyTransaction rtx = this.globalDomDataBroker.newReadOnlyTransaction();

        try {
            // read the config datastore for the available nodes.
            NormalizedNode<?, ?> ncNodes = rtx.read(LogicalDatastoreType.CONFIGURATION, NETCONF_TOPO_IID).checkedGet().get();

            if (ncNodes instanceof MapEntryNode) {
                NodeIdentifier nodeid = new NodeIdentifier(Node.QNAME);
                DataContainerChild<? extends PathArgument, ?> data = ((MapEntryNode) ncNodes).getChild(nodeid).get();

                LOG.info("type:", data.getNodeType());

                if (data instanceof MapNode) {
                    for (MapEntryNode node : ((MapNode) data).getValue()) {
                        LOG.info("node : {}", node);



                        for(DataContainerChild<? extends PathArgument, ?> nodeChild : node.getValue()) {
                            if (nodeChild instanceof LeafNode) {
                                LOG.info("leaf node info : {}", nodeChild.getValue());
                                // created the leaf node to represent "leaf-list nc-config-nodes"
                                LeafNode<Object> ncConfigNode = ImmutableLeafNodeBuilder.create()
                                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NC_CONFIG_NODES))
                                        .withValue(nodeChild.getValue())
                                        .build();

                                // create the leaf-list "leaf-list nc-config.nodes". ???
                                // how to define the moduleName, line and path.
                                // LeafSetNode<?> nodeSet = new LeafListSchemaNodeBuilder(moduleName, line, qname, path)

                                result.add(ncConfigNode);
                            }
                        }
                    }
                }			
            }
        } catch (ReadFailedException e) {
            LOG.warn(e.toString());		
        }

        // pack the output and return.
        ContainerNode resultNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(RPC_OUTPUT_QNAME))
                .withValue(result)
                .build();
        LOG.info("return the value : {}", resultNode);

        return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult(resultNode));
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> writeNode(final NormalizedNode<?, ?> normalizedNode) {
        // TODO: write the implementation for the Write-Node.
        LOG.info("invoked RPC Write-Node");
        return null;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> asyncDataChangeEvent) {
        // TODO the data change has to be handled in the same way as in NcmountProvider
    }
}
