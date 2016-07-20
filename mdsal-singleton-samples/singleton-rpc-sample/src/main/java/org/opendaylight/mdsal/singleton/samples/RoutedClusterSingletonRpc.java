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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.mdsal.singleton.dom.api.DOMClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RoutedClusterSingletonRpc implements RpcService, ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RoutedClusterSingletonRpc.class.getName());

    private final InstanceIdentifier ident = InstanceIdentifier.create(NetworkTopology.class).child(Topology.class,
            new TopologyKey(new TopologyId("my_topo")));

    private final RpcProviderRegistry rpcProviderRegistry;
    private ClusterSingletonServiceRegistration registration;

    private RoutedRpcRegistration<RoutedClusterSingletonRpc> rpcRegistration;

    RoutedClusterSingletonRpc(final RpcProviderRegistry rpcProviderRegistry,
            final DOMClusterSingletonServiceProvider provider) {

        this.rpcProviderRegistry = rpcProviderRegistry;
        registration = provider.registerClusterSingletonService(this);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return RpcSingletonApplicationSample.MY_RPC_IDENT;
    }


    @Override
    public void close() throws Exception {
        LOG.info("Client side close ClusterSingletonService");
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("we get leadership and start RPC");
        rpcRegistration = rpcProviderRegistry.addRoutedRpcImplementation(RoutedClusterSingletonRpc.class, this);
        rpcRegistration.registerPath(TopoContext.class, ident);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("we lost leadership and unregister RPC");
        rpcRegistration.unregisterPath(TopoContext.class, ident);
        rpcRegistration.close();
        return Futures.immediateCheckedFuture(null);
    }

    public static abstract class TopoContext extends BaseIdentity {
        public static final QName QNAME = org.opendaylight.yangtools.yang.common.QName
                .create("urn:opendaylight:inventory", "2013-08-19", "topo-context").intern();

        public TopoContext() {

        }
    }
}
