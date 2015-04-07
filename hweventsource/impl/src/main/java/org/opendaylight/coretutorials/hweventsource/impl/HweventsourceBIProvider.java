/*
 *  Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.hweventsource.impl;

import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HweventsourceBIProvider extends AbstractProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HweventsourceBIProvider.class);

    @Override
    public void onSessionInitiated(ProviderSession session) {
        LOG.info("HweventsourceBAProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("HweventsourceBAProvider Closed");
    }

}
