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

import com.google.common.primitives.Shorts;

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
        JmxAttributeValidationException.checkNotNull(getPort(), "port-number not set", portJmxAttribute);
        Integer portNumber = getPort().getValue();
        JmxAttributeValidationException.checkCondition((portNumber > 0 && portNumber < 65535),
                "port-number out of range", portJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getNetworkInterface(), "network-interface not set",
                networkInterfaceJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getServerId(), "server-id", serverIdJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getLeaseManager(), "lease-manager not set",
                leaseManagerJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("Starting DHCPv6 server on port {}", getPort());

        if (getPort() == null)
            System.out.println("Port == null");
        if (getNetworkInterface() == null)
            System.out.println("NetworkInterface == null");
        if (getLeaseManagerDependency() == null)
            System.out.println("LeaseManagerDependency == null");
        if (getDefaultOption() == null)
            System.out.println("DefaultOption == null");
        if (getWorkerThreadGroupDependency() == null)
            System.out.println("WorkerThreadGroupDependency == null");
        if (getServerId() == null)
            System.out.println("ServerId == null");

        return startServer(getPort().getValue().shortValue(), getNetworkInterface(), getLeaseManagerDependency(),
                getDefaultOption(), getWorkerThreadGroupDependency(), getServerId().shortValue());
    }

    private static AutoCloseable startServer(final Short port, final List<String> networkInterfaces,
            final Dhcp6LeaseManager manager, final List<DefaultOption> options, EventLoopGroup eventLoopGroup,
            Short serverDuid) {
        final Dhcp6Service dhcp6Service =
                new CustomisableLeaseManagerDhcpv6Service(manager, Shorts.toByteArray(serverDuid), options);

        final Dhcpv6Server dhcpv6Server;
        try {
            dhcpv6Server = new Dhcpv6Server(dhcp6Service, serverDuid, port, new HashSet<String>(networkInterfaces));
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
