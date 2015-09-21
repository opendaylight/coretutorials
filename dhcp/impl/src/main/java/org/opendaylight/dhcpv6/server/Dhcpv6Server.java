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
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.anarres.dhcp.v6.service.Dhcp6LeaseManager;
import org.anarres.dhcp.v6.service.Dhcp6Service;
import org.apache.directory.server.dhcp.netty.Dhcp6Handler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018.DefaultOption;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class Dhcpv6Server {

    private final Dhcp6Service service;
    private final int port;
    private NioDatagramChannel channel;
    private static final byte[] SERVER_ID = new byte[] {0, 1};

    public Dhcpv6Server(@Nonnull Dhcp6LeaseManager manager, @Nonnegative int port, List<DefaultOption> options)
            throws IllegalArgumentException {

        // final DuidOption.Duid serverId)
        // service needs to be changed
        // this.service = new LeaseManagerDhcp6Service(manager, serverId);

        this.service = null;
        this.port = port;
    }

    @PostConstruct
    public void start(EventLoopGroup eventloopGroup) throws IOException, InterruptedException {
        Bootstrap b = new Bootstrap();
        b.group(eventloopGroup);
        b.channel(NioDatagramChannel.class);
        b.handler(new Dhcp6Handler(service, SERVER_ID));
        channel = (NioDatagramChannel) b.bind(port).sync().channel();
    }

    public void addInterfaces(List<String> interfaceNames) throws SocketException, UnknownHostException {
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

        NetworkInterface networkInterface;
        while (networkInterfaces.hasMoreElements()) {
            networkInterface = networkInterfaces.nextElement();
            if (interfaceNames.isEmpty() && !interfaceNames.contains(networkInterface.getName())) {
                continue;
            }
            channel.joinGroup(new InetSocketAddress(InetAddress.getByName("FF02::1:2"), port), networkInterface);
            channel.joinGroup(new InetSocketAddress(InetAddress.getByName("FF02::1:3"), port), networkInterface);
        }
    }

    @PreDestroy
    public void stop() throws IOException, InterruptedException {
        EventLoop loop = channel.eventLoop();
        channel.close().sync();
        channel = null;
        loop.shutdownGracefully();
    }

}
