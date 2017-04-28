package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.impl.rev170505;

import org.opendaylight.binding2.prototype.demo.app.Binding2PrototypeProvider;

public class Binding2PrototypeAppModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.impl.rev170505.AbstractBinding2PrototypeAppModule {
    public Binding2PrototypeAppModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Binding2PrototypeAppModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.impl.rev170505.Binding2PrototypeAppModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final Binding2PrototypeProvider provider = new Binding2PrototypeProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
