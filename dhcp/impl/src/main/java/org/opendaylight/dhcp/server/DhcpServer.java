/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcp.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.IOException;
import java.util.List;
import org.apache.directory.server.dhcp.io.DhcpInterfaceManager;
import org.apache.directory.server.dhcp.netty.DhcpHandler;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018.DefaultOption;

/**
 * Dhcpv4 netty-based server
 */
public class DhcpServer extends DhcpInterfaceManager {

    private final DhcpService service;
    private final int port;
    private Channel channel;

    public DhcpServer(DhcpService service, int port) {
        this.service = service;
        this.port = port;
    }

    public void start(EventLoopGroup eventloopGroup) throws IOException, InterruptedException {
        super.start();
        Bootstrap b = new Bootstrap();
        b.group(eventloopGroup);
        b.channel(NioDatagramChannel.class);
        b.option(ChannelOption.SO_BROADCAST, true);
        b.handler(new DhcpHandler(service, this));
        channel = b.bind(port).sync().channel();
    }

    public void stop() throws IOException, InterruptedException {
        channel.close().sync();
        channel = null;
        super.stop();
    }

}
