package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xmpp.agent.impl.rev141210;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;


public class NoopBindingConsumer implements BindingAwareConsumer {


    @Override
    public void onSessionInitialized(final ConsumerContext session) {
        // Intentionally NOOP
    }
}
