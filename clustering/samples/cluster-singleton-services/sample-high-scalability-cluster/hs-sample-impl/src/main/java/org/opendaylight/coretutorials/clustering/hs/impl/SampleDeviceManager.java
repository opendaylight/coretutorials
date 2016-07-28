/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.impl;

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
import org.opendaylight.coretutorials.clustering.hs.commons.SampleServicesProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.sample.node.common.rev160722.SampleNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.SampleNoteActionsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopologyDiscoveryRpcService;
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

    private final static InstanceIdentifier<SampleNode> wildCardSampleNodePath = InstanceIdentifier.create(Nodes.class)
            .child(Node.class).augmentation(SampleNode.class);

    protected final ConcurrentMap<NodeKey, SampleDeviceContext> contexts = new ConcurrentHashMap<>();

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;

    private ListenerRegistration<SampleDeviceManager> listenerRegistration;

    SampleDeviceManager(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.clusterSingletonServiceProvider = Preconditions.checkNotNull(clusterSingletonServiceProvider);
        final DataTreeIdentifier<SampleNode> registerPath = new DataTreeIdentifier<>(
                LogicalDatastoreType.CONFIGURATION, wildCardSampleNodePath);
        listenerRegistration = dataBroker.registerDataTreeChangeListener(registerPath, this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<SampleNode>> changes) {
        for (final DataTreeModification<SampleNode> modif : changes) {
            final DataObjectModification<SampleNode> dataModif = modif.getRootNode();
            final ModificationType dataModifType = dataModif.getModificationType();
            final InstanceIdentifier<SampleNode> dataModifIdent = modif.getRootPath().getRootIdentifier();
            final NodeKey nodeKey = dataModifIdent.firstKeyOf(Node.class);
            switch (dataModif.getModificationType()) {
            case WRITE:
                if (contexts.containsKey(nodeKey)) {
                    LOG.info("We have relevant context up");
                    final ListenableFuture<Void> future = stopDeviceContext(nodeKey);
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
                stopDeviceContext(nodeKey);
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
        final NodeKey nodeKey = ident.firstKeyOf(Node.class);
        final SampleDeviceSetupBuilder builder = new SampleDeviceSetupBuilder();
        builder.setClusterSingletonServiceProvider(clusterSingletonServiceProvider);
        builder.setDataBroker(dataBroker);
        builder.setIdent(ident);
        builder.setRpcProviderRegistry(rpcProviderRegistry);
        builder.setSampleNode(sampleNode);
        contexts.put(nodeKey, new SampleDeviceContext(builder.build()));
    }

    private ListenableFuture<Void> stopDeviceContext(final NodeKey nodeKey) {
        final SampleDeviceContext devContext = contexts.get(nodeKey);
        if (devContext != null) {
            final ListenableFuture<Void> future = devContext.closeSampleDeviceContext();
            Futures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(final Void result) {
                    LOG.info("SampleDeviceContext is closed successfull so remove it {}", nodeKey);
                    contexts.remove(nodeKey, devContext);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Unexpected exception by closing SampleDeviceContext {}", nodeKey);
                    contexts.remove(nodeKey, devContext);
                }
            });
            return future;
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }
        for (final NodeKey key : contexts.keySet()) {
            stopDeviceContext(key);
        }
    }

    @Override
    public SampleTopologyDiscoveryRpcService getTopoDiscoveryRpc(final InstanceIdentifier<SampleNode> identifier) {
        Preconditions.checkArgument(identifier != null);
        final NodeKey nodeKey = identifier.firstKeyOf(Node.class);
        final SampleDeviceContext context = contexts.get(nodeKey);
        if (context != null) {
            context.getSampleTopologyDiscoveryRpcService();
        }
        return null;
    }

    @Override
    public SampleNoteActionsService getSampleNodeActionRpcs(final InstanceIdentifier<SampleNode> identifier) {
        Preconditions.checkArgument(identifier != null);
        final NodeKey nodeKey = identifier.firstKeyOf(Node.class);
        final SampleDeviceContext context = contexts.get(nodeKey);
        if (context != null) {
            context.getSampleNoteActionsService();
        }
        return null;
    }
}
