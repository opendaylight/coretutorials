/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractRpcService<S extends RpcService> implements ClusterSingletonService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRpcService.class.getName());

    protected final RpcProviderRegistry rpcProviderRegistry;
    private final ServiceGroupIdentifier cssGroupIdentifier;

    private ClusterSingletonServiceRegistration cssRegistration;

    AbstractRpcService(final RpcProviderRegistry rpcProviderRegistry, final ServiceGroupIdentifier cssGroupIdentifier,
            final ClusterSingletonServiceProvider cssProvider) {
        this.cssGroupIdentifier = Preconditions.checkNotNull(cssGroupIdentifier);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        cssRegistration = Preconditions.checkNotNull(cssProvider.registerClusterSingletonService(this));
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return cssGroupIdentifier;
    }

    @Override
    public void close() throws Exception {
        LOG.info("Close active instances of {}", this.getClass());
        if (cssRegistration != null) {
            cssRegistration.close();
            cssRegistration = null;
        }
    }
}
