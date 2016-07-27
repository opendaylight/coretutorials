/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.clustering;

import com.google.common.base.Preconditions;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.coretutorials.clustering.commons.HostInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.global.rpc.rev160727.GlobalSingletonAppCssRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.global.rpc.rev160727.GlobalSingletonAppCssRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.global.rpc.rev160727.GlobalSingletonAppCssRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.css.global.rpc.rev160727.SingletonAppCssGlobalRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class GlobalRpcServiceImpl implements SingletonAppCssGlobalRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalRpcServiceImpl.class);

    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;
//    private RpcRegistration<SingletonRpcGlobalRpcService> rpcServiceReg;

    /**
     * @param hostInfo
     */
    public GlobalRpcServiceImpl(final HostInformation hostInfo) {
        this.hostInfo = Preconditions.checkNotNull(hostInfo);
    }

    @Override
    public Future<RpcResult<GlobalSingletonAppCssRpcOutput>> globalSingletonAppCssRpc(
            final GlobalSingletonAppCssRpcInput input) {
        LOG.info("GlobalRpcServiceImpl.globalRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        final GlobalSingletonAppCssRpcOutput output = new GlobalSingletonAppCssRpcOutputBuilder()
                .setOutputParam(outputString)
                .setInvocations(rpcInvocations.incrementAndGet()).setHostName(hostInfo.getHostName())
                .setIpAddress(hostInfo.getIpAddresses()).setJvmUptime(hostInfo.getJvmUptime()).build();
        return RpcResultBuilder.success(output).buildFuture();
    }
}
