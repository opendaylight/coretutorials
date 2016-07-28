/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.hs.commons.SampleServicesProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.sample.node.common.rev160722.SampleNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.SampleNoteActionsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopologyDiscoveryRpcService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class HighScalabilitySampleImplProvider implements SampleServicesProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HighScalabilitySampleImplProvider.class);


    // References to MD-SAL Infrastructure services, initialized in the constructor
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    private SampleDeviceManager sampleDeviceManagerDelegator;

    /**
     * Constructor.
     *
     * @param dataBroker : reference to the MD-SAL DataBroker
     * @param rpcProviderRegistry : reference to MD-SAL RPC Provider Registry
     * @param clusterSingletonServiceProvider : reference to MD-SAL Cluster Singleton Service
     */
    public HighScalabilitySampleImplProvider(final DataBroker dataBroker,
            final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.clusterSingletonServiceProvider = Preconditions.checkNotNull(clusterSingletonServiceProvider);
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("HighScalabilitySampleImplProvider Session Initiated");
        sampleDeviceManagerDelegator = new SampleDeviceManager(dataBroker, rpcProviderRegistry,
                clusterSingletonServiceProvider);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("HighScalabilitySampleImplProvider Closed");
        try {
            sampleDeviceManagerDelegator.close();
        } catch (final Exception e) {
            LOG.error("Unexpected error by closing SampleDeviceManager", e);
        }
    }

    @Override
    public SampleTopologyDiscoveryRpcService getTopoDiscoveryRpc(final InstanceIdentifier<SampleNode> identifier) {
        return sampleDeviceManagerDelegator.getTopoDiscoveryRpc(identifier);
    }

    @Override
    public SampleNoteActionsService getSampleNodeActionRpcs(final InstanceIdentifier<SampleNode> identifier) {
        return sampleDeviceManagerDelegator.getSampleNodeActionRpcs(identifier);
    }

}
