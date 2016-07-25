/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package clustering.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.ClusteringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class ClusteringProvider implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusteringProvider.class);

    private static final ServiceGroupIdentifier IDENT = ServiceGroupIdentifier.create("Brm");

//    private static final ClusteringServiceImpl CLUSTERING_SERVICE_IMPL = new ClusteringServiceImpl();

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final NotificationPublishService notificationPublishService;
    private RpcRegistration<ClusteringService> serviceReg;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private ClusteringServiceImpl cluseringServiceImpl;
    private ClusterSingletonServiceRegistration cssRegistration;

    /**
     * @param dataBroker
     * @param rpcProviderRegistry
     * @param notificationPublishService
     */
    public ClusteringProvider(final DataBroker dataBroker,
                              final RpcProviderRegistry rpcProviderRegistry,
            final NotificationPublishService notificationPublishService,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
        this.notificationPublishService = notificationPublishService;
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ClusteringProvider Session Initiated");
        cssRegistration = clusterSingletonServiceProvider.registerClusterSingletonService(this);
        cluseringServiceImpl = new ClusteringServiceImpl();
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ClusteringProvider Closed");
        if (cssRegistration != null) {
            try {
                cssRegistration.close();
            } catch (final Exception e) {
                LOG.warn("Unexpected close exception", e);
            }
            cssRegistration = null;
        }
        if (cluseringServiceImpl != null) {
            cluseringServiceImpl = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENT;
    }

    @Override
    public void instantiateServiceInstance() {
        Preconditions.checkState(cluseringServiceImpl != null, "Unexpected state: we need instance");
        serviceReg = rpcProviderRegistry.addRpcImplementation(ClusteringService.class, cluseringServiceImpl);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        if (serviceReg != null) {
            serviceReg.close();
            serviceReg = null;
        }
        return Futures.immediateFuture(null);
    }
}
