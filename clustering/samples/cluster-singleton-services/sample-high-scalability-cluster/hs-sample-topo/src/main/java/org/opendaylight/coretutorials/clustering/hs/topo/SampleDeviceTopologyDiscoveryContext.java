/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.topo;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.opendaylight.coretutorials.clustering.hs.topo.TopoDeviceSetupBuilder.TopoDeviceSetup;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopoDiscoveryRpcInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopologyDiscoveryRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
class SampleDeviceTopologyDiscoveryContext implements ClusterSingletonService {

    private static final Logger LOG = LoggerFactory.getLogger(SampleDeviceTopologyDiscoveryContext.class);

    private final ServiceGroupIdentifier serviceGroupIdent;
    private final TopoDeviceSetup topoDeviceSetup;

    private ClusterSingletonServiceRegistration cssReg;
    private boolean scheduleTaskCanceled = true;
    private final long delay = 10l;

    public SampleDeviceTopologyDiscoveryContext(final TopoDeviceSetup topoDeviceSetup) {
        this.topoDeviceSetup = Preconditions.checkNotNull(topoDeviceSetup);
        final NodeKey nodeKey = topoDeviceSetup.getIdent().firstKeyOf(Node.class);
        this.serviceGroupIdent = ServiceGroupIdentifier.create(nodeKey.getId().getValue());
        cssReg = topoDeviceSetup.getClusterSingletonServiceProvider().registerClusterSingletonService(this);
    }

    public ListenableFuture<Void> closeSampleDeviceTopoDiscoveryContext() {
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
        scheduleTaskCanceled = false;
        topoDeviceSetup.getScheduler().schedule(makeScheduleTask(), delay, TimeUnit.SECONDS);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        scheduleTaskCanceled = true;
        return Futures.immediateFuture(null);
    }

    private Callable<Void> makeScheduleTask() {
        return new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                if (!scheduleTaskCanceled) {
                    final SampleTopologyDiscoveryRpcService topoDiscRpc = topoDeviceSetup.getSampleServiceProvider()
                            .getTopoDiscoveryRpc(topoDeviceSetup.getIdent());
                    final SampleTopoDiscoveryRpcInputBuilder builder = new SampleTopoDiscoveryRpcInputBuilder();
                    builder.setNode(new NodeRef(topoDeviceSetup.getIdent()));
                    topoDiscRpc.sampleTopoDiscoveryRpc(builder.build());
                    topoDeviceSetup.getScheduler().schedule(makeScheduleTask(), delay, TimeUnit.SECONDS);
                }
                return null;
            }
        };
    }
}
