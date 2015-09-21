/*
 * Copyright (c) 2015 Cisco Systems and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcpv6.server;

import java.util.List;

import org.anarres.dhcp.v6.Dhcp6Exception;
import org.anarres.dhcp.v6.io.Dhcp6RequestContext;
import org.anarres.dhcp.v6.messages.Dhcp6Message;
import org.anarres.dhcp.v6.messages.Dhcp6MessageType;
import org.anarres.dhcp.v6.options.Dhcp6Option;
import org.anarres.dhcp.v6.options.Dhcp6Options;
import org.anarres.dhcp.v6.options.DuidOption;
import org.anarres.dhcp.v6.service.Dhcp6LeaseManager;
import org.anarres.dhcp.v6.service.LeaseManagerDhcp6Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018.DefaultOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomisableLeaseManagerDhcpv6Service extends LeaseManagerDhcp6Service {

    private static final Logger LOG = LoggerFactory.getLogger(CustomisableLeaseManagerDhcpv6Service.class);

    private Dhcp6Options dhcpv6AdvertiseOptions;
    private Dhcp6Options dhcpv6ReplyOptions;

    public CustomisableLeaseManagerDhcpv6Service(Dhcp6LeaseManager leaseManager, byte[] serverDuid,
            List<DefaultOption> options) {
        super(leaseManager, new DuidOption.Duid(serverDuid));
        dhcpv6AdvertiseOptions = new Dhcp6Options();
        dhcpv6ReplyOptions = new Dhcp6Options();
        implementOptions(options);
    }

    @Override
    protected Dhcp6Message advertise(final Dhcp6RequestContext requestContext, final Dhcp6Message incomingMsg)
            throws Dhcp6Exception {
        LOG.trace("Creating DHCPv6 ADVERTISE message");
        Dhcp6Message message = super.advertise(requestContext, incomingMsg);
        if (message != null) {
            message.getOptions().addAll(dhcpv6AdvertiseOptions);
        }
        return message;
    }

    @Override
    protected Dhcp6Message reply(final Dhcp6RequestContext requestContext, final Dhcp6Message incomingMsg)
            throws Dhcp6Exception {
        LOG.trace("Creating DHCPv6 REPLY message");
        Dhcp6Message message = super.reply(requestContext, incomingMsg);
        if (message != null) {
            message.getOptions().addAll(dhcpv6ReplyOptions);
        }
        return message;
    }

    private void implementOptions(List<DefaultOption> options) {
        Dhcp6Option dhcp6Option;
        MessageType scope;

        for (DefaultOption o : options) {
            try {
                dhcp6Option = new CustomDhcpv6Option(o.getId().shortValue(), o.getValue());
            } catch (IllegalArgumentException e) {
                LOG.warn("Failed to parse DHCPv6 option {}", o.getId());
                continue;
            }
            scope = o.getScope();
            if (scope == MessageType.DHCPADVERTISE || scope == MessageType.ALL) {
                dhcpv6AdvertiseOptions.add(dhcp6Option);
                LOG.info("DHCPv6 ADVERTISE option {} implemented");
            }
            if (scope == MessageType.DHCPREPLY || scope == MessageType.ALL) {
                dhcpv6ReplyOptions.add(dhcp6Option);
                LOG.info("DHCPv6 REPLY option {} implemented");
            }
        }
    }

}
