package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev141210;

import java.net.NetworkInterface;
import java.util.List;

import org.anarres.dhcp.common.address.InterfaceAddress;
import org.apache.directory.server.dhcp.service.manager.LeaseManager;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

import io.netty.channel.EventLoopGroup;

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
        JmxAttributeValidationException.checkNotNull(getPortNumber(), "port-number not set", null);
        Integer portNumber = getPortNumber().getValue();
        JmxAttributeValidationException.checkCondition((portNumber > 0 && portNumber < 65535), "port-number out of range", null);

        JmxAttributeValidationException.checkNotNull(getNetworkInterface(), "network-interface not set", null);

        for (LeaseManagerOption option : getLeaseManagerOption()) {
            JmxAttributeValidationException.checkNotNull(option.getScope(), "the scope of lease-manager-option not set", null);
            JmxAttributeValidationException.checkNotNull(option.getValue(), "the value of lease-manager-option not set", null);
        };
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("Starting DHCP server on port {}", getPortNumber());
        return startServer(getPortNumber().getValue(), getNetworkInterface(), getLeaseManagerDependency(), getLeaseManagerOption(), getWorkerThreadGroupDependency());
    }

    private static AutoCloseable startServer(final Integer port, final List<String> networkInterfaces, final LeaseManager manager, final List<LeaseManagerOption> options, EventLoopGroup eventLoopGroup) {
        final DhcpModuleServer dhcpServer;
        try {
            dhcpServer = new DhcpModuleServer(manager, port, options);
        } catch (IllegalArgumentException e) {
            LOG.error("DHCP server on port {} failed to decode option arguments {}", port, e.toString());
            throw new IllegalStateException(e);
        }

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
            dhcpServer.start(eventLoopGroup);
            LOG.info("DHCP server on port {} started", port);

            return new AutoCloseable() {
                @Override public void close() throws Exception {
                    LOG.debug("Stopping DHCP server on port {}", port);
                    dhcpServer.stop();
                    LOG.info("DHCP server on port {} stoped", port);
                }
            };
        } catch (Exception e) {
            LOG.error("DHCP server on port {} failed to start");
            throw new IllegalStateException(e);
        }
    }

}
