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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import ncmount.impl.listener.LoggingNotificationListener;
import ncmount.impl.listener.PerformanceAwareNotificationListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.mdsal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.mdsal.binding.api.RpcProviderService.BindingAwareProvider;
import org.opendaylight.mdsal.binding.api.RpcProviderService; //*
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150107.InterfaceConfigurations;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev150107._interface.configurations.InterfaceConfiguration;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107.InterfaceProperties;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.DataNodes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.data.nodes.DataNode;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.data.nodes.data.node.Locationviews;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.properties.data.nodes.data.node.locationviews.Locationview;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.table.Interfaces;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.oper.rev150107._interface.table.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.RouterStatic;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.address.family.AddressFamilyBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.address.family.address.family.Vrfipv4Builder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.router._static.Vrfs;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.router._static.vrfs.Vrf;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.router._static.vrfs.VrfBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.router._static.vrfs.VrfKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.prefix.table.VrfPrefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.prefix.table.VrfPrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.prefix.table.vrf.prefixes.VrfPrefixBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.prefix.table.vrf.prefixes.VrfPrefixKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.route.VrfRouteBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.route.vrf.route.VrfNextHopsBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.route.vrf.route.vrf.next.hops.NextHopAddressBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ip._static.cfg.rev130722.vrf.unicast.VrfUnicastBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150119.CiscoIosXrString;
import org.opendaylight.yang.gen.v1.org.opendaylight.coretutorials.ncmount.example.notifications.rev150611.ExampleNotificationsListener;
import org.opendaylight.yang.gen.v1.org.opendaylight.coretutorials.ncmount.example.notifications.rev150611.VrfRouteNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.NotificationsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ListNodesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.NcmountService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.WriteRoutesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.WriteRoutesOutput;
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
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is the implementation of the ncmount application.
 * <p>
 * The skeleton for this class was generated with the MD-SAL application
 * archetype.
 */
public class NcmountProvider implements DataTreeChangeListener<Node>, NcmountService,
        BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NcmountProvider.class);
    public static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class,
                            new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    private RpcRegistration<NcmountService> rpcReg;
    private ListenerRegistration<?> dclReg;
    private MountPointService mountService;
    private DataBroker dataBroker;

    /**
     * A method called when the session to MD-SAL is established. It initializes
     * references to MD-SAL services needed throughout the lifetime of the
     * ncmount application and registers its RPC implementation and Data change
     * Listener with the MD-SAL
     * <p>
     * The skeleton for this method was generated with the MD-SAL application
     * archetype.
     *
     * @param session Reference to the established MD-SAL session
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
            this.dclReg = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID.child(Node.class)), this);
        }
    }

    /**
     * A method called when the session to MD-SAL is closed. It closes
     * registrations in MD-SAL
     * Listener with the MD-SAL
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
     * <p>
     * The signature for this method was generated by yang tools from the
     * ncmount API model.
     *
     * @param input Input parameter from the show-node service yang model -
     *              the node's configured name
     * @return Retrieved configuration and operational data
     */
    @Override
    public ListenableFuture<RpcResult<ShowNodeOutput>> showNode(ShowNodeInput input) {
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
        final ReadTransaction xrNodeReadTx = xrNodeBroker.newReadOnlyTransaction();

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

        List<Ifc> ifcList = new ArrayList<>();
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
                        .withKey(new IfcKey(ifcActive, ifcName))
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
                            view.key().getLocationviewName().getValue(),
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

    LoadingCache<String, KeyedInstanceIdentifier<Node, NodeKey>> mountIds = CacheBuilder.newBuilder()
            .maximumSize(20)
            .build(
                    new CacheLoader<String, KeyedInstanceIdentifier<Node, NodeKey>>() {
                        @Override
                        public KeyedInstanceIdentifier<Node, NodeKey> load(final String key) {
                            return NETCONF_TOPO_IID.child(Node.class, new NodeKey(new NodeId(key)));
                        }
                    });

    /**
     * Write list of routes to specified netconf device.
     * The resulting routes conform to Cisco-IOS-XR-ip-static-cfg.yang yang model.
     *
     * @param input Input list of simple routes
     * @return Success if routes were written to mounted netconf device
     */
    @Override
    public ListenableFuture<RpcResult<WriteRoutesOutput>> writeRoutes(final WriteRoutesInput input) {
        final Optional<MountPoint> mountPoint;
        try {
            // Get mount point for specified device
            mountPoint = mountService.getMountPoint(mountIds.get(input.getMountName()));
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(e);
        }

        // Get DataBroker API and create a write tx
        final DataBroker dataBroker = mountPoint.get().getService(DataBroker.class).get();
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        // Build InstanceIdentifier to point to a specific VRF in the device
        final CiscoIosXrString name = new CiscoIosXrString(input.getVrfId());
        final InstanceIdentifier<Vrf> routesInstanceId = InstanceIdentifier.create(RouterStatic.class)
                .child(Vrfs.class).child(Vrf.class, new VrfKey(name));

        // Prepare the actual routes for VRF. Transform Rpc input routes into VRF routes
        final VrfPrefixes transformedRoutes = new VrfPrefixesBuilder()
                .setVrfPrefix(Lists.transform(input.getRoute(), input1 -> {
                    final IpAddress prefix = new IpAddress(input1.getIpv4Prefix());
                    final IpAddress nextHop = new IpAddress(input1.getIpv4NextHop());
                    final long prefixLength = input1.getIpv4PrefixLength();
                    return new VrfPrefixBuilder()
                            .setVrfRoute(new VrfRouteBuilder()
                                    .setVrfNextHops(new VrfNextHopsBuilder()
                                            .setNextHopAddress(Collections.singletonList(new NextHopAddressBuilder()
                                                    .setNextHopAddress(nextHop)
                                                    .build()))
                                            .build())
                                    .build())
                            .setPrefix(prefix)
                            .setPrefixLength(prefixLength)
                            .withKey(new VrfPrefixKey(prefix, prefixLength))
                            .build();
                })).build();

        // Build the parent data structure for VRF
        final Vrf newRoutes = new VrfBuilder()
                .setVrfName(name)
                .withKey(new VrfKey(name))
                .setAddressFamily(new AddressFamilyBuilder()
                        .setVrfipv4(new Vrfipv4Builder()
                                .setVrfUnicast(new VrfUnicastBuilder()
                                        .setVrfPrefixes(transformedRoutes)
                                        .build())
                                .build())
                        .build())
                .build();

        // invoke edit-config, merge the route list for vrf identified by input.getVrfId()
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, routesInstanceId, newRoutes);

        // commit
        final CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
        return Futures.transform(submit, result -> {
            LOG.info("{} Route(s) written to {}", input.getRoute().size(), input.getMountName());
            return RpcResultBuilder.<WriteRoutesOutput>success().build();
        });
    }

    /**
     * This method is the implementation of the 'list-nodes' RESTCONF service,
     * which is one of the external APIs into the ncmount application. The
     * service provides example functionality. Lists nodes in the Netconf
     * Topology's operational data.
     * <p>
     * Netconf Topology is populated by the Netconf Connector.Operational data
     * contains status data for each netconf node configured in the Netconf
     * Connector.
     * <p>
     * The signature for this method was generated by yang tools from the
     * ncmount API model.
     *
     * @return Lists of nodes found in Netconf Topology's operational space.
     */
    @Override
    public ListenableFuture<RpcResult<ListNodesOutput>> listNodes(ListNodesInput input) {
        // Use this method for one-off operations, when you need to find out
        // something about the nodes currently in the Netconf Topology. An
        // application that needs to handle netconf node discovery/disappearance,
        // a data change listener over Netconf topology should be used.
        List<Node> nodes;
        ListNodesOutputBuilder outBld = new ListNodesOutputBuilder();

        ReadTransaction tx = dataBroker.newReadOnlyTransaction();

        // EXAMPLE: Get all the nodes from the Netconf Topology operational
        // space.
        try {
            nodes = tx.read(LogicalDatastoreType.OPERATIONAL, NETCONF_TOPO_IID)
                    .checkedGet()
                    .get()
                    .getNode();
        } catch (ReadFailedException e) {
            LOG.error("Failed to read node config from datastore", e);
            throw new IllegalStateException(e);
        }

        List<String> results = new ArrayList<>();
        for (Node node : nodes) {
            LOG.info("Node: {}", node);
            NetconfNode nnode = node.augmentation(NetconfNode.class);
            if (nnode != null) {
                // We have a Netconf device
                ConnectionStatus csts = nnode.getConnectionStatus();
                if (csts == ConnectionStatus.Connected) {
                    List<String> capabilities =
                            nnode.getAvailableCapabilities().getAvailableCapability().stream().map(cp ->
                                    cp.getCapability()).collect(Collectors.toList());
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
     * <p>
     * The skeleton for this method was generated with the MD-SAL application
     * archetype.
     *
     * @param changes Data change events
     */
    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        changes.forEach(this::onDataChanged);
    }

    private void onDataChanged(DataTreeModification<Node> change) {
        // We need to handle the following types of events:
        // 1. Discovery of new nodes
        // 2. Status change in existing nodes
        // 3. Removal of existing nodes
        LOG.info("OnDataChange, change: {}", change);

        final DataObjectModification<Node> node = change.getRootNode();
        switch (node.getModificationType()) {
            case DELETE:
                // EXAMPLE: Removal of an existing node from the Netconf topology
                // A User removed the Netconf connector for this node
                // Before a node is removed, it changes its state to connecting
                // (just as if it was disconnected). We may see this multiple
                // times, since our listener is scoped to SUBTREE.
                LOG.info("NETCONF Node: {} was removed", node.getIdentifier());
                break;
            case SUBTREE_MODIFIED:
                // EXAMPLE: New node discovery
                LOG.info("NETCONF Node: {} was updated", node.getIdentifier());
                onNodeUpdated(node.getDataAfter());
                break;
            case WRITE:
                // EXAMPLE: Status change in existing node(s)
                LOG.info("NETCONF Node: {} was created", node.getIdentifier());
                onNodeUpdated(node.getDataAfter());
                break;
            default:
                throw new IllegalStateException("Unhandled node change" + change);
        }
    }

    // React to data changes in Netconf nodes that are present in the
    // Netconf topology
    private void onNodeUpdated(final Node node) {
        // Do we have a Netconf device?
        final NetconfNode nnode = node.augmentation(NetconfNode.class);
        if (nnode == null) {
            LOG.info("NETCONF Node: {} is not managed", node.getNodeId());
            return;
        }

        final ConnectionStatus csts = nnode.getConnectionStatus();
        switch (csts) {
            case Connected: {
                // Fully connected, all services for remote device are
                // available from the MountPointService.
                LOG.info("NETCONF Node: {} is fully connected", node.getNodeId());
                List<String> capabilities =
                        nnode.getAvailableCapabilities().getAvailableCapability().stream().map(cp ->
                        cp.getCapability()).collect(Collectors.toList());
                LOG.info("Capabilities: {}", capabilities);

                // Check if device supports our example notification and if it does, register a notification listener
                if (capabilities.contains(QName.create(VrfRouteNotification.QNAME, "Example-notifications").toString())) {
                    registerNotificationListener(node.getNodeId());
                }

                break;
            }
            case Connecting:
                // A Netconf device's will be in the 'Connecting' state
                // initially, and go back to it after disconnect.
                // Note that a device could be moving back and forth
                // between the 'Connected' and 'Connecting' states for
                // various reasons, such as disconnect from remote
                // device, network connectivity loss etc.
                LOG.info("NETCONF Node: {} was disconnected", node.getNodeId());
                break;
            case UnableToConnect:
                // The maximum configured number of reconnect attempts
                // have been reached. No more reconnects will be
                // attempted by the Netconf Connector.
                LOG.info("NETCONF Node: {} connection failed", node.getNodeId());
                break;
            default:
                LOG.warn("NETCONF Node: {} unhandled status {}", node.getNodeId(), csts);
        }
    }

    private void registerNotificationListener(final NodeId nodeId) {
        final Optional<MountPoint> mountPoint;
        try {
            // Get mount point for specified device
            mountPoint = mountService.getMountPoint(mountIds.get(nodeId.getValue()));
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(e);
        }

        // Instantiate notification listener
        final ExampleNotificationsListener listener;
        // The PerformanceAwareNotificationListener is a special version of listener that
        // measures the time until a specified number of notifications was received
        // The performance is calculated as number of received notifications / elapsed time in seconds
        // This is used for performance testing/measurements and can be ignored
        if (PerformanceAwareNotificationListener.shouldMeasurePerformance(nodeId) &&
                !nodeId.getValue().equals("controller-config")/*exclude loopback netconf connection*/) {
            listener = new PerformanceAwareNotificationListener(nodeId);
        } else {
            // Regular simple notification listener with a simple log message
            listener = new LoggingNotificationListener();
        }

        // Register notification listener
        final Optional<NotificationService> service1 = mountPoint.get().getService(NotificationService.class);
        LOG.info("Registering notification listener on {} for node: {}", VrfRouteNotification.QNAME, nodeId);
        final ListenerRegistration<ExampleNotificationsListener> accessTopologyListenerListenerRegistration =
                service1.get().registerNotificationListener(listener);

        // We have the listener registered, but we need to start the notification stream from device by
        // invoking the create-subscription rpc with for stream named "STREAM_NAME". "STREAM_NAME" is not a valid
        // stream name and serves only for demonstration
        // ---
        // This snippet also demonstrates how to invoke custom RPCs on a mounted netconf node
        // The rpc being invoked here can be found at: https://tools.ietf.org/html/rfc5277#section-2.1.1
        // Note that there is no yang model for it in ncmount, but it is in org.opendaylight.controller:ietf-netconf-notifications
        // which is a transitive dependency in ncmount-impl
        final String streamName = "STREAM_NAME";
        final Optional<RpcConsumerRegistry> service = mountPoint.get().getService(RpcConsumerRegistry.class);
        final NotificationsService rpcService = service.get().getRpcService(NotificationsService.class);
        final CreateSubscriptionInputBuilder createSubscriptionInputBuilder = new CreateSubscriptionInputBuilder();
        createSubscriptionInputBuilder.setStream(new StreamNameType(streamName));
        LOG.info("Triggering notification stream {} for node {}", streamName, nodeId);
        // FIXME: do something with this
        final ListenableFuture<?> subscription = rpcService.createSubscription(createSubscriptionInputBuilder.build());
    }

    /**
     * Determines the Netconf Node Node ID, given the node's instance
     * identifier.
     *
     * @param path Node's instance identifier
     * @return NodeId for the node
     */
    private NodeId getNodeId(final InstanceIdentifier<?> path) {
        for (InstanceIdentifier.PathArgument pathArgument : path.getPathArguments()) {
            if (pathArgument instanceof InstanceIdentifier.IdentifiableItem<?, ?>) {

                final Identifier key = ((InstanceIdentifier.IdentifiableItem) pathArgument).getKey();
                if (key instanceof NodeKey) {
                    return ((NodeKey) key).getNodeId();
                }
            }
        }
        return null;
    }
}
