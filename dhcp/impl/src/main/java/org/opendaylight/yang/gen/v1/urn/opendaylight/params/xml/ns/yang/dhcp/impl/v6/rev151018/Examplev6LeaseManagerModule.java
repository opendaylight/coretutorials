package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018;

import org.anarres.dhcp.v6.service.AbstractDhcp6LeaseManager;
import org.opendaylight.dhcpv6.server.Examplev6LeaseManager;

public class Examplev6LeaseManagerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.v6.rev151018.AbstractExamplev6LeaseManagerModule {
    public Examplev6LeaseManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Examplev6LeaseManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, Examplev6LeaseManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new Examplev6LeaseManager(new AbstractDhcp6LeaseManager.Lifetimes(50000, 80000, 100000, 150000), getIp());
    }

}
