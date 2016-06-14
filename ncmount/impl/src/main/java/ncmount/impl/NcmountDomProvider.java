/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
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

/**
 * This class is Binding Independent version of NcmountProvider
 * and provide the implementation of ncmount application.
 *
 * BI - It uses a neutral data DOM format for data and API calls,
 *      which is independent of generated Java language bindings.
 *
 * BA - It uses code generated both at development time and at runtime.
 *
 *  Here in the BI implementation, we used the Qname to access the DOM nodes
 *  e.g. ((MapEntryNode) topology).getChild(new NodeIdentifier(Node.QNAME)).
 *
 *  While in BA implementation we can directly use the generated bindings
 *  from the yang model to access the nodes.
 *
 *  One can follow the following link to understand the difference between BI and BA :
 *  https://ask.opendaylight.org/question/998/binding-independent-and-binding-aware-difference/
 */
public class NcmountDomProvider implements Provider, AutoCloseable, DOMRpcImplementation, DOMDataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(NcmountDomProvider.class);

    private static final DOMRpcIdentifier SHOW_NODE_RPC_ID = SchemaPath.create(true, QName.create(ShowNodeInput.QNAME, "show-node")).intern();
    private static final DOMRpcIdentifier LIST_NODES_ID = SchemaPath.create(true, QName.create(ListNodesOutput.QNAME, "list-nodes")),intern();
    private static final DOMRpcIdentifier WRITE_NODES_ID = SchemaPath.create(true, QName.create(WriteRoutesInput.QNAME, "write-routes")).intern();

    // Qname used to construct the output for the list-node rpc.
    static final QName RPC_OUTPUT_QNAME = QName.create(ListNodesOutput.QNAME, "list-nodes").intern();
    static final QName NC_CONFIG_NODES = QName.create(ListNodesOutput.QNAME, "nc-config-nodes").intern();
    static final QName NC_OPER_NODES = QName.create(ListNodesOutput.QNAME, "nc-oper-nodes").intern();

    private static NodeIdentifier TOPO_NODE_ID_PATHARG = new NodeIdentifier(QName.create(Topology.QNAME, "node-id").intern());

    public static final YangInstanceIdentifier NETCONF_TOPO_IID = YangInstanceIdentifier.builder()
            .node(NetworkTopology.QNAME).node(Topology.QNAME)
            .nodeWithKey(Topology.QNAME, QName.create(Topology.QNAME, "topology-id").intern(), TopologyNetconf.QNAME.getLocalName())
            .build();
    }

    private DOMMountPointService mountPointService;
    private DOMDataBroker globalDomDataBroker;

    /**
     * This method initializes DomDataBroker and Mountpoint service.
     * This services needed throughout the lifetime of the ncmount
     * application and registers its RPC implementation and Data change
     * Listener with the MD-SAL.
     */
    @Override
    public void onSessionInitiated(final Broker.ProviderSession providerSession) {
        // get the DOM versions of MD-SAL services
        this.globalDomDataBroker = providerSession.getService(DOMDataBroker.class);
        this.mountPointService = providerSession.getService(DOMMountPointService.class);

        final DOMRpcProviderService service = providerSession.getService(DOMRpcProviderService.class);
        service.registerRpcImplementation(this, SHOW_NODE_RPC_ID, LIST_NODES_ID, WRITE_NODES_ID);

        LOG.info("NcmountDomProvider is registered");

        this.globalDomDataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                NETCONF_TOPO_IID.node(Node.QNAME),
                this,
                AsyncDataBroker.DataChangeScope.SUBTREE);
    }


    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        // Deprecated, not using
        return Collections.emptySet();
    }

    @Override
    public void close() {
        this.globalDomDataBroker = null;
        this.mountPointService = null;
    }

    /**
     * This method is invoked on RPC invocation of the registered
     * method. domRpcIdentifier(localname) is used to invoke the
     * correct requested method.
     */
    @Nonnull
    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(@Nonnull final DOMRpcIdentifier domRpcIdentifier, final NormalizedNode<?, ?> normalizedNode) {

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

    /**
     * This method is the implementation of the 'show-node' RESTCONF service,
     * which is one of the external APIs into the ncmount application. The
     * service provides two example functions:
     * 1. Browse through a subset of a mounted node's configuration data
     * 2. Browse through  a subset of a mounted node's operational data
     *
     * @param input Input parameter from the show-node service yang model -
     *              the node's configured name
     * @return      Retrieved configuration and operational data
     */
    private CheckedFuture<DOMRpcResult, DOMRpcException> showNode(final NormalizedNode<?, ?> normalizedNode) {
        LOG.info("invoked showNode: {}", normalizedNode);

        // TODO: Method need to be implemented.
        return Futures.immediateFailedCheckedFuture((DOMRpcException)new MethodNotImplemented("method not implemented"));
    }


    /**
     * This method reads the config data-store and return list of nodes
     * from Netconf topology.
     *
     * @return list of nodes in Netconf topology from config data store.
     */
    private LeafSetNode<Object> getNCConfigNodes() {
        DOMDataReadOnlyTransaction rtx = this.globalDomDataBroker.newReadOnlyTransaction();
        NormalizedNode<?, ?> ncNodes = null;
        NodeIdentifier nodeid = new NodeIdentifier(Node.QNAME);

        // created the leaf node to represent "leaf-list nc-config-nodes"
        ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafListBuilder = Builders.leafSetBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NC_CONFIG_NODES));

        // read the config data-store for the available nodes.
        try {
            Optional<NormalizedNode<?, ?>> optNCNodes = rtx.read(LogicalDatastoreType.CONFIGURATION, NETCONF_TOPO_IID)
                                                           .checkedGet();

            // if the nodes are present then only move further
            // otherwise return the empty leaf list.
            if (optNCNodes.isPresent()) {
                ncNodes = optNCNodes.get();
            } else {
                return leafListBuilder.build();
            }

        } catch (ReadFailedException e) {
            LOG.warn("Failed to read config datastore: {}", e);
            return leafListBuilder.build();
        } finally {
            // close the transaction resource.
            rtx.close();
        }

        if (ncNodes instanceof MapEntryNode) {
            // get all the nodes from the ncNode
            DataContainerChild<? extends PathArgument, ?> data = ((MapEntryNode) ncNodes).getChild(nodeid).get();

            if (data instanceof MapNode) {
                for (MapEntryNode node : ((MapNode) data).getValue()) {

                    for(DataContainerChild<? extends PathArgument, ?> nodeChild : node.getValue()) {
                        if (nodeChild instanceof LeafNode) {
                            // add the node value to the leaf-list.
                            leafListBuilder.withChildValue(nodeChild.getValue());
                        }
                    }
                }
            }
        }

        return leafListBuilder.build();
    }

    /**
     * This method reads the operational data-store and return list of nodes
     * from Netconf topology. It also logs the capability of 'connected'
     * netconf nodes.
     *
     * @return list of nodes in Netconf topology from operational data store.
     */
    private LeafSetNode<Object> getNCOperationalNodes() {

        // read and create the leaf node to represent "leaf-list nc-oper-nodes"
        ListNodeBuilder<Object, LeafSetEntryNode<Object>> ncOperLeafListBuilder = Builders.leafSetBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NC_OPER_NODES));
        NormalizedNode<?, ?> topology = null;

        DOMDataReadOnlyTransaction rtx = this.globalDomDataBroker.newReadOnlyTransaction();
        try {
            Optional<NormalizedNode<?, ?>> optTopology = rtx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID)
                    .checkedGet();

            // if the topology is present the move further,
            // otherwise return with the empty leaf-list of nc node.
            if (optTopology.isPresent()) {
                topology = optTopology.get();
            } else {
                return ncOperLeafListBuilder.build();
            }

        } catch (ReadFailedException e) {
            LOG.warn("Failed to read operational datastore: {}", e);
            return ncOperLeafListBuilder.build();
        } finally {
            // close the transaction resource.
            rtx.close();
        }

        DataContainerChild<? extends PathArgument, ?> nodeList = ((MapEntryNode) topology).getChild(new NodeIdentifier(Node.QNAME))
                                                                                          .get();
        for (MapEntryNode operNode : ((MapNode)nodeList).getValue()) {

            // pick the leaf node with local name "node-id"
            String nodeId = ((String) operNode
                    .getChild(TOPO_NODE_ID_PATHARG).get().getValue());

            final Optional<DataContainerChild<? extends PathArgument, ?>> netconfNode = operNode.getChild(
                    // TODO the augmentation identifier could be extracted into a static constant
                    new YangInstanceIdentifier.AugmentationIdentifier(Sets.newHashSet(
                            toQNames(NetconfNode.QNAME, "tcp-only", "available-capabilities", "port",
                                    "reconnect-on-changed-schema", "connected-message",
                                    "default-request-timeout-millis", "host", "max-connection-attempts",
                                    "connection-status", "credentials", "unavailable-capabilities",
                                    "between-attempts-timeout-millis", "keepalive-delay",
                                    "clustered-connection-status","yang-module-capabilities", "pass-through",
                                    "connection-timeout-millis", "sleep-factor"))));

            if(!netconfNode.isPresent()) {
                // Skipping non netconf nodes, even though this should not happen,
                // since we are querying netconf topology
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

        return ncOperLeafListBuilder.build();
    }

    /**
     * This method is the implementation of the 'list-nodes' RESTCONF service,
     * which is one of the external APIs into the ncmount application. The
     * service provides two example functions:
     * 1. Lists nodes in the Netconf Topology's configuration data
     * 2. Lists nodes in the Netconf Topology's operational data
     *
     * Netconf Topology is populated by the Netconf Connector.  Configuration
     * data contains the configuration data for each netconf node configured in
     * the Netconf Connector. Operational data contains status data for each
     * netconf node configured in the Netconf Connector.
     *
     * @return Lists of nodes found in Netconf Topology's configuration and
     *          operational spaces.
     */
    private CheckedFuture<DOMRpcResult, DOMRpcException> listNodes() {
        LOG.info(" invoked RPC List-Node");

        // created the leaf node to represent "leaf-list nc-config-nodes."
        LeafSetNode<Object> ncConfigLeafList = getNCConfigNodes();

        // created the leaf node to represent "leaf-list nc-oper-nodes"
        LeafSetNode<Object> ncOperLeafList = getNCOperationalNodes();


        // pack the output and return.
        ContainerNode resultNode = ImmutableContainerNodeBuilder.create()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(RPC_OUTPUT_QNAME))
                .withChild(ncConfigLeafList)
                .withChild(ncOperLeafList)
                .build();

        return Futures.immediateCheckedFuture((DOMRpcResult) new DefaultDOMRpcResult(resultNode));
    }

    private static Collection<QName> toQNames(final QName baseQName, String... localNames) {
        return Collections2.transform(Arrays.asList(localNames), new Function<String, QName>() {
            @Override public QName apply(final String input) {
                return QName.create(baseQName, input).intern();
            }
        });
    }

    private static QName toQName(final QName baseQName, String localName) {
        return QName.create(baseQName, localName).intern();
    }

    /**
     * Write list of routes to specified netconf device.
     * The resulting routes conform to Cisco-IOS-XR-ip-static-cfg.yang yang model.
     *
     * @param input Input list of simple routes
     * @return Success if routes were written to mounted netconf device
     */
    private CheckedFuture<DOMRpcResult, DOMRpcException> writeNode(final NormalizedNode<?, ?> normalizedNode) {
     // TODO: Method need to be implemented.
        LOG.info("invoked RPC Write-Node: {}", normalizedNode);
        return Futures.immediateFailedCheckedFuture((DOMRpcException)new MethodNotImplemented("method not implemented"));

    }

    /**
     * This method is ncmount's Data Change Listener on the Netconf Topology
     * namespace. It registers at the root of the Netconf Topology subtree for
     * changes in the entire subtree. At this point the method only logs the
     * data change to demonstrate the basic design pattern. A real application
     * would use the the data contained in the data change event to, for
     * example, maintain paths to connected netconf nodes.
     *
     * @param change Data change event
     */
    @Override
    public void onDataChanged(final AsyncDataChangeEvent<YangInstanceIdentifier, NormalizedNode<?, ?>> change) {
        // TODO: Method need to be implemented. The data change has to
        // be handled in the same way as in NcmountProvider.
        LOG.info("data changed: {}", change);
    }
}

/**
 * This class is used to create the exception for the
 * unimplemented methods.
 */
class MethodNotImplemented extends DOMRpcException {

    public MethodNotImplemented(String message) {
        super(message);
    }
}



