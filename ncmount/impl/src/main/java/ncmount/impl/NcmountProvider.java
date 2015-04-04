/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ncmount.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150107.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150107._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107.InterfaceProperties;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.DataNodes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.data.nodes.DataNode;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.data.nodes.data.node.Locationviews;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.data.nodes.data.node.locationviews.Locationview;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.table.Interfaces;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.table.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeFields.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.NcmountService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.show.node.output.IfCfgDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.show.node.output._if.cfg.data.Ifc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.show.node.output._if.cfg.data.IfcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.show.node.output._if.cfg.data.IfcKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is the implementation of the ncmount application.
 *
 * The skeleton for this class was generated with the MD-SAL application
 * archetype.
 *

 */
public class NcmountProvider implements DataChangeListener, NcmountService, 
                                    BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NcmountProvider.class);
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier
            .create(NetworkTopology.class)
            .child(Topology.class, 
                   new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
    private RpcRegistration<NcmountService> rpcReg;
    private ListenerRegistration<DataChangeListener> dclReg;
    private MountPointService mountService;
    private DataBroker dataBroker;

    /**
     * A method called when the session to MD-SAL is established. It initializes
     * references to MD-SAL services needed throughout the lifetime of the 
     * ncmount application and registers its RPC implementation and Data change
     * Listener with the MD-SAL 
     *
     * The skeleton for this method was generated with the MD-SAL application
     * archetype.
     *

     * @param session Reference to the established MD-SAL session
     * @return        None
     */
    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NcmountProvider Session Initiated");

        // Get references to the data broker and mount service
        this.mountService = session.getSALService(MountPointService.class);
        this.dataBroker = session.getSALService(DataBroker.class);

        // Register ourselves as the REST API RPC implementation
        this.rpcReg = session.addRpcImplementation(NcmountService.class, this);

        // Register ourselves as data change listener for changes on Netconf
        // nodes. Netconf nodes are accessed via "Netconf Topology" - a special
        // topology that is created by the system infrastructure. It contains
        // all Netconf nodes the Netconf connector knows about. NETCONF_TOPO_IID
        // is equivalent to the following URL:
        // .../restconf/operational/network-topology:network-topology/topology/topology-netconf
        if (dataBroker != null) {
            this.dclReg = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                    NETCONF_TOPO_IID.child(Node.class), 
                    this, 
                    DataChangeScope.SUBTREE);
        }
    }

     /**
      * A method called when the session to MD-SAL is closed. It closes
      * registrations in MD-SAL 
      * Listener with the MD-SAL 
      *
      * @param session Reference to the established MD-SAL session
      * @return        None
      */
    @Override
    public void close() throws Exception {
        LOG.info("NcmountProvider Closed");
        // Clean up the RPC service registration
        if (rpcReg != null) {
            rpcReg.close();
        }
        // Clean up the Data Change Listener registration
        if (dclReg != null) {
            dclReg.close();
        }

    }

    /**
     * This method is the implementation of the 'show-node' RESTCONF service,
     * which is one of the external APIs into the ncmount application. The 
     * service provides two example functions:
     * 1. Browse through a subset of a mounted node's configuration data
     * 2. Browse through  a subset of a mounted node's operational data
     *
     *
     * The signature for this method was generated by yang tools from the
     * ncmount API model.
     *
     * @param input Input parameter from the show-node service yang model -
     *              the node's configured name
     * @return      Retrieved configuration and operational data
     */
    @Override
    public Future<RpcResult<ShowNodeOutput>> showNode(ShowNodeInput input) {
        LOG.info("showNode called, input {}", input);

        // Get the mount point for the specified node
        // Equivalent to '.../restconf/<config | operational>/opendaylight-inventory:nodes/node/<node-name>/yang-ext:mount/'
        // Note that we can read both config and operational data from the same
        // mount point
        final Optional<MountPoint> xrNodeOptional = mountService.getMountPoint(NETCONF_TOPO_IID
                .child(Node.class, new NodeKey(new NodeId(input.getNodeName()))));

        Preconditions.checkArgument(xrNodeOptional.isPresent(),
                "Unable to locate mountpoint: %s, not mounted yet or not configured",
                input.getNodeName());
        final MountPoint xrNode = xrNodeOptional.get();

        // Get the DataBroker for the mounted node
        final DataBroker xrNodeBroker = xrNode.getService(DataBroker.class).get();
        // Start a new read only transaction that we will use to read data 
        // from the device
        final ReadOnlyTransaction xrNodeReadTx = xrNodeBroker.newReadOnlyTransaction();

        // EXAMPLE: Browsing through the node's interface configuration data.

        // First, we get an Instance Identifier for the configuration data. Note
        // that the Instance Identifier is relative to the mountpoint (we got
        // the path to the mountpoint above). The Instance Identifier path is
        // equivalent to:
        // '.../yang-ext:mount/Cisco-IOS-XR-ifmgr-cfg:interface-configurations'
        InstanceIdentifier<InterfaceConfigurations> iid =
                InstanceIdentifier.create(InterfaceConfigurations.class);

        Optional<InterfaceConfigurations> ifConfig;
        try {
            // Read from a transaction is asynchronous, but a simple
            // get/checkedGet makes the call synchronous
            ifConfig = xrNodeReadTx.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unexpected error reading data from " + input.getNodeName(), e);
        }

        List<Ifc> ifcList = new ArrayList<Ifc>();
        if (ifConfig.isPresent()) {
            List<InterfaceConfiguration> ifConfigs = ifConfig
                                                     .get()
                                                     .getInterfaceConfiguration();
            for (InterfaceConfiguration config : ifConfigs) {
                LOG.info("Config for '{}': config {}", 
                        config.getInterfaceName().getValue(), config);
                String ifcActive = config.getActive().getValue();
                String ifcName = config.getInterfaceName().getValue();
                ifcList.add(new IfcBuilder()
                                .setActive(ifcActive)
                                .setBandwidth(config.getBandwidth())
                                .setDescription(config.getDescription())
                                .setInterfaceName(ifcName)
                                .setLinkStatus(config.isLinkStatus() == Boolean.TRUE ? "Up" : "Down")
                                .setKey(new IfcKey(ifcActive, ifcName))
                                .build());
            }
        } else {
            LOG.info("No data present on path '{}' for mountpoint: {}", 
                    iid, 
                    input.getNodeName());
        }

        // EXAMPLE: Browsing through the node's interface operational data

        // First, we get an Instance Identifier for the portion of the 
        // operational data that we want to browse through. Note that we are 
        // getting an identifier to a more specific path - the data-nodes 
        // container within the interface-properties container. The Instance 
        // Identifier path is equivalent to:
        // '.../yang-ext:mount/Cisco-IOS-XR-ifmgr-oper:interface-properties/data-nodes'
        InstanceIdentifier<DataNodes> idn = InstanceIdentifier.create(InterfaceProperties.class)
                                                              .child(DataNodes.class);
        Optional<DataNodes> ldn;
        try {
            ldn = xrNodeReadTx.read(LogicalDatastoreType.OPERATIONAL, idn).checkedGet();
        } catch (ReadFailedException e) {
            throw new IllegalStateException("Unexpected error reading data from " + input.getNodeName(), e);
        }

        if (ldn.isPresent()) {
            List<DataNode> dataNodes = ldn.get().getDataNode();
            for (DataNode node : dataNodes) {
                LOG.info("DataNode '{}'", node.getDataNodeName().getValue());

                Locationviews lw = node.getLocationviews();
                List<Locationview> locationViews = lw.getLocationview();
                for (Locationview view : locationViews) {
                    LOG.info("LocationView '{}': {}",
                            view.getKey().getLocationviewName().getValue(),
                            view);
                }

                Interfaces ifc = node.getSystemView().getInterfaces();
                List<Interface> ifList = ifc.getInterface();
                for (Interface intf : ifList) {
                    LOG.info("Interface '{}': {}",
                            intf.getInterface().getValue(), intf);
                }

            }
        } else {
            LOG.info("No data present on path '{}' for mountpoint: {}",
                    idn, input.getNodeName());
        }

        // Finally, we build the RPC response with the retrieved data and return
        ShowNodeOutput output = new ShowNodeOutputBuilder()
                                    .setIfCfgData(new IfCfgDataBuilder()
                                                        .setIfc(ifcList)
                                                        .build())
                                    .build();
        return RpcResultBuilder.success(output).buildFuture();
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
     * The signature for this method was generated by yang tools from the
     * ncmount API model.
     *
     * @param   None
     * @return  Lists of nodes found in Netconf Topology's configuration and 
     *          operational spaces
     */
    @Override
    public Future<RpcResult<ListNodesOutput>> listNodes() {
        // Use this method for one-off operations, when you need to find out
        // something about the nodes currently in the Netconf Topology. An
        // application that needs to handle netconf node discovery/disappearance,
        // a data change listener over Netconf topology should be used.
        List<Node> nodes;
        ListNodesOutputBuilder outBld = new ListNodesOutputBuilder();

        ReadTransaction tx = dataBroker.newReadOnlyTransaction();

        // EXAMPLE: Get all the nodes from the Netconf Topology configuration
        // space.
        try {
            nodes = tx.read(LogicalDatastoreType.CONFIGURATION, NETCONF_TOPO_IID)
                                .checkedGet()
                                .get()
                                .getNode();
        } catch (ReadFailedException e) {
            LOG.error("Failed to read node config from datastore", e);
            throw new IllegalStateException(e);
        }

        List<String> results = new ArrayList<String>();
        for (Node node : nodes) {
            LOG.info("Node: {}", node);
            results.add(node.getNodeId().getValue());
        }
        outBld.setNcConfigNodes(results);

        try {
            nodes = tx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID)
                                .checkedGet()
                                .get()
                                .getNode();
        } catch (ReadFailedException e) {
            LOG.error("Failed to read node config from datastore", e);
            throw new IllegalStateException(e);
        }

        results = new ArrayList<String>();
        for (Node node : nodes) {
            LOG.info("Node: {}", node);
            NetconfNode nnode = node.getAugmentation(NetconfNode.class);
            if (nnode != null) {
                // We have a Netconf device
                ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == ConnectionStatus.Connected) {
                    List<String> capabilities = nnode
                                                .getAvailableCapabilities()
                                                .getAvailableCapability();
                    LOG.info("Capabilities: {}", capabilities);
                }
            }
            results.add(node.getNodeId().getValue());
        }
        outBld.setNcOperNodes(results);

        return RpcResultBuilder.success(outBld.build()).buildFuture();
    }

    /**
     * This method is ncmount's Data Change Listener on the Netconf Topology 
     * namespace. It registers at the root of the Netconf Topology subtree for 
     * changes in the entire subtree. At this point the method only logs the 
     * data change to demonstrate the basic design pattern. A real application 
     * would use the the data contained in the data change event to, for 
     * example, maintain paths to connected netconf nodes.
     *
     * The skeleton for this method was generated with the MD-SAL application
     * archetype.
     *
     * @param change Data change event
     * @return  None
     */
     @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // We need to handle the following types of events:
        // 1. Discovery of new nodes
        // 2. Status change in existing nodes
        // 3. Removal of existing nodes
        LOG.info("OnDataChange, change: {}", change);

        // EXAMPLE: New node discovery
        // React to new Netconf nodes added to the Netconf topology or existing
        // Netconf nodes deleted from the Netconf topology
        for ( Entry<InstanceIdentifier<?>,
                DataObject> entry : change.getCreatedData().entrySet()) {
            if (entry.getKey().getTargetType() == NetconfNode.class) {
                NodeId nodeId = getNodeId(entry.getKey());
                LOG.info("NETCONF Node: {} was created", nodeId.getValue());

                // Not much can be done at this point, we need UPDATE event with
                // state set to connected
            }
        }

        // EXAMPLE: Status change in existing node(s)
        // React to data changes in Netconf nodes that are present in the 
        // Netconf topology
        for ( Entry<InstanceIdentifier<?>,
                DataObject> entry : change.getUpdatedData().entrySet()) {
            if (entry.getKey().getTargetType() == NetconfNode.class) {
                NodeId nodeId = getNodeId(entry.getKey());

                // We have a Netconf device
                NetconfNode nnode = (NetconfNode)entry.getValue();
                ConnectionStatus csts = nnode.getConnectionStatus();

                switch (csts) {
                    case Connected: {
                        // Fully connected, all services for remote device are
                        // available from the MountPointService.
                        LOG.info("NETCONF Node: {} is fully connected", nodeId.getValue());
                        List<String> capabilities =
                                nnode.getAvailableCapabilities().getAvailableCapability();
                        LOG.info("Capabilities: {}", capabilities);
                        break;
                    }
                    case Connecting: {
                        // A Netconf device's will be in the 'Connecting' state
                        // initially, and go back to it after disconnect.
                        // Note that a device could be moving back and forth 
                        // between the 'Connected' and 'Connecting' states for
                        // various reasons, such as disconnect from remote
                        // device, network connectivity loss etc.
                        LOG.info("NETCONF Node: {} was disconnected", nodeId.getValue());
                        break;
                    }
                    case UnableToConnect: {
                        // The maximum configured number of reconnect attempts 
                        // have been reached. No more reconnects will be 
                        // attempted by the Netconf Connector.
                        LOG.info("NETCONF Node: {} connection failed", nodeId.getValue());
                        break;
                    }
                }
            }
        }

        // EXAMPLE: Removal of an existing node from the Netconf topology
        for (InstanceIdentifier<?> removedPath : change.getRemovedPaths()) {
            final NodeId nodeId = getNodeId(removedPath);
            if (nodeId != null) {
                // A User removed the Netconf connector for this node
                // Before a node is removed, it changes its state to connecting 
                // (just as if it was disconnected). We may see this multiple
                // times, since our listener is scoped to SUBTREE.
                LOG.info("NETCONF Node: {} was removed", nodeId.getValue());
            }
        }
    }

     /**
      * Determines the Netconf Node Node ID, given the node's instance
      * identifier.
      *
      * @param path Node's instance identifier
      * @return     NodeId for the node
      */
    private NodeId getNodeId(final InstanceIdentifier<?> path) {
        for (InstanceIdentifier.PathArgument pathArgument : path.getPathArguments()) {
            if (pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {

                final Identifier key = ((InstanceIdentifier.IdentifiableItem) pathArgument).getKey();
                if(key instanceof NodeKey) {
                    return ((NodeKey) key).getNodeId();
                }
            }
        }
        return null;
    }

}
