package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.config.rev141210;

import org.opendaylight.toaster.ToasterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToasterImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.config.rev141210.AbstractToasterImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(ToasterImplModule.class);

    public ToasterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ToasterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.config.rev141210.ToasterImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        LOG.info("Performing custom validation");
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Creating a new Toaster instance");
        ToasterImpl provider = new ToasterImpl();
        String logMsg = "Provider: " + provider.toString();
        LOG.info(logMsg);
        getBindingAwareBrokerDependency().registerProvider(provider, null);
        return provider;
    }

}
