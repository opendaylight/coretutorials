/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.coretutorials.clustering.hs.frm;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.hs.commons.SampleServicesProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.sample.node.common.rev160722.SampleNode;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
class FrmDeviceSetupBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(FrmDeviceSetupBuilder.class);

    private ClusterSingletonServiceProvider clusterSingletonServiceProvider;
    private RpcProviderRegistry rpcProviderRegistry;
    private DataBroker dataBroker;
    private InstanceIdentifier<SampleNode> ident;
    private SampleNode sampleNode;
    private SampleServicesProvider sampleServiceProvider;

    public SampleServicesProvider getSampleServiceProvider() {
        return sampleServiceProvider;
    }

    public void setSampleServiceProvider(final SampleServicesProvider sampleServiceProvider) {
        this.sampleServiceProvider = sampleServiceProvider;
    }

    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return clusterSingletonServiceProvider;
    }

    public void setClusterSingletonServiceProvider(
            final ClusterSingletonServiceProvider clusterSingletonServiceProvider) {
        this.clusterSingletonServiceProvider = clusterSingletonServiceProvider;
    }

    public RpcProviderRegistry getRpcProviderRegistry() {
        return rpcProviderRegistry;
    }

    public void setRpcProviderRegistry(final RpcProviderRegistry rpcProviderRegistry) {
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public void setDataBroker(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public InstanceIdentifier<SampleNode> getIdent() {
        return ident;
    }

    public void setIdent(final InstanceIdentifier<SampleNode> ident) {
        this.ident = ident;
    }

    public SampleNode getSampleNode() {
        return sampleNode;
    }

    public void setSampleNode(final SampleNode sampleNode) {
        this.sampleNode = sampleNode;
    }

    public FrmDeviceSetup build() {
        return new FrmDeviceSetup(this);
    }

    class FrmDeviceSetup {

        private final ClusterSingletonServiceProvider clusterSingletonServiceProvider;
        private final RpcProviderRegistry rpcProviderRegistry;
        private final DataBroker dataBroker;
        private final InstanceIdentifier<SampleNode> ident;
        private final SampleNode sampleNode;
        private final SampleServicesProvider sampleServiceProvider;

        private FrmDeviceSetup(final FrmDeviceSetupBuilder builder) {
            this.clusterSingletonServiceProvider = Preconditions
                    .checkNotNull(builder.getClusterSingletonServiceProvider());
            this.rpcProviderRegistry = Preconditions.checkNotNull(builder.getRpcProviderRegistry());
            this.dataBroker = Preconditions.checkNotNull(builder.getDataBroker());
            this.ident = Preconditions.checkNotNull(builder.getIdent());
            this.sampleNode = Preconditions.checkNotNull(builder.getSampleNode());
            this.sampleServiceProvider = Preconditions.checkNotNull(builder.getSampleServiceProvider());
        }

        public SampleServicesProvider getSampleServiceProvider() {
            return sampleServiceProvider;
        }

        public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
            return clusterSingletonServiceProvider;
        }

        public RpcProviderRegistry getRpcProviderRegistry() {
            return rpcProviderRegistry;
        }

        public DataBroker getDataBroker() {
            return dataBroker;
        }

        public InstanceIdentifier<SampleNode> getIdent() {
            return ident;
        }

        public SampleNode getSampleNode() {
            return sampleNode;
        }
    }
}
