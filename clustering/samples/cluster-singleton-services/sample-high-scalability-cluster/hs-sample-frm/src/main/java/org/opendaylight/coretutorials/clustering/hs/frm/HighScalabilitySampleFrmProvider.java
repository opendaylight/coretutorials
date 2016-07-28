/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.frm;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class HighScalabilitySampleFrmProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HighScalabilitySampleFrmProvider.class);

    private static final ServiceGroupIdentifier IDENT = ServiceGroupIdentifier
            .create(HighScalabilitySampleFrmProvider.class.getName());

    // References to MD-SAL Infrastructure services, initialized in the constructor
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;


    /**
     * Constructor.
     * 
     * @param dataBroker : reference to the MD-SAL DataBroker
     * @param rpcProviderRegistry : reference to MD-SAL RPC Provider Registry
     * @param clusterSingletonServiceProvider : reference to MD-SAL Cluster Singleton Service
     */
    public HighScalabilitySampleFrmProvider(final DataBroker dataBroker,
                              final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ClusteringProvider Session Initiated");

    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ClusteringProvider Closed");
    }

}
