/*
 * Copyright (c) 2015 Cisco Systems. Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.toaster;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToasterImpl implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ToasterImpl.class);

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSessionInitiated(ProviderContext arg0) {
        LOG.info("Hello World!");

    }

}