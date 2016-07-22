/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.singleton.samples;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class BlueprintSingletonApplication implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(BlueprintSingletonApplication.class.getName());

    private static final ServiceGroupIdentifier IDENT = ServiceGroupIdentifier.create("MyApp");
    private static BlueprintSingletonApplication INSTANCE;

    private final RpcProviderRegistry rpcRegistry;

    private RpcRegistration<MyAppService> rpcServiceRegistration;
    private ClusterSingletonServiceRegistration registration;

    /**
     * Method called when the blueprint container is created.
     *
     * @param rpcProviderRegistry - rpc service registration provider
     * @param clusterSingletonProvider - cluster singleton service registration provider
     */
    public static void init(final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider clusterSingletonProvider) {
        LOG.info("BlueprintSingletonApplication OSGi Session Initiated");
        Preconditions.checkArgument(clusterSingletonProvider != null, "ClusterSingletonProvider can not be null");
        Preconditions.checkArgument(rpcProviderRegistry != null, "RpcProviderRegistry can not be null");
        Preconditions.checkState(INSTANCE == null, "One instance is still active.");
        INSTANCE = new BlueprintSingletonApplication(rpcProviderRegistry, clusterSingletonProvider);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("BlueprintSingletonApplication OSGi Closed");
        if (registration != null) {
            try {
                registration.close();
            } catch (final Exception e) {
                LOG.error("Unexpected exception by close", e);
            }
            registration = null;
        }
        INSTANCE = null;
    }

    private BlueprintSingletonApplication(final RpcProviderRegistry rpcProvider,
            final ClusterSingletonServiceProvider provider) {
        this.rpcRegistry = rpcProvider;
        this.registration = provider.registerClusterSingletonService(this);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENT;
    }

    @Override
    public void instantiateServiceInstance() {
        rpcServiceRegistration = rpcRegistry.addRpcImplementation(MyAppService.class, new MyAppService());
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        rpcServiceRegistration.close();
        return Futures.immediateFuture(null);
    }
}
