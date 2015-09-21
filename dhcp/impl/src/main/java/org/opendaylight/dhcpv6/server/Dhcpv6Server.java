/*
 * Copyright (c) 2015 Cisco Systems and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcpv6.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.anarres.dhcp.v6.service.Dhcp6Service;
import org.apache.directory.server.dhcp.netty.Dhcp6Handler;

import com.google.common.primitives.Shorts;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public final class Dhcpv6Server {

    public final static InetAddress ALL_DHCP_REALAY_AGENTS_AND_SERVERS_MULTICAST_ADDRESS;
    public final static InetAddress ALL_DHCP_SERVERS_MULTICAST_ADDRESS;

    static {
        try {
            ALL_DHCP_REALAY_AGENTS_AND_SERVERS_MULTICAST_ADDRESS = InetAddress.getByName("FF02::1:2");
            ALL_DHCP_SERVERS_MULTICAST_ADDRESS = InetAddress.getByName("FF02::1:3");
        } catch (UnknownHostException e) {
            // this exception is never thrown
            throw new RuntimeException(e);
        }
    }

    private final Dhcp6Handler handler;
    private final List<NetworkInterface> networkInterfaces;
    private final short port;

    private NioDatagramChannel channel;

    public Dhcpv6Server(Dhcp6Service dhcpService, short serverDuid, short port, Set<String> interfaceNames)
            throws SocketException {
        this.handler = new Dhcp6Handler(dhcpService, Shorts.toByteArray(serverDuid));

        this.networkInterfaces = new ArrayList<NetworkInterface>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        NetworkInterface networkInterface;
        while (networkInterfaces.hasMoreElements()) {
            networkInterface = networkInterfaces.nextElement();
            if (interfaceNames.isEmpty() || interfaceNames.contains(networkInterface.getName())) {
                this.networkInterfaces.add(networkInterface);
            }
        }

        this.port = port;
    }

    public void start(EventLoopGroup eventLoopGroup) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.handler(handler);
        channel = (NioDatagramChannel) bootstrap.bind(port).sync().channel();

        for (NetworkInterface networkInterface : networkInterfaces) {
            channel.joinGroup(ALL_DHCP_REALAY_AGENTS_AND_SERVERS_MULTICAST_ADDRESS, networkInterface, null);
            channel.joinGroup(ALL_DHCP_SERVERS_MULTICAST_ADDRESS, networkInterface, null);
        }
    }

    public void stop() throws IOException, InterruptedException {
        if (channel != null) {
            channel.close().sync();
            channel = null;
        }
    }

}
