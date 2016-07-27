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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.commons.HostInformation;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.commons.rev160722.RoutedRpcContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.commons.rev160722.RoutedRpcMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.commons.rev160722.RoutedRpcMemberKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.routed.rpc.rev160727.RoutedSingletonRpcRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.routed.rpc.rev160727.RoutedSingletonRpcRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.routed.rpc.rev160727.RoutedSingletonRpcRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.rpc.routed.rpc.rev160727.SingletonRpcRoutedRpcService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class RoutedRpcServiceImpl extends AbstractRpcService<SingletonRpcRoutedRpcService>
        implements SingletonRpcRoutedRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(RoutedRpcServiceImpl.class);

    private final InstanceIdentifier<RoutedRpcMember> ident = InstanceIdentifier
            .builder(RoutedRpcMember.class, new RoutedRpcMemberKey("rpc-key")).build();
    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;
    protected RoutedRpcRegistration<SingletonRpcRoutedRpcService> rpcServiceReg;


    /**
     * @param hostInfo
     * @param rpcProviderRegistry
     * @param cssProvider
     * @param cssGroupIdentifier
     */
    public RoutedRpcServiceImpl(final HostInformation hostInfo, final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider cssProvider, final ServiceGroupIdentifier cssGroupIdentifier) {
        super(rpcProviderRegistry, cssGroupIdentifier, cssProvider);
        this.hostInfo = Preconditions.checkNotNull(hostInfo);
    }

    @Override
    public Future<RpcResult<RoutedSingletonRpcRpcOutput>> routedSingletonRpcRpc(
            final RoutedSingletonRpcRpcInput input) {
        LOG.info("ClusteringServiceImpl.routedRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        final RoutedSingletonRpcRpcOutput output = new RoutedSingletonRpcRpcOutputBuilder().setOutputParam(outputString)
                .setInvocations(rpcInvocations.incrementAndGet()).setHostName(hostInfo.getHostName())
                .setIpAddress(hostInfo.getIpAddresses()).setJvmUptime(hostInfo.getJvmUptime()).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public void instantiateServiceInstance() {
        Preconditions.checkState(rpcServiceReg == null,
                "Unexpected state: we have active RoutedRpcServiceRegistration");
        rpcServiceReg = rpcProviderRegistry.addRoutedRpcImplementation(SingletonRpcRoutedRpcService.class, this);
        /*
         * so route param has to contain /clustering-rpc-common:routed-rpc-member[clustering-rpc-common:name="rpc-key"]
         */
        rpcServiceReg.registerPath(RoutedRpcContext.class, ident);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        if (rpcServiceReg != null) {
            rpcServiceReg.unregisterPath(RoutedRpcContext.class, ident);
            rpcServiceReg.close();
            rpcServiceReg = null;
        }
        return Futures.immediateFuture(null);
    }

}
