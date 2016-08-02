/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package singleton.simple.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import singleton.simple.cli.api.SingletonSimpleCliCommands;

public class SingletonSimpleCliCommandsImpl implements SingletonSimpleCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonSimpleCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public SingletonSimpleCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("SingletonSimpleCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}