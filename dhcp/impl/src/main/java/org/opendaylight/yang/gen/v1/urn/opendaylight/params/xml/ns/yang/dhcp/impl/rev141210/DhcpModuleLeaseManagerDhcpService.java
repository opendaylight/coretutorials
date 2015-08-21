package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpRequestContext;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.options.DhcpOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.StringOption;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.apache.directory.server.dhcp.service.manager.LeaseManagerDhcpService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class DhcpModuleLeaseManagerDhcpService extends LeaseManagerDhcpService {
	
	private static final Logger LOG = LoggerFactory.getLogger(DhcpModule.class);

	OptionsField dhcpOfferOptionField;
	OptionsField dhcpAckOptionField;
	OptionsField dhcpNakOptionField;

	public DhcpModuleLeaseManagerDhcpService(LeaseManager manager, List<Option> options) {
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
    	return reply;
    }

	@Override
    protected DhcpMessage handleDECLINE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
		LOG.debug("handleDECLINE");
		DhcpMessage reply = super.handleDECLINE(context, request);
		return reply;
	}

	@Override
    protected DhcpMessage handleRELEASE(DhcpRequestContext context, DhcpMessage request) throws DhcpException {
		LOG.debug("handleRELEASE");
		DhcpMessage reply = super.handleRELEASE(context, request);
		return reply;
	}

	protected void implementOptions(List<Option> options) {
		DhcpOption dhcpOption;
		byte tag;
		for (Option o : options) {
			tag = (byte) ((int) o.getId());
			if (o.getValueType() == ValueType.String) {
				System.out.println("implementing string option");
				dhcpOption = new CustomDhcpOption(tag, o.getValue());
			} else if (o.getValueType() == ValueType.Address) {
				System.out.println("implementing address option");
				dhcpOption = new CustomDhcpOption(tag, InetAddresses.forString(o.getValue()));
			} else if (o.getValueType() == ValueType.Integer) {
				System.out.println("implementing integer option");
				dhcpOption = new CustomDhcpOption(tag, Integer.valueOf(o.getValue()));
			} else if (o.getValueType() == ValueType.AddressList) {
				System.out.println("implementing address-list option");
				String[] values = o.getValue().split(" ");
				InetAddress[] inetAddresses = new InetAddress[values.length];
				for (int i = 0; i < values.length; i++) {
					inetAddresses[i] = InetAddresses.forString(values[i]);
				}
				dhcpOption = new CustomDhcpOption(tag, inetAddresses);
			} else {
				continue;
			}
			if (o.getScope() == MessageType.DHCPOFFER) {
				dhcpOfferOptionField.add(dhcpOption);
			}
			if (o.getScope() == MessageType.DHCPACK) {
				dhcpAckOptionField.add(dhcpOption);
			}
			if (o.getScope() == MessageType.DHCPNAK) {
				dhcpNakOptionField.add(dhcpOption);
			}
		}
	}

}
