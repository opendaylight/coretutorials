/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.singleton.hs.spi;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.singleton.hs.api.SampleServicesProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.SingletonhsRpcSampleNodeActionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.topo.discovery.rev160728.SingletonhsRpcTopoDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.SampleNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public final class SampleDeviceManager
        implements ClusteredDataTreeChangeListener<SampleNode>, SampleServicesProvider, AutoCloseable {
    protected static final Logger LOG = LoggerFactory.getLogger(SampleDeviceManager.class);

    private final static InstanceIdentifier<SampleNode> wildCardSampleNodePath = InstanceIdentifier
        .create(NetworkTopology.class).child(Topology.class).child(Node.class).augmentation(SampleNode.class);

    protected final ConcurrentMap<InstanceIdentifier<SampleNode>, SampleDeviceContext> contexts = new ConcurrentHashMap<>();
    private ListenerRegistration<SampleDeviceManager> dataChangeListenerRegistration;

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    SampleDeviceManager(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.clusterSingletonServiceProvider = Preconditions.checkNotNull(clusterSingletonServiceProvider);
        final DataTreeIdentifier<SampleNode> registerPath = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, wildCardSampleNodePath);
        dataChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(registerPath, this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<SampleNode>> changes) {
        for (final DataTreeModification<SampleNode> modif : changes) {
            final DataObjectModification<SampleNode> dataModif = modif.getRootNode();
            final ModificationType dataModifType = dataModif.getModificationType();
            final InstanceIdentifier<SampleNode> dataModifIdent = modif.getRootPath().getRootIdentifier();
            switch (dataModif.getModificationType()) {
            case WRITE:
                if (contexts.containsKey(dataModifIdent)) {
                    LOG.info("We have relevant context up");
                    final ListenableFuture<Void> future = stopDeviceContext(dataModifIdent);
                    Futures.addCallback(future, new FutureCallback<Void>() {

                        @Override
                        public void onSuccess(final Void result) {
                            startDeviceContext(dataModifIdent, dataModif.getDataAfter());
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            startDeviceContext(dataModifIdent, dataModif.getDataAfter());
                        }
                    });
                } else {
                    startDeviceContext(dataModifIdent, dataModif.getDataAfter());
                }
                break;
            case DELETE:
                stopDeviceContext(dataModifIdent);
                break;
            case SUBTREE_MODIFIED:
                LOG.info("SubTree Modification - no action for Node {}", dataModifIdent);
                break;
            default:
                LOG.error("Unexpected ModificationType {} for Node {}", dataModifType, dataModifIdent);
                break;
            }
        }
    }

    protected void startDeviceContext(final InstanceIdentifier<SampleNode> ident, final SampleNode sampleNode) {
        final SampleDeviceSetupBuilder builder = new SampleDeviceSetupBuilder();
        builder.setClusterSingletonServiceProvider(clusterSingletonServiceProvider);
        builder.setDataBroker(dataBroker);
        builder.setIdent(ident);
        builder.setRpcProviderRegistry(rpcProviderRegistry);
        builder.setSampleNode(sampleNode);
        contexts.put(ident, new SampleDeviceContext(builder.build()));
    }

    private ListenableFuture<Void> stopDeviceContext(final InstanceIdentifier<SampleNode> ident) {
        final SampleDeviceContext devContext = contexts.get(ident);
        if (devContext != null) {
            final ListenableFuture<Void> future = devContext.closeSampleDeviceContext();
            Futures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(final Void result) {
                    LOG.info("SampleDeviceContext is closed successfull so remove it {}", ident);
                    contexts.remove(ident, devContext);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Unexpected exception by closing SampleDeviceContext {}", ident);
                    contexts.remove(ident, devContext);
                }
            });
            return future;
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
        if (dataChangeListenerRegistration != null) {
            dataChangeListenerRegistration.close();
            dataChangeListenerRegistration = null;
        }
        for (final InstanceIdentifier<SampleNode> ident : contexts.keySet()) {
            stopDeviceContext(ident);
        }
    }

    @Override
    public SingletonhsRpcTopoDiscoveryService getTopoDiscoveryRpc(final InstanceIdentifier<SampleNode> identifier) {
        Preconditions.checkArgument(identifier != null);
        final SampleDeviceContext context = contexts.get(identifier);
        if (context != null) {
            context.getSampleTopologyDiscoveryRpcService();
        }
        return null;
    }

    @Override
    public SingletonhsRpcSampleNodeActionService getSampleNodeActionRpcs(
            final InstanceIdentifier<SampleNode> identifier) {
        Preconditions.checkArgument(identifier != null);
        final SampleDeviceContext context = contexts.get(identifier);
        if (context != null) {
            context.getSampleNoteActionsService();
        }
        return null;
    }
}
