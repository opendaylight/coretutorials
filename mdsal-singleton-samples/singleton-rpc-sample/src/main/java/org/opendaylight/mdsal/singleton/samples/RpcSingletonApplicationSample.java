/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.mdsal.singleton.samples;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RpcSingletonApplicationSample implements AutoCloseable {

    static ServiceGroupIdentifier MY_RPC_IDENT = ServiceGroupIdentifier.create("serviceGroupIdent");

    private static final Logger LOG = LoggerFactory.getLogger(RpcSingletonApplicationSample.class.getName());

    private final GlobalClusterSingletonRpc globalRpc;
    private final RoutedClusterSingletonRpc routedRpc;

    /**
     * @param rpcProviderRegistry
     * @param provider
     */
    public RpcSingletonApplicationSample(final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider provider) {
        LOG.info("RpcSingAppSample started from OSGi call.");
        Preconditions.checkArgument(rpcProviderRegistry != null, "RpcProvider registry can not be null!");
        Preconditions.checkArgument(provider != null, "ClusterSingletonProvider can not be null !");
        globalRpc = new GlobalClusterSingletonRpc(rpcProviderRegistry, provider);
        routedRpc = new RoutedClusterSingletonRpc(rpcProviderRegistry, provider);
    }

    @Override
    public void close() throws Exception {
        globalRpc.close();
        routedRpc.close();
        LOG.info(
                "RpcSingAppSample finishing from OSGi call or some unexpected ending calls e.g. Karaf internal problem or uninstall ..");
    }

}
