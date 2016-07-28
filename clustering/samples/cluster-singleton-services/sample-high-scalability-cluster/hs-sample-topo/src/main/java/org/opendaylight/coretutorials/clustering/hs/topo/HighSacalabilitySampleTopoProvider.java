/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.topo;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.hs.commons.SampleServicesProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class HighSacalabilitySampleTopoProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HighSacalabilitySampleTopoProvider.class);

    // References to MD-SAL Infrastructure services, initialized in the constructor
    private static SampleDeviceTopologyDiscoveryManager sampleDeviceTopologyDiscoveryManager;

    /**
     * Method called when the blueprint container is created.
     *
     * @param dataBroker : reference to the MD-SAL DataBroker
     * @param rpcProviderRegistry : reference to MD-SAL RPC Provider Registry
     * @param clusterSingletonServiceProvider : reference to MD-SAL Cluster Singleton Service
     * @param sampleServiceProvider : All device RPCs holder
     */
    public static void init(final DataBroker dataBroker,
            final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider,
            final SampleServicesProvider sampleServiceProvider) {
        LOG.info("HighSacalabilitySampleTopoProvider Session Initiated");
        Preconditions.checkState(sampleDeviceTopologyDiscoveryManager == null, "we have instance of SFRM");
        sampleDeviceTopologyDiscoveryManager = new SampleDeviceTopologyDiscoveryManager(dataBroker, rpcProviderRegistry,
                clusterSingletonServiceProvider, sampleServiceProvider);
    }

    /**
     * Method called when the blueprint container is destroyed.
     *
     * @throws Exception unexpected close exception
     */
    public void close() throws Exception {
        LOG.info("HighSacalabilitySampleTopoProvider Closed");
        if (sampleDeviceTopologyDiscoveryManager != null) {
            sampleDeviceTopologyDiscoveryManager.close();
            sampleDeviceTopologyDiscoveryManager = null;
        }
    }
}
