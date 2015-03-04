/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ncmount.impl;

import java.util.List;
import java.util.concurrent.Future;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.NcmountService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.rev150105.ShowNodeOutputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NcmountProvider implements NcmountService, BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NcmountProvider.class);
    private RpcRegistration<NcmountService> rpcReg;
    private MountService mountService;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NcmountProvider Session Initiated");
        // Register the RPC and Mount services
        this.rpcReg = session.addRpcImplementation(NcmountService.class, this);
        this.mountService = session.getSALService(MountProviderService.class);
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
        // Note that we can read both config and operational data from the same mount point
        MountInstance xrNode = mountService.getMountPoint(InstanceIdentifier.create(Nodes.class)
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
        
        ShowNodeOutput output = new ShowNodeOutputBuilder().setInterfaces((long) 1).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
