/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package clustering.impl;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.ClusteringService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.GlobalRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.GlobalRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.rev160722.GlobalRpcOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteringServiceImpl implements ClusteringService {
    private static final Logger LOG = LoggerFactory.getLogger(ClusteringService.class);
    private static final AtomicInteger invocations = new AtomicInteger(0);
    private String hostName = "Unknown";
    private final List<String> ipAddresses = new ArrayList<>();

    ClusteringServiceImpl() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                List<InterfaceAddress> ifAddrs = interfaces.nextElement().getInterfaceAddresses();
                for (InterfaceAddress ifAddr : ifAddrs) {
                    ipAddresses.add(ifAddr.getAddress().getHostAddress());
                }
            }
        } catch (SocketException e1) {
            LOG.info("Could not initialize IP Addresses, {}", e1);
        }

        try {
            InetAddress ip = InetAddress.getLocalHost();
            hostName = ip.getHostName();
            LOG.info("Initialized ClusteringServiceImpl, hostName {}, ipAddresses {}", hostName, ipAddresses);

        } catch (UnknownHostException e) {
            LOG.info("Could not initialize host name, {}", e);
        }
    }

    @Override
    public Future<RpcResult<GlobalRpcOutput>> globalRpc(GlobalRpcInput input) {
        LOG.info("GlobalRpcExample.globalRpc input: {}", input);

        GlobalRpcOutput output = new GlobalRpcOutputBuilder()
                                    .setOutputParam(input.getInputParam())
                                    .setInvocations(invocations.getAndIncrement())
                                    .setHostName(hostName)
                                    .setIpAddress(ipAddresses)
                                    .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

}
