/*
 * Copyright (c) 2016 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcpv6.server;

import com.google.common.primitives.Shorts;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.List;

import org.anarres.dhcp.v6.service.Dhcp6LeaseManager;
import org.anarres.dhcp.v6.service.Dhcp6Service;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.rev161018.DhcpServerCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.rev161018.dhcp.server.cfg.DefaultOption;

/**
 * Dhcpv6Server Deamon,as blueprint bean.
 */
public class Dhcpv6ServerDaemon {
    private static final Logger LOG = LoggerFactory.getLogger(Dhcpv6ServerDaemon.class);
    private final Dhcp6Service dhcpService;
    private final Dhcpv6Server dhcpServer;
    private final EventLoopGroup eventLoopGroup;
    private final DataBroker dataBroker;

    public Dhcpv6ServerDaemon(final DataBroker dataBroker,
                              final EventLoopGroup eventLoopGroup, final Dhcp6LeaseManager manager, short serverId, final DhcpServerCfg dhcpServerCfg) {
        this.dataBroker = dataBroker;
        this.eventLoopGroup = eventLoopGroup;
        final int port = dhcpServerCfg.getPort().getValue();
        final List<String> networkInterfaces = dhcpServerCfg.getNetworkInterface();
        dhcpService = new CustomisableLeaseManagerDhcpv6Service(manager, Shorts.toByteArray(serverId),dhcpServerCfg.getDefaultOption());
        try {
            dhcpServer = new Dhcpv6Server(dhcpService,serverId, (short)port,new HashSet<String>(networkInterfaces));
        } catch (SocketException | IllegalArgumentException e) {
            LOG.error("DHCP server on port {} failed to decode option arguments {}", port, e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("DhcpProvider Session Initiated");

        try {
            dhcpServer.start(eventLoopGroup);
            LOG.info("new DHCP server started!!!");
        } catch (InterruptedException e) {
            LOG.warn("dhcpServer start fail!",e);
        }
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        try {
            dhcpServer.stop();
        } catch (IOException | InterruptedException  e) {
            LOG.warn("dhcpServer stop fail!",e);
        }
        LOG.info("DhcpProvider Closed");
    }
}

