/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.coretutorials.clustering.singleton.hs.spi;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.coretutorials.clustering.singleton.hs.spi.SampleDeviceContext.TransactionGuardian;
import org.opendaylight.coretutorials.clustering.singleton.hs.spi.SampleDeviceSetupBuilder.SampleDeviceSetup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.AddSampleNoteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.AddSampleNoteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.AddSampleNoteOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.RemoveSampleNoteInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.RemoveSampleNoteOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.RemoveSampleNoteOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.rpc.sample.node.action.rev160728.SingletonhsRpcSampleNodeActionService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.RoutedSampleNodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.SampleNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.singletonhs.sample.node.rev160722.sample.sub.item.def.SubItems;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class SingletonhsRpcSampleNodeActionServiceImpl implements SingletonhsRpcSampleNodeActionService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonhsRpcSampleNodeActionServiceImpl.class);

    private final SampleDeviceSetup deviceSetup;
    private final TransactionGuardian txGuard;
    private RoutedRpcRegistration<SingletonhsRpcSampleNodeActionService> nodeActionRpcReg;

    SingletonhsRpcSampleNodeActionServiceImpl(final SampleDeviceSetup deviceSetup, final TransactionGuardian txGuard) {
        this.deviceSetup = Preconditions.checkNotNull(deviceSetup);
        this.txGuard = Preconditions.checkNotNull(txGuard);
        this.nodeActionRpcReg = deviceSetup.getRpcProviderRegistry()
                .addRoutedRpcImplementation(SingletonhsRpcSampleNodeActionService.class, this);
        nodeActionRpcReg.registerPath(RoutedSampleNodeContext.class, deviceSetup.getIdent());
    }

    @Override
    public Future<RpcResult<RemoveSampleNoteOutput>> removeSampleNote(final RemoveSampleNoteInput input) {
        Preconditions.checkArgument(input != null);
        txGuard.delete(LogicalDatastoreType.OPERATIONAL, deviceSetup.getIdent());
        txGuard.submit();
        final RemoveSampleNoteOutput output = (new RemoveSampleNoteOutputBuilder()).setReport(true).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<AddSampleNoteOutput>> addSampleNote(final AddSampleNoteInput input) {
        Preconditions.checkArgument(input != null);
        final SampleNodeBuilder snBuilder = new SampleNodeBuilder(deviceSetup.getSampleNode());
        final List<SubItems> list = new ArrayList<>();
        for (final SubItems item : input.getSubItems()) {
            list.add(item);
        }
        snBuilder.setSubItems(list);

        txGuard.put(LogicalDatastoreType.OPERATIONAL, deviceSetup.getIdent(), snBuilder.build());
        txGuard.submit();
        final AddSampleNoteOutput output = (new AddSampleNoteOutputBuilder()).setReport(true).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public void close() throws Exception {
        if (nodeActionRpcReg != null) {
            nodeActionRpcReg.unregisterPath(RoutedSampleNodeContext.class, deviceSetup.getIdent());
            nodeActionRpcReg.close();
            nodeActionRpcReg = null;
        }
    }

}
