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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.coretutorials.clustering.singleton.hs.data.manager.DataContextDeviceSetupBuilder.DataContextDeviceSetup;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.AddSampleNoteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.AddSampleNoteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.AddSampleNoteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.RemoveSampleNoteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.RemoveSampleNoteInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.RemoveSampleNoteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.SingletonhsRpcSampleNodeActionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.SampleNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.SampleNodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.sample.sub.item.def.SubItems;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
class SampleDeviceDataContext
        implements ClusteredDataTreeChangeListener<SubItems>, ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDeviceDataContext.class);

    private final InstanceIdentifier<SubItems> wildCardSubItemsPath;
    private final ServiceGroupIdentifier serviceGroupIdent;
    private final DataContextDeviceSetup dataContextDeviceSetup;

    private ClusterSingletonServiceRegistration cssReg;
    private ListenerRegistration<SampleDeviceDataContext> listenerReg;

    public SampleDeviceDataContext(final DataContextDeviceSetup deviceDataSetup) {
        this.dataContextDeviceSetup = Preconditions.checkNotNull(deviceDataSetup);
        this.wildCardSubItemsPath = deviceDataSetup.getIdent().child(SubItems.class);

        this.serviceGroupIdent = ServiceGroupIdentifier.create(deviceDataSetup.getIdent().toString());
        cssReg = deviceDataSetup.getClusterSingletonServiceProvider().registerClusterSingletonService(this);
    }

    public ListenableFuture<Void> closeSampleDeviceForwardingRuleContext() {
        final ListenableFuture<Void> future = closeServiceInstance();
        if (cssReg != null) {
            try {
                cssReg.close();
            } catch (final Exception e) {
                LOG.error("Unexpected exception by closing ClusterSingletonServiceRegistration instance", e);
            }
            cssReg = null;
        }
        return future;
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return serviceGroupIdent;
    }

    @Override
    public void instantiateServiceInstance() {
        final DataTreeIdentifier<SubItems> dataTreeIdent = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, wildCardSubItemsPath);
        listenerReg = dataContextDeviceSetup.getDataBroker().registerDataTreeChangeListener(dataTreeIdent, this);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        if (listenerReg != null) {
            listenerReg.close();
            listenerReg = null;
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<SubItems>> changes) {
        for (final DataTreeModification<SubItems> modif : changes) {
            final DataObjectModification<SubItems> dataModif = modif.getRootNode();
            final ModificationType dataModifType = dataModif.getModificationType();
            final InstanceIdentifier<SubItems> dataModifIdent = modif.getRootPath().getRootIdentifier();
            final InstanceIdentifier<SampleNode> nodeIdent = dataModifIdent.firstIdentifierOf(SampleNode.class);
            final SingletonhsRpcSampleNodeActionService rpc = dataContextDeviceSetup.getSampleServiceProvider()
                    .getSampleNodeActionRpcs(nodeIdent);
            switch (dataModif.getModificationType()) {
            case WRITE:
                LOG.info("We have new note for write {}", dataModifIdent);
                final Future<RpcResult<AddSampleNoteOutput>> addFuture = rpc.addSampleNote(makeAddInput(Collections.singletonList(dataModif.getDataAfter())));
                break;
            case DELETE:
                LOG.info("We have to remove note {}", dataModifIdent);
                final Future<RpcResult<RemoveSampleNoteOutput>> delFuture = rpc
                        .removeSampleNote(makeDelInput(Collections.singletonList(dataModif.getDataAfter())));
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

    private AddSampleNoteInput makeAddInput(final List<SubItems> subItems) {
        final SampleNodeRef nodeRef = new SampleNodeRef(dataContextDeviceSetup.getIdent());
        final AddSampleNoteInputBuilder builder = new AddSampleNoteInputBuilder();
        builder.setSubItems(subItems);
        builder.setNode(nodeRef);
        return builder.build();
    }

    private RemoveSampleNoteInput makeDelInput(final List<SubItems> subItems) {
        final SampleNodeRef nodeRef = new SampleNodeRef(dataContextDeviceSetup.getIdent());
        final RemoveSampleNoteInputBuilder builder = new RemoveSampleNoteInputBuilder();
        builder.setNode(nodeRef);
        builder.setSubItems(subItems);
        return builder.build();
    }

    private static FutureCallback<RpcResult<? extends DataObject>> createErrorLogFutureCallback(
            final SampleNodeRef noderef) {
        return new FutureCallback<RpcResult<? extends DataObject>>() {

            @Override
            public void onSuccess(final RpcResult<? extends DataObject> result) {
                LOG.info("RPC result for node {} is ok", noderef);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("RPC result for node {} fail", noderef, t);
            }
        };
    }
}
