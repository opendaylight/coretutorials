package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018;

import java.net.SocketException;
import java.util.HashSet;
import java.util.List;

import org.anarres.dhcp.v6.service.Dhcp6LeaseManager;
import org.anarres.dhcp.v6.service.Dhcp6Service;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.dhcpv6.server.CustomisableLeaseManagerDhcpv6Service;
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
        return startServer(getPort().getValue().shortValue(), getNetworkInterface(), getLeaseManagerDependency(),
                getDefaultOption(), getWorkerThreadGroupDependency(), getServerId().shortValue());
    }

    private static AutoCloseable startServer(final Short port, final List<String> networkInterfaces,
            final Dhcp6LeaseManager manager, final List<DefaultOption> options, EventLoopGroup eventLoopGroup,
            Short serverId) {
        byte[] duid = new byte[2];
        duid[0] = (byte) (serverId & 0xff);
        duid[1] = (byte) ((serverId >> 8) & 0xff);
        final Dhcp6Service dhcp6Service = new CustomisableLeaseManagerDhcpv6Service(manager, duid, options);

        final Dhcpv6Server dhcpv6Server;
        try {
            dhcpv6Server = new Dhcpv6Server(dhcp6Service, serverId, port, new HashSet<String>(networkInterfaces));
        } catch (SocketException e) {
            LOG.error("DHCPv6 server on port {} failed to initialise {}", e.toString());
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
            LOG.warn("DHCPv6 server on port {} failed to start {}", e.toString());
            throw new IllegalStateException(e);
        }
    }

}
