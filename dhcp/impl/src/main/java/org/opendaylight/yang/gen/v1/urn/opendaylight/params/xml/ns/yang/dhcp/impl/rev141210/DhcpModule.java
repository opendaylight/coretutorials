package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.anarres.dhcp.common.address.InterfaceAddress;
import org.apache.directory.server.dhcp.netty.DhcpServer;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

public class DhcpModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210.AbstractDhcpModule {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpModule.class);

    public DhcpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DhcpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210.DhcpModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

	public static void main(String[] args) {
    	List<String> networkInterfaces = Arrays.asList("lo");
    	List<Option> options = new ArrayList<Option>();

    	Option timeOffsetOffer = new Option();
    	timeOffsetOffer.setId(2);
    	timeOffsetOffer.setValue("2");
    	timeOffsetOffer.setValueType(ValueType.Integer);
    	timeOffsetOffer.setScope(MessageType.DHCPOFFER);
    	options.add(timeOffsetOffer);
    	
    	Option timeServerOffer = new Option();
    	timeServerOffer.setId(4);
    	timeServerOffer.setValue("10.0.2.30");
    	timeServerOffer.setValueType(ValueType.Address);
    	timeServerOffer.setScope(MessageType.DHCPOFFER);
    	options.add(timeServerOffer);
    	
    	Option vendorSpecificOffer = new Option();
    	vendorSpecificOffer.setId(43);
    	vendorSpecificOffer.setValue("10.0.2.40 10.0.2.42 10.0.2.44");
    	vendorSpecificOffer.setValueType(ValueType.AddressList);
    	vendorSpecificOffer.setScope(MessageType.DHCPOFFER);
    	options.add(vendorSpecificOffer);
    	
        startServer(67, networkInterfaces, new ExampleLeaseManagerModule.StaticLeaseManager("10.0.0.100"), options);
    }

    @Override
    public void customValidation() {
    	// TODO add validation , as it is not possible to do it via yang file
//    	JmxAttributeValidationException.checkCondition(!getNetworkInterfaces().isEmpty(), "no network interfaces deffined for DHCP server", null );
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("Starting DHCP server on port {}", getPort());
        return startServer(getPort(), getNetworkInterfaces(), getLeaseManagerDependency(), getOption());
    }

    private static AutoCloseable startServer(final Integer port, final List<String> networkInterfaces, final LeaseManager manager, final List<Option> options) {
    	DhcpModuleLeaseManagerDhcpService service = new DhcpModuleLeaseManagerDhcpService(manager, options);    	
    	final DhcpServer dhcpServer = new DhcpServer(service, port);

        if (networkInterfaces.contains("lo")) {
        	try {
        		dhcpServer.addInterface(InterfaceAddress.forString("127.0.0.1"));
        	} catch (Exception e) {
        		LOG.error("DHCP server on port {} failed to add network interface", port);
        		throw new IllegalStateException(e);
        	}
        }
        
        try {
			dhcpServer.addInterfaces(new Predicate<NetworkInterface>() {
			    public boolean apply(final NetworkInterface input) {
			        return networkInterfaces.contains(input.getName());
			    }
			});
        } catch (Exception e) {
        	LOG.error("DHCP server on port {} failed to add network interfaces", port);
        	throw new IllegalStateException(e);
        }
        
        try {
            // TODO dont use start, provide nio event loop group ourselves
        	LOG.debug("starting DHCP server on port {}", port);
            dhcpServer.start();
            LOG.info("DHCP server started on port {}", port);

            return new AutoCloseable() {
                @Override public void close() throws Exception {
                    LOG.debug("Stopping DHCP server on port {}", port);
                    dhcpServer.stop();
                    LOG.info("DHCP server stopper  on port {}", port);
                }
            };
        } catch (Exception e) {
        	LOG.error("failed to start DHCP server on port {}", port);
        	throw new IllegalStateException(e);
        }
    }
}
