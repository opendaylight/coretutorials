/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.singleton.hs.spi;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.coretutorials.clustering.singleton.hs.spi.SampleDeviceSetupBuilder.SampleDeviceSetup;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.SingletonhsRpcSampleNodeActionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.topo.discovery.rev160728.SingletonhsRpcTopoDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.RoutedSampleNodeContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SampleDeviceContext implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDeviceContext.class.getName());

    private final SampleDeviceSetup deviceSetup;
    private final ServiceGroupIdentifier serviceGroupIdent;
    private ClusterSingletonServiceRegistration cssReg;
    private TransactionGuardian txGuard;
    private SingletonhsRpcSampleNodeActionService snas;
    private RoutedRpcRegistration<SingletonhsRpcSampleNodeActionService> snasReg;
    private SingletonhsRpcTopoDiscoveryService stdrs;
    private RoutedRpcRegistration<SingletonhsRpcTopoDiscoveryService> stdrsReg;

    public SampleDeviceContext(final SampleDeviceSetup sampleDeviceSetup) {
        this.deviceSetup = Preconditions.checkNotNull(sampleDeviceSetup);
        this.serviceGroupIdent = ServiceGroupIdentifier.create(sampleDeviceSetup.getIdent().toString());
        cssReg = deviceSetup.getClusterSingletonServiceProvider().registerClusterSingletonService(this);
    }

    public SingletonhsRpcSampleNodeActionService getSampleNoteActionsService() {
        return snas;
    }

    public SingletonhsRpcTopoDiscoveryService getSampleTopologyDiscoveryRpcService() {
        return stdrs;
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return serviceGroupIdent;
    }

    @Override
    public void instantiateServiceInstance() {
        txGuard = new TransactionGuardian(deviceSetup);
        snas = new SingletonhsRpcSampleNodeActionServiceImpl(deviceSetup, txGuard);
        snasReg = deviceSetup.getRpcProviderRegistry()
                .addRoutedRpcImplementation(SingletonhsRpcSampleNodeActionService.class, snas);
        snasReg.registerPath(RoutedSampleNodeContext.class, deviceSetup.getIdent());
        stdrs = new SingletonhsRpcTopoDiscoveryServiceImpl(deviceSetup);
        stdrsReg = deviceSetup.getRpcProviderRegistry()
                .addRoutedRpcImplementation(SingletonhsRpcTopoDiscoveryService.class, stdrs);
        stdrsReg.registerPath(RoutedSampleNodeContext.class, deviceSetup.getIdent());

        txGuard.put(LogicalDatastoreType.OPERATIONAL, deviceSetup.getIdent(), deviceSetup.getSampleNode());
        txGuard.submit();


    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        if (snasReg != null) {
            snasReg.close();
            snasReg = null;
        }
        if (stdrsReg != null) {
            stdrsReg.close();
            stdrsReg = null;
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
