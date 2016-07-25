/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package clustering.impl;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.local.rpc.rev160722.ClusteringLocalRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.local.rpc.rev160722.LocalRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.local.rpc.rev160722.LocalRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.local.rpc.rev160722.LocalRpcOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class LocalRpcServiceImpl implements ClusteringLocalRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(LocalRpcServiceImpl.class);
    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;

    /**
     * @param hostInfo
     */
    public LocalRpcServiceImpl(HostInformation hostInfo) {
        this.hostInfo = hostInfo;
    }

    @Override
    public Future<RpcResult<LocalRpcOutput>> localRpc(LocalRpcInput input) {
        LOG.info("ClusteringServiceImpl.routedRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        LocalRpcOutput output = new LocalRpcOutputBuilder()
                                    .setOutputParam(outputString)
                                    .setInvocations(rpcInvocations.incrementAndGet())
                                    .setHostName(hostInfo.getHostName())
                                    .setIpAddress(hostInfo.getIpAddresses())
                                    .setJvmUptime(hostInfo.getJvmUptime())
                                    .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
