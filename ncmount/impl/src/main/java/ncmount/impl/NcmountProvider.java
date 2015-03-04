/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ncmount.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.mount.MountInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountService;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.fields.AvailableCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.NcmountService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NcmountProvider implements DataChangeListener, NcmountService, BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NcmountProvider.class);
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID = InstanceIdentifier
                                                                        .create(NetworkTopology.class)
                                                                        .child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));
    private RpcRegistration<NcmountService> rpcReg;
    private MountService mountService;
    private DataBroker dataBroker;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NcmountProvider Session Initiated");
        // Register the RPC and Mount services
        this.rpcReg = session.addRpcImplementation(NcmountService.class, this);
        this.mountService = session.getSALService(MountProviderService.class);
        this.dataBroker = session.getSALService(DataBroker.class);
        
        // Register ourselves as data change listener for changes on any Netconf
        // node. Netconf nodes are accessed via "Netconf Topology" - a special 
        // topology created by the system infra, which contains all netconf 
        // nodes. NETCONF_TOPO_IID is equivalent to the following URL:
        // .../restconf/operational/network-topology:network-topology/topology/topology-netconf
        if (dataBroker != null) {
            dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, 
                    NETCONF_TOPO_IID.child(Node.class), this, DataChangeScope.SUBTREE);
        }

    }

    @Override
    public void close() throws Exception {
        LOG.info("NcmountProvider Closed");
        if (rpcReg != null) {
        	rpcReg.close();
        }
    }

    @Override
    public Future<RpcResult<ShowNodeOutput>> showNode(ShowNodeInput input) {
        LOG.info("showNode called, input {}", input);
        
        // Get the mount point for the specified node
        // Equivalent to '.../restconf/<config | operational>/opendaylight-inventory:nodes/node/<node-name>/yang-ext:mount/'
        // Note that we can read both config and operational data from the same
        // mount point
        MountInstance xrNode = mountService.getMountPoint(NETCONF_TOPO_IID
                                                          .child(Node.class, new NodeKey(new NodeId(input.getNodeName()))));

        // Browse through the node's interface configuration data (as example)
        // Equivalent to '.../yang-ext:mount/Cisco-IOS-XR-ifmgr-cfg:interface-configurations'
        InstanceIdentifier<InterfaceConfigurations> iid = InstanceIdentifier.create(InterfaceConfigurations.class); 
        InterfaceConfigurations ifConfig = (InterfaceConfigurations)xrNode.readConfigurationData(iid);
        List<InterfaceConfiguration> ifConfigs = ifConfig.getInterfaceConfiguration();
        for (InterfaceConfiguration config : ifConfigs) {
            LOG.info("Config for '{}': config {}", config.getInterfaceName().getValue(), config);
        }

        // Browse through node's interface operational data
        // Equivalent to '.../yang-ext:mount/Cisco-IOS-XR-ifmgr-oper:interface-properties/data-nodes'
        // Note that we are not using the top level container here
        InstanceIdentifier<DataNodes> idn = InstanceIdentifier.create(InterfaceProperties.class)
                                                              .child(DataNodes.class);
        DataNodes ldn = (DataNodes)xrNode.readOperationalData(idn);

        List<DataNode> dataNodes = ldn.getDataNode();
        for (DataNode node : dataNodes) {
            LOG.info("DataNode '{}'", node.getDataNodeName().getValue());
            
            Locationviews lw = node.getLocationviews();
            List<Locationview> locationViews = lw.getLocationview();
            for (Locationview view : locationViews) {
                LOG.info("LocationView '{}': {}", view.getKey().getLocationviewName().getValue(), view);
            }

            Interfaces ifc = node.getSystemView().getInterfaces();
            List<Interface> ifList = ifc.getInterface();
            for (Interface intf : ifList) {
                LOG.info("Interface '{}': {}", intf.getInterface().getValue(), intf);
            }

        }
        
        ShowNodeOutput output = new ShowNodeOutputBuilder()
                                    .setMsg("See the ODL log for results (in karaf console, type 'display')")
                                    .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ListNodesOutput>> listNodes() {
        // USe this for one-off operations, when you need to find out something about the current nodes
        // For an app working with netconf nodes, use data change listener over Netconf topology
        List<Node> nodes;
        ListNodesOutputBuilder outBld = new ListNodesOutputBuilder();

        ReadTransaction tx = dataBroker.newReadOnlyTransaction();
        
        // Get all the nodes from configuration space
        try {
            nodes = tx.read(LogicalDatastoreType.CONFIGURATION, NETCONF_TOPO_IID).checkedGet().get().getNode();
        } catch (ReadFailedException e) {
            LOG.error("Failed to read node config from datastore", e);
            throw new IllegalStateException(e);
        }

        List<String> results = new ArrayList<String>();
        for( Node node : nodes) {
            LOG.info("Node: {}", node);
            results.add(node.getNodeId().getValue());
        }
        outBld.setNcConfigNodes(results);

        // Get all the nodes from operational space
        try {
            nodes = tx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID).checkedGet().get().getNode();
        } catch (ReadFailedException e) {
            LOG.error("Failed to read node config from datastore", e);
            throw new IllegalStateException(e);
        }

        results = new ArrayList<String>();
        for( Node node : nodes) {
            LOG.info("Node: {}", node);
            NetconfNode nnode = node.getAugmentation(NetconfNode.class);
            if (nnode != null) {
                // We have a Netconf device
                ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == ConnectionStatus.Connected) {
                    List<String> capabilities = nnode.getAvailableCapabilities().getAvailableCapability();
                    LOG.info("Capabilities: {}", capabilities);
                }
            }
            results.add(node.getNodeId().getValue());
        }
        outBld.setNcOperNodes(results);

        return RpcResultBuilder.success(outBld.build()).buildFuture();
	}

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        LOG.info("OnDataChange, change: {}", change);

        for ( Entry<InstanceIdentifier<?>, DataObject> entry : change.getCreatedData().entrySet()) {
            if( entry.getKey().getTargetType() == NetconfNode.class) {
                // We have a Netconf device
                NetconfNode nnode = (NetconfNode)entry.getValue();
                ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == ConnectionStatus.Connected) {
                    List<String> capabilities = nnode.getAvailableCapabilities().getAvailableCapability();
                    LOG.info("Capabilities: {}", capabilities);
                }
            }
        }
        for ( Entry<InstanceIdentifier<?>, DataObject> entry : change.getUpdatedData().entrySet()) {
            if( entry.getKey().getTargetType() == NetconfNode.class) {
                // We have a Netconf device
                NetconfNode nnode = (NetconfNode)entry.getValue();
                ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == ConnectionStatus.Connected) {
                    List<String> capabilities = nnode.getAvailableCapabilities().getAvailableCapability();
                    LOG.info("Capabilities: {}", capabilities);
                }
            }
        }

    }

}
