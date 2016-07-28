/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering.hs.commons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.common.clustering.sample.node.common.rev160722.SampleNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.SampleNoteActionsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.topology.discovery.rpc.rev160728.SampleTopologyDiscoveryRpcService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author jmedved
 *
 */
public interface SampleServicesProvider {

    /**
     * Return implementation of {@link SampleTopologyDiscoveryRpcService}
     * @param identifier - {@link InstanceIdentifier} to {@link SampleNode}
     * @return service - relevant service instance or <code>NULL</code>
     */
    @Nullable
    SampleTopologyDiscoveryRpcService getTopoDiscoveryRpc(@Nonnull InstanceIdentifier<SampleNode> identifier);

    /**
     * Return implementation of {@link SampleNoteActionsService}
     * @param identifier - {@link InstanceIdentifier} to {@link SampleNode}
     * @return service - relevant service instance or <code>NULL</code>
     */
    @Nullable
    SampleNoteActionsService getSampleNodeActionRpcs(@Nonnull InstanceIdentifier<SampleNode> identifier);
}
