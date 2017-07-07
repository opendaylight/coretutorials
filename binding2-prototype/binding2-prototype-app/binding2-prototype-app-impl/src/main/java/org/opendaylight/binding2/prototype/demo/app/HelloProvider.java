/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.binding2.prototype.demo.app;

import org.opendaylight.mdsal.binding.javav2.api.RpcActionProviderService;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.Binding2PrototypeAppHelloWorldRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloProvider {

    private static final Logger LOG = LoggerFactory.getLogger(HelloProvider.class);

    private final RpcActionProviderService operationService;

    public HelloProvider(final RpcActionProviderService operationService) {
        this.operationService = operationService;
    }

    public void init() {
        LOG.info("HelloProvider Session Initiated");
        operationService.registerRpcImplementation(Binding2PrototypeAppHelloWorldRpc.class,
                new Binding2PrototypeAppHelloWorldRpcImpl());
    }

    public void close() {
        LOG.info("HelloProvider Closed");
    }
}
