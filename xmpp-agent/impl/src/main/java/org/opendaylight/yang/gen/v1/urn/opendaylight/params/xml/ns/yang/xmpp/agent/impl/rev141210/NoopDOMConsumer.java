package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.xmpp.agent.impl.rev141210;

import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.Consumer;

public class NoopDOMConsumer implements Consumer {

    @Override
    public void onSessionInitiated(final ConsumerSession session) {
        // NOOP
    }

    @Override
    public Collection<ConsumerFunctionality> getConsumerFunctionality() {
        return Collections.emptySet();
    }

}
