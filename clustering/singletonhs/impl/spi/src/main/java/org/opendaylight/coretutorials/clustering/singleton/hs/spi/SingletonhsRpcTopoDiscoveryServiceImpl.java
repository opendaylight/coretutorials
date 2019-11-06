/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.singleton.hs.spi;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.coretutorials.clustering.singleton.hs.common.HostInformation;
import org.opendaylight.coretutorials.clustering.singleton.hs.spi.SampleDeviceSetupBuilder.SampleDeviceSetup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.topo.discovery.rev160728.SampleTopoDiscoveryRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.topo.discovery.rev160728.SampleTopoDiscoveryRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.topo.discovery.rev160728.SampleTopoDiscoveryRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.topo.discovery.rev160728.SingletonhsRpcTopoDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.RoutedSampleNodeContext;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
class SingletonhsRpcTopoDiscoveryServiceImpl implements SingletonhsRpcTopoDiscoveryService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonhsRpcTopoDiscoveryServiceImpl.class);

    private final SampleDeviceSetup deviceSetup;
    private RoutedRpcRegistration<SingletonhsRpcTopoDiscoveryService> nodeTopoRpcReg;
    private final HostInformation hostInfo;

    SingletonhsRpcTopoDiscoveryServiceImpl(final SampleDeviceSetup deviceSetup) {
        this.deviceSetup = Preconditions.checkNotNull(deviceSetup);
        this.nodeTopoRpcReg = deviceSetup.getRpcProviderRegistry()
                .addRoutedRpcImplementation(SingletonhsRpcTopoDiscoveryService.class, this);
        nodeTopoRpcReg.registerPath(RoutedSampleNodeContext.class, deviceSetup.getIdent());
        hostInfo = new HostInformation();
    }

    @Override
    public ListenableFuture<RpcResult<SampleTopoDiscoveryRpcOutput>> sampleTopoDiscoveryRpc(
            final SampleTopoDiscoveryRpcInput input) {
        LOG.warn("TOPO DISCOVERY for {} {} {} in {}", deviceSetup.getIdent(), hostInfo.getHostName(),
                hostInfo.getIpAddresses(), hostInfo.getJvmUptime());

        final SampleTopoDiscoveryRpcOutput output = new SampleTopoDiscoveryRpcOutputBuilder().setReport(true)
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public void close() throws Exception {
        if (nodeTopoRpcReg != null) {
            nodeTopoRpcReg.unregisterPath(RoutedSampleNodeContext.class, deviceSetup.getIdent());
            nodeTopoRpcReg.close();
            nodeTopoRpcReg = null;
        }
    }
}
