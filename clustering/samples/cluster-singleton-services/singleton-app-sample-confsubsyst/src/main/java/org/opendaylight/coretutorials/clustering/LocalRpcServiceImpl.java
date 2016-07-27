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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.local.rpc.rev160727.LocalSingletonAppCssRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.local.rpc.rev160727.LocalSingletonAppCssRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.local.rpc.rev160727.LocalSingletonAppCssRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.local.rpc.rev160727.SingletonAppCssLocalRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class LocalRpcServiceImpl implements SingletonAppCssLocalRpcService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalRpcServiceImpl.class);

    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;

    /**
     * @param hostInfo
     * @param rpcProviderRegistry
     */
    public LocalRpcServiceImpl(final HostInformation hostInfo) {
        this.hostInfo = hostInfo;
    }

    @Override
    public Future<RpcResult<LocalSingletonAppCssRpcOutput>> localSingletonAppCssRpc(
            final LocalSingletonAppCssRpcInput input) {
        LOG.info("ClusteringServiceImpl.routedRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        final LocalSingletonAppCssRpcOutput output = new LocalSingletonAppCssRpcOutputBuilder()
                .setOutputParam(outputString).setInvocations(rpcInvocations.incrementAndGet())
                .setHostName(hostInfo.getHostName()).setIpAddress(hostInfo.getIpAddresses())
                .setJvmUptime(hostInfo.getJvmUptime()).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
