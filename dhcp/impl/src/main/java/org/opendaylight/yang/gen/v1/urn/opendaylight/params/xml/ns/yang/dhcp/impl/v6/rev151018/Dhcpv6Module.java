package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018;

import java.util.List;

import org.anarres.dhcp.v6.service.Dhcp6LeaseManager;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.dhcpv6.server.Dhcpv6Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;

public class Dhcpv6Module extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018.AbstractDhcpv6Module {

    private static final Logger LOG = LoggerFactory.getLogger(Dhcpv6Module.class);

    public Dhcpv6Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Dhcpv6Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, Dhcpv6Module oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getLeaseManager(), "lease-manager not set",
                leaseManagerJmxAttribute);

        JmxAttributeValidationException.checkNotNull(getPort(), "port-number not set", portJmxAttribute);
        Integer portNumber = getPort().getValue();
        JmxAttributeValidationException.checkCondition((portNumber > 0 && portNumber < 65535),
                "port-number out of range", portJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("Starting DHCPv6 server on port {}", getPort());
        return startServer(getPort().getValue(), getNetworkInterface(), getLeaseManagerDependency(), getDefaultOption(),
                getWorkerThreadGroupDependency());
    }

    private static AutoCloseable startServer(final Integer port, final List<String> networkInterfaces,
            final Dhcp6LeaseManager manager, final List<DefaultOption> options, EventLoopGroup eventLoopGroup) {
        final Dhcpv6Server dhcpv6Server;

        // dhcpv6Server = new Dhcpv6Server(
        // new Examplev6LeaseManager(new AbstractDhcp6LeaseManager.Lifetimes(50000, 80000, 100000,
        // 150000), "fe80::a00:27ff:fe4f:7b7f"),
        // getPort().getValue(), new DuidOption.Duid(new byte[] { 1, 2 }));

        try {
            dhcpv6Server = new Dhcpv6Server(manager, port, options);
        } catch (Exception e) {
            LOG.error("DHCPv6 server on port {} failed to decode option arguments {}", port, e.toString());
            throw new IllegalStateException(e);
        }

        try {
            dhcpv6Server.addInterfaces(networkInterfaces);
        } catch (Exception e) {
            LOG.error("DHCP server on port {} failed to add network interfaces: {}", port, e.toString());
            throw new IllegalStateException(e);
        }

        try {
            dhcpv6Server.start(eventLoopGroup);
            LOG.info("DHCPv6 server on port {} started", port);

            return new AutoCloseable() {

                @Override
                public void close() throws Exception {
                    LOG.debug("Stopping DHCPv6 server on port {}", port);
                    dhcpv6Server.stop();
                    LOG.info("DHCPv6 server on port {} stopped", port);
                }
            };
        } catch (Exception e) {
            LOG.error("DHCPv6 server on port {} failed to start");
            throw new IllegalStateException(e);
        }
    }

}
