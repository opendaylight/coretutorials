/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.coretutorials.clustering.hs.impl;

import com.google.common.base.Preconditions;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.coretutorials.clustering.hs.impl.SampleDeviceSetupBuilder.SampleDeviceSetup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.AddSampleNoteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.AddSampleNoteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.RemoveSampleNoteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.RemoveSampleNoteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.hs.sample.note.actions.rev160728.SampleNoteActionsService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class SampleNoteActionServiceImpl implements SampleNoteActionsService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SampleNoteActionServiceImpl.class);

    private final SampleDeviceSetup deviceSetup;
    private RoutedRpcRegistration<SampleNoteActionsService> nodeActionRpcReg;

    SampleNoteActionServiceImpl(final SampleDeviceSetup deviceSetup) {
        this.deviceSetup = Preconditions.checkNotNull(deviceSetup);
        this.nodeActionRpcReg = deviceSetup.getRpcProviderRegistry()
                .addRoutedRpcImplementation(SampleNoteActionsService.class, this);
        nodeActionRpcReg.registerPath(NodeContext.class, deviceSetup.getIdent());
    }

    @Override
    public Future<RpcResult<RemoveSampleNoteOutput>> removeSampleNote(final RemoveSampleNoteInput input) {
        // FIXME : add functionality to remove input from Oper DS
        throw new UnsupportedOperationException("Missing implemenation - it is BUG");
    }

    @Override
    public Future<RpcResult<AddSampleNoteOutput>> addSampleNote(final AddSampleNoteInput input) {
        // FIXME : add functionality to add input to Oper DS
        throw new UnsupportedOperationException("Missing implemenation - it is BUG");
    }

    @Override
    public void close() throws Exception {
        if (nodeActionRpcReg != null) {
            nodeActionRpcReg.unregisterPath(NodeContext.class, deviceSetup.getIdent());
            nodeActionRpcReg.close();
            nodeActionRpcReg = null;
        }
    }

}
