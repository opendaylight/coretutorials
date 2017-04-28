/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.binding2.prototype.app;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Binding2PrototypeProvider implements BindingAwareProvider, /* Binding2PrototypeService, */ AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Binding2PrototypeProvider.class);

    /* private RpcRegistration<Binding2PrototypeService> rpcReg; */

    /*
     * @Override public Future<RpcResult<HelloWorldOutput>> helloWorld(final
     * HelloWorldInput input) { final HelloWorldOutputBuilder helloBuilder = new
     * HelloWorldOutputBuilder(); helloBuilder.setGreeting("Hello " +
     * input.getName()); return
     * RpcResultBuilder.success(helloBuilder.build()).buildFuture(); }
     */

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        /*
         * this.rpcReg =
         * session.addRpcImplementation(Binding2PrototypeService.class, this);
         */
        LOG.info("Binding2PrototypeProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        /* this.rpcReg.close(); */
    }

}
