/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.binding2.prototype.demo.app;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.mdsal.binding.javav2.spec.base.RpcCallback;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.Binding2PrototypeAppHelloWorldRpc;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.HelloWorldInput;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.HelloWorldOutput;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.HelloWorldOutputBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Binding2PrototypeProvider
        implements BindingAwareProvider, Binding2PrototypeAppHelloWorldRpc, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Binding2PrototypeProvider.class);

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("Binding2PrototypeProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void invoke(final HelloWorldInput input, final RpcCallback<HelloWorldOutput> callback) {
        final HelloWorldOutputBuilder helloWorldOutputBuilder = new HelloWorldOutputBuilder();
        helloWorldOutputBuilder.setGreeting("Hello " + input.getName());

        callback.onSuccess(helloWorldOutputBuilder.build());
    }

}
