/*
 * Copyright (c) 2016 ZTE and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.dhcp.server;

import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.util.List;
import org.anarres.dhcp.common.address.InterfaceAddress;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.rev161018.DhcpServerCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DhcpServer Deamon,as blueprint bean.
 */
public class DhcpServerDaemon {
    private static final Logger LOG = LoggerFactory.getLogger(DhcpServerDaemon.class);
    private final DhcpService dhcpService;
    private final DhcpServer dhcpServer;
    private final EventLoopGroup eventLoopGroup;
    private final DataBroker dataBroker;

    public DhcpServerDaemon(final DataBroker dataBroker,
                        final EventLoopGroup eventLoopGroup,final LeaseManager manager,
                        final DhcpServerCfg dhcpServerCfg) {
        this.dataBroker = dataBroker;
        this.eventLoopGroup = eventLoopGroup;
        final int port = dhcpServerCfg.getPort().getValue();
        final List<String> networkInterfaces = dhcpServerCfg.getNetworkInterface();
        dhcpService = new CustomisableLeaseManagerDhcpService(manager, dhcpServerCfg.getDefaultOption());
        try {
            dhcpServer = new DhcpServer(dhcpService, port);
        } catch (IllegalArgumentException e) {
            LOG.error("DHCP server on port {} failed to decode option arguments {}", port, e);
            throw new IllegalStateException(e);
        }

        // Localhost required special setting
        if (networkInterfaces.contains("lo") || networkInterfaces.isEmpty()) {
            try {
                dhcpServer.addInterface(InterfaceAddress.forString("127.0.0.1"));
            } catch (IOException | InterruptedException  e) {
                LOG.error("DHCP server on port {} failed to add network interface {}", port, e);
                throw new IllegalStateException(e);
            }
        }

        // Bind only to pre-configured network interfaces
        try {
            dhcpServer.addInterfaces(networkInterfaces.isEmpty() ? input -> true
                    : input -> networkInterfaces.contains(input.getName()));
        } catch (IOException | InterruptedException  e) {
            LOG.error("DHCP server on port {} failed to add network interfaces: {}", port, e);
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
        } catch (IOException | InterruptedException e) {
            LOG.warn("dhcpServer start fail!",e);
        }
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        try {
            dhcpServer.stop();
        } catch (IOException e) {
            LOG.warn("dhcpServer stop fail!",e);
        } catch (InterruptedException e) {
            LOG.warn("dhcpServer stop fail!",e);
        }
        LOG.info("DhcpProvider Closed");
    }
}
