/*
 * Copyright (c) 2015 Cisco Systems and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcp.server;

import java.util.List;

import org.anarres.dhcp.v6.options.Dhcp6Option;
import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpRequestContext;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.options.DhcpOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.apache.directory.server.dhcp.service.manager.LeaseManagerDhcpService;
import org.opendaylight.dhcpv6.server.CustomDhcpv6Option;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.rev161018.dhcp.server.cfg.DefaultOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.rev161018.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple extension to LeaseManagerDhcpService that provides a set of predefined options.
 */
public class CustomisableLeaseManagerDhcpService extends LeaseManagerDhcpService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomisableLeaseManagerDhcpService.class);

    private final OptionsField dhcpOfferOptionField;
    private final OptionsField dhcpAckOptionField;
    private final OptionsField dhcpNakOptionField;

    public CustomisableLeaseManagerDhcpService(LeaseManager manager, List<DefaultOption> options)
            throws IllegalArgumentException {
        super(manager);
        dhcpOfferOptionField = new OptionsField();
        dhcpAckOptionField = new OptionsField();
        dhcpNakOptionField = new OptionsField();
        implementOptions(options);
    }

    @Override
    protected DhcpMessage handleDISCOVER(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Creating DHCP DISCOVER message");
        DhcpMessage message = super.handleDISCOVER(context, request);
        if (message != null) {
            message.getOptions().addAll(dhcpOfferOptionField);
        }
        return message;
    }

    @Override
    protected DhcpMessage handleREQUEST(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Creating DHCP REQUEST message");
        DhcpMessage message = super.handleREQUEST(context, request);
        if (message != null) {
            message.getOptions().addAll(dhcpAckOptionField);
        }
        return message;
    }

    @Override
    protected DhcpMessage handleDECLINE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Creating DHCP DECLINE message");
        DhcpMessage message = super.handleDECLINE(context, request);
        if (message != null) {
            message.getOptions().addAll(dhcpNakOptionField);
        }
        return message;
    }

    @Override
    protected DhcpMessage handleRELEASE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Creating DHCP RELEASE message");
        return super.handleRELEASE(context, request);
    }

    protected void implementOptions(List<DefaultOption> options) {
        DhcpOption dhcpOption;
        MessageType scope;
        for (DefaultOption o : options) {
            try {
                dhcpOption = new CustomDhcpOption((byte) o.getId().intValue(), o.getValue());
            } catch (IllegalArgumentException e) {
                LOG.warn("Failed to parse DHCP option {}, skipping implementation", o.getId());
                continue;
            }
            scope = o.getScope();
            if (scope == MessageType.DHCPOFFER || scope == MessageType.ALL) {
                dhcpOfferOptionField.add(dhcpOption);
                LOG.debug("DHCP OFFER option {} implemented", o.getId());
            }
            if (scope == MessageType.DHCPACK || scope == MessageType.ALL) {
                dhcpAckOptionField.add(dhcpOption);
                LOG.debug("DHCP ACK option {} implemented", o.getId());
            }
            if (scope == MessageType.DHCPNAK || scope == MessageType.ALL) {
                dhcpNakOptionField.add(dhcpOption);
                LOG.debug("DHCP NAK option {} implemented", o.getId());
            }
        }
    }

}
