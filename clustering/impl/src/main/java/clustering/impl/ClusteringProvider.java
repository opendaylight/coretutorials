/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package clustering.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.ClusteringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class ClusteringProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ClusteringProvider.class);
    private static final ClusteringServiceImpl CLUSTERING_SERVICE_IMPL = new ClusteringServiceImpl();

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final NotificationPublishService notificationPublishService;
    private RpcRegistration<ClusteringService> serviceReg;

    /**
     * @param dataBroker
     * @param rpcProviderRegistry
     * @param notificationPublishService
     */
    public ClusteringProvider(final DataBroker dataBroker,
                              final RpcProviderRegistry rpcProviderRegistry,
                              final NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.notificationPublishService = notificationPublishService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ClusteringProvider Session Initiated");
        serviceReg = rpcProviderRegistry.addRpcImplementation(ClusteringService.class, CLUSTERING_SERVICE_IMPL);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ClusteringProvider Closed");
        serviceReg.close();
    }
}
