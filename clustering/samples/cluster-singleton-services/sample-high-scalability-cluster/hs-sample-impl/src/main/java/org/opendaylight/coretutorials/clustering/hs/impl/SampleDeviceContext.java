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
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SampleDeviceContext implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDeviceContext.class.getName());

    private final SampleDeviceSetup deviceSetup;
    private final ServiceGroupIdentifier serviceGroupIdent;
    private ClusterSingletonServiceRegistration cssReg;
    private SampleNoteActionServiceImpl snas;
    private SampleTopologyDiscoveryRpcServiceImpl stdrs;
    private TransactionGuardian txGuard;

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
        txGuard = new TransactionGuardian(deviceSetup);
        snas = new SampleNoteActionServiceImpl(deviceSetup, txGuard);
        stdrs = new SampleTopologyDiscoveryRpcServiceImpl(deviceSetup);

        txGuard.put(LogicalDatastoreType.OPERATIONAL, deviceSetup.getIdent(), deviceSetup.getSampleNode());
        txGuard.submit();
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        if (snas != null) {
            snas.close();
            snas = null;
        }
        if (stdrs != null) {
            stdrs.close();
            stdrs = null;
        }
        return txGuard.closeTransactionGuardian();
    }

    public ListenableFuture<Void> closeSampleDeviceContext() {
        final ListenableFuture<Void> future = closeServiceInstance();
        if (cssReg != null) {
            try {
                cssReg.close();
            } catch (final Exception e) {
                LOG.error("Unexpected exception by closing ClusterSingletonServiceRegistration", e);
            }
            cssReg = null;
        }
        return future;
    }


    class TransactionGuardian implements TransactionChainListener{

        // TODO finalize it with fresh head ... this isn't good sample

        private final SampleDeviceSetup deviceSetup;
        private BindingTransactionChain txChain;
        private final Object TX_LOCK = new Object();
        private WriteTransaction tx;
        private ListenableFuture<Void> lastFuture;
        private final boolean close = false;

        public TransactionGuardian(final SampleDeviceSetup deviceSetup) {
            this.deviceSetup = Preconditions.checkNotNull(deviceSetup);
            txChain = deviceSetup.getDataBroker().createTransactionChain(this);
        }

        public void submit() {
            synchronized (TX_LOCK) {
                lastFuture = tx.submit();
                tx = null;
            }
        }

        public <D extends DataObject> void put(final LogicalDatastoreType dsType, final InstanceIdentifier<D> ident, final D data) {
            final WriteTransaction wtx = getTx();
            wtx.put(dsType, ident, data);
        }

        public void delete(final LogicalDatastoreType dsType, final InstanceIdentifier<?> ident) {
            final WriteTransaction wtx = getTx();
            wtx.delete(dsType, ident);
        }

        public ListenableFuture<Void> closeTransactionGuardian() {
            return lastFuture;
        }

        @Override
        public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
                final Throwable cause) {
            synchronized (TX_LOCK) {
                tx = null;
                txChain = deviceSetup.getDataBroker().createTransactionChain(this);
            }
        }

        @Override
        public void onTransactionChainSuccessful(
                final org.opendaylight.controller.md.sal.common.api.data.TransactionChain<?, ?> chain) {
            LOG.debug("submit is success");
        }

        private WriteTransaction getTx() {
            if (tx == null) {
                synchronized (TX_LOCK) {
                    if (tx == null) {
                        tx = txChain.newWriteOnlyTransaction();
                    }
                }
            }
            return tx;
        }
    }
}
