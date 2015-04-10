package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.uagent.rev150408;

import java.io.File;

import org.opendaylight.controller.hweventsource.uagent.NoopDOMConsumer;
import org.opendaylight.controller.hweventsource.uagent.Providers;
import org.opendaylight.controller.hweventsource.uagent.UserAgent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HweventsourceUagentModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.uagent.rev150408.AbstractHweventsourceUagentModule {

    private static final Logger LOG = LoggerFactory.getLogger(HweventsourceUagentModule.class);

    public HweventsourceUagentModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HweventsourceUagentModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.uagent.rev150408.HweventsourceUagentModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final ProviderContext bindingCtx = getBrokerDependency().registerProvider(new Providers.BindingAware());
        final RpcProviderRegistry rpcRegistry = bindingCtx.getSALService(RpcProviderRegistry.class);
        final DOMNotificationService notifyService = getDomBrokerDependency()
                .registerConsumer(new NoopDOMConsumer())
                .getService(DOMNotificationService.class);
        final File outputFile = new File(getOutputFileName());
        UserAgent ua = UserAgent.create(notifyService,rpcRegistry, outputFile);

        if(ua != null){
            LOG.info("HweventsourceUagent has been initialized");
        } else {
            LOG.error("HweventsourceUagent has not been initialized");
        }
        return ua;
    }

}
