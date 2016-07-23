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

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.ClusteringService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.GlobalRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.GlobalRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.GlobalRpcOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.RoutedRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.RoutedRpcOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class ClusteringServiceImpl implements ClusteringService {
    private static final Logger LOG = LoggerFactory.getLogger(ClusteringServiceImpl.class);
    private static final AtomicInteger INVOCATIONS = new AtomicInteger(0);
    private static final HostInfo HOST_INFO = new HostInfo();


    @Override
    public Future<RpcResult<GlobalRpcOutput>> globalRpc(GlobalRpcInput input) {
        LOG.info("GlobalRpcExample.globalRpc input: {}", input);

        final String outputString;
        if (input == null) {
            outputString = "";
        } else {
            outputString = input.getInputParam();
        }

        GlobalRpcOutput output = new GlobalRpcOutputBuilder()
                                    .setOutputParam(outputString)
                                    .setInvocations(INVOCATIONS.incrementAndGet())
                                    .setHostName(HOST_INFO.getHostName())
                                    .setIpAddress(HOST_INFO.getIpAddresses())
                                    .setJvmUptime(HOST_INFO.getJvmUptime())
                                    .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RoutedRpcOutput>> routedRpc(RoutedRpcInput input) {
        // TODO Auto-generated method stub
        return null;
    }

}
