/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ncmount.impl;

import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.mount.MountInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.binding.api.mount.MountService;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.ifmgr.cfg.rev130722.InterfaceConfigurations;
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
    private MountService mountService;
    private RpcRegistration<NcmountService> rpcReg;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NcmountProvider Session Initiated");
        // Register the RPC service
        this.rpcReg = session.addRpcImplementation(NcmountService.class, this);
        // Get the mount service provider
        this.mountService = session.getSALService(MountProviderService.class);
    }

    @Override
    public void close() throws Exception {
        LOG.info("NcmountProvider Closed");
        rpcReg.close();
    }

    @Override
    public Future<RpcResult<ShowNodeOutput>> showNode(ShowNodeInput input) {
        LOG.info("showNode called, input {}", input);
        
        // This is equivalent to '.../opendaylight-inventory:nodes/node/<node-name>/
        MountInstance xrNode = mountService.getMountPoint(InstanceIdentifier.create(Nodes.class)
                                           .child(Node.class, new NodeKey(new NodeId(input.getNodeName()))));

        InstanceIdentifier<InterfaceConfigurations> iid = InstanceIdentifier.create(InterfaceConfigurations.class); 
        InterfaceConfigurations ifConfig = (InterfaceConfigurations)xrNode.readConfigurationData(iid);
        LOG.info("Interface config: ifConfig {}", ifConfig);
        
        ShowNodeOutput output = new ShowNodeOutputBuilder().setInterfaces((long) 1).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
