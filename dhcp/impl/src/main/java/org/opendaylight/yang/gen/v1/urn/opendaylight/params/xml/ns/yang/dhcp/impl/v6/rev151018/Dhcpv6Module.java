package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018;

import java.io.IOException;
import org.anarres.dhcp.v6.options.DuidOption;
import org.anarres.dhcp.v6.service.AbstractDhcp6LeaseManager;
import org.apache.directory.server.dhcp.netty.Dhcp6Server;
import org.opendaylight.dhcp.server.Examplev6LeaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dhcpv6Module extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018.AbstractDhcpv6Module {

    private static final Logger LOG = LoggerFactory.getLogger(Dhcpv6Module.class);

    public Dhcpv6Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Dhcpv6Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, Dhcpv6Module oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        final Dhcp6Server dhcp6Server = new Dhcp6Server(
            new Examplev6LeaseManager(new AbstractDhcp6LeaseManager.Lifetimes(50000, 80000, 100000, 150000), "fe80::a00:27ff:fe4f:7b7f"),
            getPort().getValue(), new DuidOption.Duid(new byte[] { 1, 2 }));

        try {
            dhcp6Server.start();
            LOG.info("DHCP server on port {} started", getPort());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new AutoCloseable() {
            @Override public void close() throws Exception {
                dhcp6Server.stop();
            }
        };
    }

}
