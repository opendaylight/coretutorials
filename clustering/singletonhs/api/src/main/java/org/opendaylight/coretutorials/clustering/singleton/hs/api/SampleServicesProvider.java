/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.singleton.hs.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.SingletonhsRpcSampleNodeActionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.topo.discovery.rev160728.SingletonhsRpcTopoDiscoveryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.SampleNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.SampleNodeDef;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author jmedved
 *
 */
public interface SampleServicesProvider {

    /**
     * Return implementation of {@link SingletonhsRpcTopoDiscoveryService}
     *
     * @param identifier - {@link InstanceIdentifier} to {@link SampleNodeDef}
     * @return service - relevant service instance or <code>NULL</code>
     */
    @Nullable
    SingletonhsRpcTopoDiscoveryService getTopoDiscoveryRpc(@Nonnull InstanceIdentifier<SampleNode> identifier);

    /**
     * Return implementation of {@link SingletonhsRpcSampleNodeActionService}
     *
     * @param identifier - {@link InstanceIdentifier} to {@link SampleNodeDef}
     * @return service - relevant service instance or <code>NULL</code>
     */
    @Nullable
    SingletonhsRpcSampleNodeActionService getSampleNodeActionRpcs(@Nonnull InstanceIdentifier<SampleNode> identifier);
}
