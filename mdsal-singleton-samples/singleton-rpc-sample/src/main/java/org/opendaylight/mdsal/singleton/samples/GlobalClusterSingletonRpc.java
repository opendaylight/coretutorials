/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.singleton.samples;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.mdsal.singleton.dom.api.DOMClusterSingletonServiceProvider;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class GlobalClusterSingletonRpc implements RpcService, ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalClusterSingletonRpc.class.getName());

    private final RpcProviderRegistry rpcProviderRegistry;

    private ClusterSingletonServiceRegistration registration;
    private RpcRegistration<GlobalClusterSingletonRpc> rpcRegistration;


    GlobalClusterSingletonRpc(final RpcProviderRegistry rpcProviderRegistry,
            final DOMClusterSingletonServiceProvider provider) {
        this.rpcProviderRegistry = rpcProviderRegistry;
        registration = provider.registerClusterSingletonService(this);
        LOG.info("GlobalClusterSingetonRpc instance is creating amd registred in ClusterSingletonServiceProvider");
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return RpcSingletonApplicationSample.MY_RPC_IDENT;
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("GlobalClusterSingletonRpc instance get MASTER from EOS and it is registred to RpcProviderRegistry");
        rpcRegistration = rpcProviderRegistry.addRpcImplementation(GlobalClusterSingletonRpc.class, this);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info(
                "GlobalClusterSingletonRpc instance lost MASTER from EOS and it is unregistred to RpcProviderRegistry");
        if (rpcRegistration != null) {
            rpcRegistration.close();
            rpcRegistration = null;
        }
        return Futures.immediateCheckedFuture(null);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Close instancion from client side");
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

}
