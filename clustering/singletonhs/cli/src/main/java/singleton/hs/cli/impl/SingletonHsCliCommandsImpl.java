/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package singleton.hs.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import singleton.hs.cli.api.SingletonHsCliCommands;

public class SingletonHsCliCommandsImpl implements SingletonHsCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonHsCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public SingletonHsCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("SingletonHsCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}