/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.binding2.prototype.demo.app;

import org.opendaylight.mdsal.binding.javav2.spec.base.RpcCallback;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.Binding2PrototypeAppHelloWorldRpc;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.data.hello_world.HelloWorldInput;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.data.hello_world.HelloWorldOutput;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.binding2.prototype.app.rev170505.dto.hello_world.HelloWorldOutputBuilder;

public class Binding2PrototypeAppHelloWorldRpcImpl implements Binding2PrototypeAppHelloWorldRpc {

    @Override
    public void invoke(final HelloWorldInput input, final RpcCallback<HelloWorldOutput> callback) {
        final HelloWorldOutputBuilder helloWorldOutputBuilder = new HelloWorldOutputBuilder();
        helloWorldOutputBuilder.setGreeting("Hello " + input.getName());
        callback.onSuccess((HelloWorldOutput)helloWorldOutputBuilder.build());
    }

}
