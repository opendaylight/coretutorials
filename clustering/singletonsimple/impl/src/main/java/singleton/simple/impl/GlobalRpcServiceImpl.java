/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package singleton.simple.impl;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.singleton.simple.global.rpc.rev160722.GlobalRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.singleton.simple.global.rpc.rev160722.GlobalRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.singleton.simple.global.rpc.rev160722.GlobalRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.singleton.simple.global.rpc.rev160722.GlobalRpcService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of the example global RPC Service.
 * @author jmedved
 *
 */
public class GlobalRpcServiceImpl implements GlobalRpcService {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalRpcServiceImpl.class);
    private final AtomicInteger rpcInvocations = new AtomicInteger(0);
    private final HostInformation hostInfo;

    /** Constructor.
     * @param hostInfo: reference to an object that holds the example host
     *                  data returned in this service's response.
     */
    public GlobalRpcServiceImpl(HostInformation hostInfo) {
        this.hostInfo = hostInfo;
    }

    /* (non-Javadoc)
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.
     * clustering.global.rpc.rev160722.ClusteringGlobalRpcService#globalRpc(
     *    org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.
     *    global.rpc.rev160722.GlobalRpcInput
     * )
     */
    @Override
    public Future<RpcResult<GlobalRpcOutput>> globalRpc(GlobalRpcInput input) {
        LOG.info("GlobalRpcServiceImpl.globalRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        GlobalRpcOutput output = new GlobalRpcOutputBuilder()
                                    .setOutputParam(outputString)
                                    .setInvocations(rpcInvocations.incrementAndGet())
                                    .setHostName(hostInfo.getHostName())
                                    .setIpAddress(hostInfo.getIpAddresses())
                                    .setJvmUptime(hostInfo.getJvmUptime())
                                    .build();
        return RpcResultBuilder.success(output).buildFuture();
    }
}
