/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering;

import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.commons.HostInformation;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class SingletonRpcSampleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonRpcSampleProvider.class);

    private static final ServiceGroupIdentifier IDENT = ServiceGroupIdentifier
            .create(SingletonRpcSampleProvider.class.getName());

    // References to MD-SAL Infrastructure services, initialized in the constructor
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final NotificationPublishService notificationPublishService;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private final HostInformation hostInfo = new HostInformation();

    private final List<AutoCloseable> listAutoclosableServices = new ArrayList<>();

    /** Constructor.
     * @param dataBroker: reference to the MD-SAL DataBroker
     * @param rpcProviderRegistry: reference to  MD-SAL RPC Provider Registry
     * @param notificationPublishService: reference to MD-SAL Notification service where subscribers
     *                                    register to receive Notifications
     * @param clusterSingletonServiceProvider: reference to MD-SAL Cluster Singleton Service
     */
    public SingletonRpcSampleProvider(final DataBroker dataBroker,
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

        listAutoclosableServices.add(new LocalRpcServiceImpl(hostInfo, rpcProviderRegistry));
        listAutoclosableServices
                .add(new GlobalRpcServiceImpl(hostInfo, rpcProviderRegistry, clusterSingletonServiceProvider, IDENT));
        listAutoclosableServices
                .add(new RoutedRpcServiceImpl(hostInfo, rpcProviderRegistry, clusterSingletonServiceProvider, IDENT));

    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ClusteringProvider Closed");
        for (final Iterator<AutoCloseable> iterator = Iterators
                .consumingIterator(listAutoclosableServices.iterator()); iterator.hasNext();) {
            try {
                iterator.next().close();
            } catch (final Exception e) {
                LOG.error("Unexpected exception by close RPC", e);
            }
        }
    }
}
