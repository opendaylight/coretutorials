package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.util.List;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpRequestContext;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.options.DhcpOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.apache.directory.server.dhcp.service.manager.LeaseManagerDhcpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

public class CustomisableLeaseManagerDhcpService extends LeaseManagerDhcpService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomisableLeaseManagerDhcpService.class);

    private OptionsField dhcpOfferOptionField;
    private OptionsField dhcpAckOptionField;
    private OptionsField dhcpNakOptionField;

    public CustomisableLeaseManagerDhcpService(LeaseManager manager, List<LeaseManagerOption> options) {
        super(manager);
        dhcpOfferOptionField = new OptionsField();
        dhcpAckOptionField = new OptionsField();
        dhcpNakOptionField = new OptionsField();
        implementOptions(options);
    }

    @Override
    protected DhcpMessage handleDISCOVER(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        DhcpMessage reply = super.handleDISCOVER(context, request);
        reply.setOptions(dhcpOfferOptionField);
        return reply;
    }

    @Override
    protected DhcpMessage handleREQUEST(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.debug("handleREQUEST");
        DhcpMessage reply = super.handleREQUEST(context, request);
        reply.setOptions(dhcpAckOptionField);
        return reply;
    }

    @Override
    protected DhcpMessage handleDECLINE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.debug("handleDECLINE");
        DhcpMessage reply = super.handleDECLINE(context, request);
        reply.setOptions(dhcpNakOptionField);
        return reply;
    }

    @Override
    protected DhcpMessage handleRELEASE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
        LOG.debug("handleRELEASE");
        DhcpMessage reply = super.handleRELEASE(context, request);
        return reply;
    }

    protected void implementOptions(List<LeaseManagerOption> options) {
        DhcpOption dhcpOption;
        byte tag;
        for (LeaseManagerOption o : options) {
            tag = (byte) ((int) o.getId());
            dhcpOption = new CustomDhcpOption(tag, BaseEncoding.base16().decode(o.getValue().toUpperCase()));
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
