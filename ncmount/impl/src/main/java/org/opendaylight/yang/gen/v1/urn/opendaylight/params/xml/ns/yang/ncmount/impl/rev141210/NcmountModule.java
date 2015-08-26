/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.impl.rev141210;

import ncmount.impl.NcmountDomProvider;
import ncmount.impl.NcmountProvider;

public class NcmountModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.impl.rev141210.AbstractNcmountModule {
    public NcmountModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NcmountModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ncmount.impl.rev141210.NcmountModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
//        final NcmountProvider provider = new NcmountProvider();
//        getBrokerDependency().registerProvider(provider);

        final NcmountDomProvider domProvider = new NcmountDomProvider();
        getDomBrokerDependency().registerProvider(domProvider);

        // Now we have 2 resources to close
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                //provider.close();
                domProvider.close();
            }
        };
    }

}
