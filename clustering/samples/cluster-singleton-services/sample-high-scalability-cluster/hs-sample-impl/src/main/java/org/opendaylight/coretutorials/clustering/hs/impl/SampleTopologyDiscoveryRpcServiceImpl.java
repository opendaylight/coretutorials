/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.coretutorials.clustering.hs.impl.SampleDeviceSetupBuilder.SampleDeviceSetup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopoDiscoveryRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopoDiscoveryRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopologyDiscoveryRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
class SampleTopologyDiscoveryRpcServiceImpl implements SampleTopologyDiscoveryRpcService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SampleTopologyDiscoveryRpcServiceImpl.class);

    private final SampleDeviceSetup deviceSetup;
    private RoutedRpcRegistration<SampleTopologyDiscoveryRpcService> nodeTopoRpcReg;

    SampleTopologyDiscoveryRpcServiceImpl(final SampleDeviceSetup deviceSetup) {
        this.deviceSetup = Preconditions.checkNotNull(deviceSetup);
        this.nodeTopoRpcReg = deviceSetup.getRpcProviderRegistry()
                .addRoutedRpcImplementation(SampleTopologyDiscoveryRpcService.class, this);
        nodeTopoRpcReg.registerPath(NodeContext.class, deviceSetup.getIdent());
    }

    @Override
    public Future<RpcResult<SampleTopoDiscoveryRpcOutput>> sampleTopoDiscoveryRpc(
            final SampleTopoDiscoveryRpcInput input) {
        // FIXME : add functionality to LOG base information about machine, node
        throw new UnsupportedOperationException("Missing implemenation - it is BUG");
    }

    @Override
    public void close() throws Exception {
        if (nodeTopoRpcReg != null) {
            nodeTopoRpcReg.unregisterPath(NodeContext.class, deviceSetup.getIdent());
            nodeTopoRpcReg.close();
            nodeTopoRpcReg = null;
        }
    }
}
