/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.coretutorials.clustering.hs.impl.SampleDeviceSetupBuilder.SampleDeviceSetup;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.SampleNoteActionsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopologyDiscoveryRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SampleDeviceContext implements ClusterSingletonService, TransactionChainListener {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDeviceContext.class.getName());

    private final SampleDeviceSetup deviceSetup;
    private final ServiceGroupIdentifier serviceGroupIdent;
    private final ClusterSingletonServiceRegistration cssReg;
    private BindingTransactionChain txChain;
    private SampleNoteActionsService snas;
    private SampleTopologyDiscoveryRpcService stdrs;

    public SampleDeviceContext(final SampleDeviceSetup sampleDeviceSetup) {
        this.deviceSetup = Preconditions.checkNotNull(sampleDeviceSetup);
        final NodeKey nodeKey = deviceSetup.getIdent().firstKeyOf(Node.class);
        this.serviceGroupIdent = ServiceGroupIdentifier.create(nodeKey.getId().getValue());
        cssReg = deviceSetup.getClusterSingletonServiceProvider().registerClusterSingletonService(this);
    }

    public SampleNoteActionsService getSampleNoteActionsService() {
        return snas;
    }

    public SampleTopologyDiscoveryRpcService getSampleTopologyDiscoveryRpcService() {
        return stdrs;
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return serviceGroupIdent;
    }

    @Override
    public void instantiateServiceInstance() {
        snas = new SampleNoteActionServiceImpl(deviceSetup);
        stdrs = new SampleTopologyDiscoveryRpcServiceImpl(deviceSetup);



        txChain = deviceSetup.getDataBroker().createTransactionChain(this);

        final WriteTransaction tx = txChain.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, deviceSetup.getIdent(), deviceSetup.getSampleNode());
        tx.submit();
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        // TODO Auto-generated method stub
        return null;
    }

    public ListenableFuture<Void> closeSampleDeviceContext() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTransactionChainSuccessful(
            final org.opendaylight.controller.md.sal.common.api.data.TransactionChain<?, ?> chain) {
        // TODO Auto-generated method stub

    }

    class TransactionGuardian {

        public TransactionGuardian() {
            // TODO Auto-generated constructor stub
        }
    }
}
