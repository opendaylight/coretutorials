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
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.global.rpc.rev160722.ClusteringGlobalRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.local.rpc.rev160722.ClusteringLocalRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.routed.rpc.rev160722.ClusteringRoutedRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.routed.rpc.rev160722.RoutedRpcContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.routed.rpc.rev160722.RpcMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.routed.rpc.rev160722.RpcMemberKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class ClusteringProvider implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusteringProvider.class);

    private static final ServiceGroupIdentifier IDENT = ServiceGroupIdentifier.create("Brm");

    // References to MD-SAL Infrastructure services, initialized in the constructor
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final NotificationPublishService notificationPublishService;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private final HostInformation hostInfo = new HostInformation();

    // Service registrations, added/deleted at various points of the controller lifecycle
    private RpcRegistration<ClusteringGlobalRpcService> globalRpcServiceReg;
    private RoutedRpcRegistration<ClusteringRoutedRpcService> routedRpcServiceReg;
    private RpcRegistration<ClusteringLocalRpcService> localRpcServiceReg;

    private ClusteringLocalRpcService localRpcServiceImpl;

    private ClusterSingletonServiceRegistration cssRegistration;

    /** Constructor.
     * @param dataBroker: reference to the MD-SAL DataBroker
     * @param rpcProviderRegistry: reference to  MD-SAL RPC Provider Registry
     * @param notificationPublishService: reference to MD-SAL Notification service where subscribers
     *                                    register to receive Notifications
     * @param clusterSingletonServiceProvider: reference to MD-SAL Cluster Singleton Service
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

        localRpcServiceImpl = new LocalRpcServiceImpl(hostInfo);

        localRpcServiceReg =
                rpcProviderRegistry.addRpcImplementation(ClusteringLocalRpcService.class, localRpcServiceImpl);
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
        if (localRpcServiceImpl != null) {
            localRpcServiceImpl = null;
        }

        if (localRpcServiceReg != null) {
            localRpcServiceReg.close();
            localRpcServiceReg = null;
        }
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return IDENT;
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("We take Leadership");
        Preconditions.checkState(globalRpcServiceReg == null,
                "Unexpected state: we have active GlobalRpcServiceRegistration");
        Preconditions.checkState(routedRpcServiceReg == null,
                "Unexpected state: we have active RoutedRpcServiceRegistration");
        globalRpcServiceReg = rpcProviderRegistry.addRpcImplementation(ClusteringGlobalRpcService.class, new GlobalRpcServiceImpl(hostInfo));
        routedRpcServiceReg = rpcProviderRegistry.addRoutedRpcImplementation(ClusteringRoutedRpcService.class,
                new RoutedRpcServiceImpl(hostInfo));
        routedRpcServiceReg.registerPath(RoutedRpcContext.class,
                InstanceIdentifier.builder(RpcMember.class, new RpcMemberKey("rpc-key")).build());
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("We lost Leadership");
        if (globalRpcServiceReg != null) {
            globalRpcServiceReg.close();
            globalRpcServiceReg = null;
        }
        if (routedRpcServiceReg != null) {
            routedRpcServiceReg.unregisterPath(RoutedRpcContext.class,
                    InstanceIdentifier.builder(RpcMember.class, new RpcMemberKey("rpc-key")).build());
            routedRpcServiceReg.close();
            routedRpcServiceReg = null;
        }
        return Futures.immediateFuture(null);
    }

}
