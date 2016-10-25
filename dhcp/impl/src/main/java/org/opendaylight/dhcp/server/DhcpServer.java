/*
 * Copyright (c) 2015 Cisco Systems and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcp.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.IOException;
import org.apache.directory.server.dhcp.io.DhcpInterfaceManager;
import org.apache.directory.server.dhcp.netty.DhcpHandler;
import org.apache.directory.server.dhcp.service.DhcpService;
/**
 * Dhcpv4 netty-based server.
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
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventloopGroup);
        bootstrap.channel(NioDatagramChannel.class);
        bootstrap.option(ChannelOption.SO_BROADCAST, true);
        bootstrap.handler(new DhcpHandler(service, this));
        channel = bootstrap.bind(port).sync().channel();
    }

    public void stop() throws IOException, InterruptedException {
        if (channel != null) {
            channel.close().sync();
            channel = null;
        }
        super.stop();
    }

}
