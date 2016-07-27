/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.commons.HostInformation;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.global.rpc.rev160727.GlobalSingletonRpcRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.global.rpc.rev160727.GlobalSingletonRpcRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.global.rpc.rev160727.GlobalSingletonRpcRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.global.rpc.rev160727.SingletonRpcGlobalRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class GlobalRpcServiceImpl extends AbstractRpcService<SingletonRpcGlobalRpcService>
        implements SingletonRpcGlobalRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalRpcServiceImpl.class);

    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;
    private RpcRegistration<SingletonRpcGlobalRpcService> rpcServiceReg;

    /**
     * @param hostInfo
     * @param rpcProviderRegistry
     * @param cssProvider
     * @param cssGroupIdentifier
     */
    public GlobalRpcServiceImpl(final HostInformation hostInfo, final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider cssProvider, final ServiceGroupIdentifier cssGroupIdentifier) {
        super(rpcProviderRegistry, cssGroupIdentifier, cssProvider);
        this.hostInfo = Preconditions.checkNotNull(hostInfo);
    }

    @Override
    public Future<RpcResult<GlobalSingletonRpcRpcOutput>> globalSingletonRpcRpc(
            final GlobalSingletonRpcRpcInput input) {
        LOG.info("GlobalRpcServiceImpl.globalRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        final GlobalSingletonRpcRpcOutput output = new GlobalSingletonRpcRpcOutputBuilder().setOutputParam(outputString)
                .setInvocations(rpcInvocations.incrementAndGet()).setHostName(hostInfo.getHostName())
                .setIpAddress(hostInfo.getIpAddresses()).setJvmUptime(hostInfo.getJvmUptime()).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("We take Leadership");
        Preconditions.checkState(rpcServiceReg == null,
                "Unexpected state: we have active GlobalRpcServiceRegistration");
        rpcServiceReg = rpcProviderRegistry.addRpcImplementation(SingletonRpcGlobalRpcService.class, this);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("We lost Leadership");
        if (rpcServiceReg != null) {
            rpcServiceReg.close();
            rpcServiceReg = null;
        }
        return Futures.immediateFuture(null);
    }
}
