/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.coretutorials.clustering;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.coretutorials.clustering.commons.HostInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.routed.rpc.rev160722.RoutedSingletonAppRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.routed.rpc.rev160722.RoutedSingletonAppRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.routed.rpc.rev160722.RoutedSingletonAppRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.routed.rpc.rev160722.SingletonAppRoutedRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class RoutedRpcServiceImpl implements SingletonAppRoutedRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(RoutedRpcServiceImpl.class);
    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;


    /**
     * Constructor.
     * @param hostInfo : reference to an object that holds host data shown by this service
     */
    public RoutedRpcServiceImpl(final HostInformation hostInfo) {
        this.hostInfo = hostInfo;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.routed.rpc.rev160722
     * .SingletonAppRoutedRpcService#routedSingletonAppRpc(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.
     * yang.coretutorials.singleton.app.routed.rpc.rev160722.RoutedSingletonAppRpcOutput)
     */
    @Override
    public Future<RpcResult<RoutedSingletonAppRpcOutput>> routedSingletonAppRpc(
            final RoutedSingletonAppRpcInput input) {
        LOG.info("ClusteringServiceImpl.routedRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        final RoutedSingletonAppRpcOutput output = new RoutedSingletonAppRpcOutputBuilder().setOutputParam(outputString)
                .setInvocations(rpcInvocations.incrementAndGet()).setHostName(hostInfo.getHostName())
                .setIpAddress(hostInfo.getIpAddresses()).setJvmUptime(hostInfo.getJvmUptime()).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
