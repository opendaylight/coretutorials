/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcp.server;

import com.google.common.io.BaseEncoding;
import com.google.common.net.InetAddresses;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.anarres.dhcp.common.address.NetworkAddress;
import org.anarres.dhcp.common.address.Subnet;
import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpRequestContext;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.directory.server.dhcp.options.misc.VendorSpecificInformation;
import org.apache.directory.server.dhcp.service.manager.AbstractDynamicLeaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example implementation of a lease manager. It provides IPs to the clients based on an initial IP increasing the IP by one for every new client. <p/>
 * <b> This is NOT intended for real use. </b>
 */
public class ExampleLeaseManager extends AbstractDynamicLeaseManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleLeaseManager.class);

    private InetAddress ip;

    public ExampleLeaseManager(final String ip) {
        try {
            this.ip = Inet4Address.getByName(ip);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid base IP provided", e);
        }
    }

    @Override protected InetAddress getFixedAddressFor(final HardwareAddress hardwareAddress) throws DhcpException {
        LOG.info("StaticLeaseManager.getFixedAddressFor {}", hardwareAddress);
        return null;
    }

    @Override protected Subnet getSubnetFor(final NetworkAddress networkAddress) throws DhcpException {
        LOG.info("StaticLeaseManager.getSubnetFor {}", networkAddress);
        return null;
    }

    @Override protected boolean leaseIp(final InetAddress address, final HardwareAddress hardwareAddress,
        final long ttl) throws Exception {
        LOG.info("StaticLeaseManager.leaseIp {}, {}", address, hardwareAddress);
        return true;
    }

    @Override protected InetAddress leaseMac(final DhcpRequestContext context, final DhcpMessage request,
        final InetAddress clientRequestedAddress, final long ttl) throws Exception {
        this.ip = InetAddresses.increment(ip);
        LOG.info("StaticLeaseManager.leaseMac leasing: {}", ip);
        return ip;
    }

    @Override public DhcpMessage leaseOffer(final DhcpRequestContext context, final DhcpMessage request,
        final InetAddress clientRequestedAddress, final long clientRequestedExpirySecs) throws DhcpException {
        LOG.info("StaticLeaseManager.leaseOffer request: {}, requested address: {}", request.getOptions(), clientRequestedAddress);
        request.getOptions().getStringOption(VendorClassIdentifier.class);

        final DhcpMessage dhcpMessage = super
            .leaseOffer(context, request, clientRequestedAddress, clientRequestedExpirySecs);
        // FIX message type, the base implementation sets the type to ACK. This will be fixed in the next version of dhcp4j
        dhcpMessage.setMessageType(org.apache.directory.server.dhcp.messages.MessageType.DHCPOFFER);

        // Add some option
        final OptionsField options = dhcpMessage.getOptions();
        options.setOption(VendorSpecificInformation.class, BaseEncoding.base16().decode("0B0410000002"));
        dhcpMessage.setOptions(options);
        LOG.info("StaticLeaseManager.leaseOffer response: {}", dhcpMessage);
        return dhcpMessage;
    }

    @Override public void close() throws Exception {
        // No resources to close
    }
}
