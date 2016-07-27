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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.global.rpc.rev160722.GlobalSingletonAppRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.global.rpc.rev160722.GlobalSingletonAppRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.global.rpc.rev160722.GlobalSingletonAppRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.global.rpc.rev160722.SingletonAppGlobalRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class GlobalRpcServiceImpl implements SingletonAppGlobalRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalRpcServiceImpl.class);
    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;

    /**
     * Constructor.
     *
     * @param hostInfo : reference to an object that holds host data shown by this service.
     */
    public GlobalRpcServiceImpl(final HostInformation hostInfo) {
        this.hostInfo = hostInfo;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.global.rpc.rev160722
     * .SingletonAppGlobalRpcService#globalSingletonAppRpc(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.
     * yang.coretutorials.singleton.app.global.rpc.rev160722.GlobalSingletonAppRpcOutput)
     */
    @Override
    public Future<RpcResult<GlobalSingletonAppRpcOutput>> globalSingletonAppRpc(
            final GlobalSingletonAppRpcInput input) {
        LOG.info("GlobalRpcServiceImpl.globalRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        final GlobalSingletonAppRpcOutput output = new GlobalSingletonAppRpcOutputBuilder().setOutputParam(outputString)
                .setInvocations(rpcInvocations.incrementAndGet()).setHostName(hostInfo.getHostName())
                .setIpAddress(hostInfo.getIpAddresses()).setJvmUptime(hostInfo.getJvmUptime()).build();
        return RpcResultBuilder.success(output).buildFuture();
    }
}
