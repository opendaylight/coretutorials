/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.singleton.hs.data.manager;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.SampleNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
class SampleDeviceDataManager implements ClusteredDataTreeChangeListener<SampleNode>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDeviceDataManager.class);

    protected final ConcurrentMap<NodeKey, SampleDeviceDataContext> contexts = new ConcurrentHashMap<>();

    private final static InstanceIdentifier<SampleNode> wildCardSampleNodePath = InstanceIdentifier
            .create(NetworkTopology.class).child(Topology.class).child(Node.class).augmentation(SampleNode.class);

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private final SampleServicesProvider sampleServiceProvider;

    private ListenerRegistration<SampleDeviceDataManager> listenerRegistration;

    SampleDeviceDataManager(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry,
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider,
            final SampleServicesProvider sampleServiceProvider) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.clusterSingletonServiceProvider = Preconditions.checkNotNull(clusterSingletonServiceProvider);
        this.sampleServiceProvider = Preconditions.checkNotNull(sampleServiceProvider);
        final DataTreeIdentifier<SampleNode> registerPath = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                wildCardSampleNodePath);
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
                    final ListenableFuture<Void> future = stopDeviceForwardingRulesContext(nodeKey);
                    Futures.addCallback(future, new FutureCallback<Void>() {

                        @Override
                        public void onSuccess(final Void result) {
                            startDeviceForwardingRulesContext(dataModifIdent, dataModif.getDataAfter());
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            startDeviceForwardingRulesContext(dataModifIdent, dataModif.getDataAfter());
                        }
                    });
                } else {
                    startDeviceForwardingRulesContext(dataModifIdent, dataModif.getDataAfter());
                }
                break;
            case DELETE:
                stopDeviceForwardingRulesContext(nodeKey);
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

    @Override
    public void close() throws Exception {
        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }
        for (final NodeKey key : contexts.keySet()) {
            stopDeviceForwardingRulesContext(key);
        }
    }

    protected void startDeviceForwardingRulesContext(final InstanceIdentifier<SampleNode> ident, final SampleNode sampleNode) {
        final NodeKey nodeKey = ident.firstKeyOf(Node.class);
        final DataContextDeviceSetupBuilder builder = new DataContextDeviceSetupBuilder();
        builder.setClusterSingletonServiceProvider(clusterSingletonServiceProvider);
        builder.setDataBroker(dataBroker);
        builder.setIdent(ident);
        builder.setRpcProviderRegistry(rpcProviderRegistry);
        builder.setSampleNode(sampleNode);
        builder.setSampleServiceProvider(sampleServiceProvider);
        contexts.put(nodeKey, new SampleDeviceDataContext(builder.build()));
    }

    private ListenableFuture<Void> stopDeviceForwardingRulesContext(final NodeKey nodeKey) {
        final SampleDeviceDataContext devContext = contexts.get(nodeKey);
        if (devContext != null) {
            final ListenableFuture<Void> future = devContext.closeSampleDeviceForwardingRuleContext();
            Futures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(final Void result) {
                    LOG.info("SampleDeviceForwardingRuleContext is closed successfull so remove it {}", nodeKey);
                    contexts.remove(nodeKey, devContext);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Unexpected exception by closing SampleDeviceForwardingRuleContext {}", nodeKey);
                    contexts.remove(nodeKey, devContext);
                }
            });
            return future;
        }
        return Futures.immediateFuture(null);
    }
}
