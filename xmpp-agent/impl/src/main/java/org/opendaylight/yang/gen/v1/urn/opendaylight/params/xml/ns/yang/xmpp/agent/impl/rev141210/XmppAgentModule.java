package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xmpp.agent.impl.rev141210;

import org.opendaylight.coretutorials.agent.xmpp.impl.XmppUserAgentFactory;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;

public class XmppAgentModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xmpp.agent.impl.rev141210.AbstractXmppAgentModule {
    public XmppAgentModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public XmppAgentModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xmpp.agent.impl.rev141210.XmppAgentModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DataBroker dataBroker = getBindingBrokerDependency()
                .registerConsumer(new NoopBindingConsumer())
                .getSALService(DataBroker.class);
        final DOMNotificationService notifyService = getDomBrokerDependency()
                .registerConsumer(new NoopDOMConsumer())
                .getService(DOMNotificationService.class);
        return new XmppUserAgentFactory(dataBroker, notifyService);
    }

}
