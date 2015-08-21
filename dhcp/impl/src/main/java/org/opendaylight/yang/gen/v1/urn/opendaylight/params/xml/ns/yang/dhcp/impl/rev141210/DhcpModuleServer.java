package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.io.IOException;
import java.util.List;

import org.apache.directory.server.dhcp.io.DhcpInterfaceManager;
import org.apache.directory.server.dhcp.netty.DhcpHandler;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class DhcpModuleServer extends DhcpInterfaceManager {

    private final DhcpService service;
    private final int port;
    private Channel channel;

    public DhcpModuleServer(LeaseManager manager, int port, List<LeaseManagerOption> options) throws IllegalArgumentException {
        this.service = new CustomisableLeaseManagerDhcpService(manager, options);
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
        EventLoop loop = channel.eventLoop();
        channel.close().sync();
        channel = null;
        loop.shutdownGracefully();

        super.stop();
    }

}
