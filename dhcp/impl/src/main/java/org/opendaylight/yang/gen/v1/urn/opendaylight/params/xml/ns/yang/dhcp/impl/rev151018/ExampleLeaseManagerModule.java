package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.dhcp.server.ExampleLeaseManager;

public class ExampleLeaseManagerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dhcp.impl.rev151018.AbstractExampleLeaseManagerModule {

    public ExampleLeaseManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ExampleLeaseManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, ExampleLeaseManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
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
        return new ExampleLeaseManager(getIp());
    }

}
