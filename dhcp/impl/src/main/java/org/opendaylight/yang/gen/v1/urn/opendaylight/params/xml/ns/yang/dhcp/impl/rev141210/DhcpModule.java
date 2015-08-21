package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.anarres.dhcp.common.address.InterfaceAddress;
import org.apache.directory.server.dhcp.netty.DhcpServer;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
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

    @Override
    public void customValidation() {
        // TODO add validation , as it is not possible to do it via yang file
//        JmxAttributeValidationException.checkCondition(!getNetworkInterface().isEmpty(), "no network interfaces deffined for DHCP server", null );
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("Starting DHCP server on port {}", getPortNumber());
        return startServer(getPortNumber().getValue(), getNetworkInterface(), getLeaseManagerDependency(), getLeaseManagerOption());
    }

    private static AutoCloseable startServer(final Integer port, final List<String> networkInterfaces, final LeaseManager manager, final List<LeaseManagerOption> options) {
        CustomisableLeaseManagerDhcpService service = new CustomisableLeaseManagerDhcpService(manager, options);
        final DhcpServer dhcpServer = new DhcpServer(service, port);

        if (networkInterfaces.contains("lo")) {
            try {
                dhcpServer.addInterface(InterfaceAddress.forString("127.0.0.1"));
            } catch (Exception e) {
                LOG.error("DHCP server on port {} failed to add network interface {}", port, e.toString());
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
            LOG.error("DHCP server on port {} failed to add network interfaces: {}", port, e.toString());
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
