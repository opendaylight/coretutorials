/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dhcp.server;

import java.util.List;
import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpRequestContext;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.options.DhcpOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.apache.directory.server.dhcp.service.manager.LeaseManagerDhcpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018.DefaultOption;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple extension to LeaseManagerDhcpService that provides a set of predefined options
 */
public class CustomisableLeaseManagerDhcpService extends LeaseManagerDhcpService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomisableLeaseManagerDhcpService.class);

    private OptionsField dhcpOfferOptionField;
    private OptionsField dhcpAckOptionField;
    private OptionsField dhcpNakOptionField;

    public CustomisableLeaseManagerDhcpService(LeaseManager manager, List<DefaultOption> options) throws IllegalArgumentException {
        super(manager);
        dhcpOfferOptionField = new OptionsField();
        dhcpAckOptionField = new OptionsField();
        dhcpNakOptionField = new OptionsField();
        implementOptions(options);
    }

    @Override
    protected DhcpMessage handleDISCOVER(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Handle DISCOVER");
        DhcpMessage reply = super.handleDISCOVER(context, request);
        if (reply != null) {
            reply.getOptions().addAll(dhcpOfferOptionField);
        }
        return reply;
    }

    @Override
    protected DhcpMessage handleREQUEST(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Handle REQUEST");
        DhcpMessage reply = super.handleREQUEST(context, request);
        if (reply != null) {
            reply.getOptions().addAll(dhcpAckOptionField);
        }
        return reply;
    }

    @Override
    protected DhcpMessage handleDECLINE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Handle DECLINE");
        DhcpMessage reply = super.handleDECLINE(context, request);
        if (reply != null) {
            reply.getOptions().addAll(dhcpNakOptionField);
        }
        return reply;
    }

    @Override
    protected DhcpMessage handleRELEASE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.trace("Handle RELEASE");
        return super.handleRELEASE(context, request);
    }

    protected void implementOptions(List<DefaultOption> options)  throws IllegalArgumentException {
        DhcpOption dhcpOption;
        for (DefaultOption o : options) {
            byte tag = (byte) o.getId().intValue();

            dhcpOption = new CustomDhcpOption(tag, o.getValue());
            if (o.getScope() == MessageType.DHCPOFFER) {
                dhcpOfferOptionField.add(dhcpOption);
            }
            if (o.getScope() == MessageType.DHCPACK) {
                dhcpAckOptionField.add(dhcpOption);
            }
            if (o.getScope() == MessageType.DHCPNAK) {
                dhcpNakOptionField.add(dhcpOption);
            }
            if (o.getScope() == MessageType.ALL) {
                dhcpOfferOptionField.add(dhcpOption);
                dhcpAckOptionField.add(dhcpOption);
                dhcpNakOptionField.add(dhcpOption);
            }
        }
    }

}
