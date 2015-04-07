/*
 *  Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.hweventsource.uagent;

import java.util.Collection;
import java.util.Collections;

import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopDOMConsumer implements Consumer {

    private static final Logger LOG = LoggerFactory.getLogger(NoopDOMConsumer.class);

    @Override
    public void onSessionInitiated(final ConsumerSession session) {
        LOG.info("NoopDOMConsumer initialized");
    }

    @Override
    public Collection<ConsumerFunctionality> getConsumerFunctionality() {
        return Collections.emptySet();
    }

}
