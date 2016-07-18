/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcpv6.server;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.anarres.dhcp.v6.Dhcp6Exception;
import org.anarres.dhcp.v6.io.Dhcp6RequestContext;
import org.anarres.dhcp.v6.messages.Dhcp6Message;
import org.anarres.dhcp.v6.options.Dhcp6Option;
import org.anarres.dhcp.v6.options.Dhcp6Options;
import org.anarres.dhcp.v6.options.DuidOption;
import org.anarres.dhcp.v6.options.IaOption;
import org.anarres.dhcp.v6.options.VendorSpecificInformationOption;
import org.anarres.dhcp.v6.service.AbstractDhcp6LeaseManager;
import org.anarres.dhcp.v6.service.ClientBindingRegistry;
import org.opendaylight.dhcp.server.ExampleLeaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example implementation of a lease manager. It provides IPs to the clients based on an initial IP increasing the IP by one for every new client. <p>
 * <b> This is NOT intended for real use. </b>
 */
public class Examplev6LeaseManager extends AbstractDhcp6LeaseManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Examplev6LeaseManager.class);

    private InetAddress ip;

    public Examplev6LeaseManager(final Lifetimes lifetimes, final String baseIp) {
        super(lifetimes, ClientBindingRegistry.createForIaNa(), ClientBindingRegistry.createForIaTa());
        try {
            this.ip = InetAddress.getByName(baseIp);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid base IP provided", e);
        }
    }

    @Override protected boolean isAppropriate(final Dhcp6RequestContext dhcp6RequestContext, final DuidOption.Duid duid,
        final int i, final InetAddress inetAddress) throws Dhcp6Exception.UnableToAnswerException {
        return true;
    }

    @Override protected InetAddress newIp(final Dhcp6RequestContext dhcp6RequestContext, final DuidOption.Duid duid,
        final IaOption iaOption) throws Dhcp6Exception {
        ip = InetAddresses.increment(ip);
        LOG.info("Examplev6LeaseManager.leaseMac leasing: {}", ip);
        return ip;
    }

    @Nonnull @Override public Dhcp6Message requestInformation(final Dhcp6RequestContext requestContext,
        final Dhcp6Message incomingMsg, final Dhcp6Message reply) throws Dhcp6Exception {
        // Example specific options for the client
        final Dhcp6Options vendorSpecificOptions = new Dhcp6Options();
        vendorSpecificOptions.add(new VendorSpecificSuboption("example"));
        reply.getOptions().add(VendorSpecificInformationOption.create(4444, vendorSpecificOptions));
        LOG.info("Examplev6LeaseManager.leaseOffer response: {}", reply);
        return reply;
    }

    private class VendorSpecificSuboption extends Dhcp6Option {

        public VendorSpecificSuboption(final String example) {
            setData(example.getBytes(StandardCharsets.UTF_8));
        }

        @Override public short getTag() {
            return 3;
        }
    }

    @Override public void close() throws Exception {
        // No resources to close
    }
}
