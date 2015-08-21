package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import com.google.common.io.BaseEncoding;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.anarres.dhcp.common.address.NetworkAddress;
import org.anarres.dhcp.common.address.Subnet;
import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpRequestContext;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.directory.server.dhcp.options.misc.VendorSpecificInformation;
import org.apache.directory.server.dhcp.service.manager.AbstractDynamicLeaseManager;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleLeaseManagerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210.AbstractExampleLeaseManagerModule {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleLeaseManagerModule.class);

    public ExampleLeaseManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ExampleLeaseManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210.ExampleLeaseManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        try {
            InetAddress.getByName(getIp());
        } catch (UnknownHostException e) {
            throw JmxAttributeValidationException.wrap(e, "Provided IP address is invalid", ipJmxAttribute);
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new StaticLeaseManager(getIp());
    }

    static class StaticLeaseManager extends AbstractDynamicLeaseManager implements AutoCloseable {

        private String ip;

        public StaticLeaseManager(final String ip) {
            this.ip = ip;
        }

        @Override protected InetAddress getFixedAddressFor(final HardwareAddress hardwareAddress)
            throws DhcpException {
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
            LOG.info("StaticLeaseManager.leaseMac {}, {}", request.getOptions(), clientRequestedAddress);
            return Inet4Address.getByName(ip);
        }

        @Override public DhcpMessage leaseOffer(final DhcpRequestContext context, final DhcpMessage request,
            final InetAddress clientRequestedAddress, final long clientRequestedExpirySecs) throws DhcpException {
            LOG.info("StaticLeaseManager.leaseOffer {}, {}", request.getOptions(), clientRequestedAddress);
            request.getOptions().getStringOption(VendorClassIdentifier.class);

            final DhcpMessage dhcpMessage = super
                .leaseOffer(context, request, clientRequestedAddress, clientRequestedExpirySecs);
            // FIX message type, the base implementation sets the type to ACK
            dhcpMessage.setMessageType(MessageType.DHCPOFFER);

            // Add some option
            final OptionsField options = dhcpMessage.getOptions();
            options.setOption(VendorSpecificInformation.class, BaseEncoding.base16().decode("0B0410000002"));
            dhcpMessage.setOptions(options);
            return dhcpMessage;
        }

        @Override public void close() throws Exception {
            // No resources to close
        }
    }

}
