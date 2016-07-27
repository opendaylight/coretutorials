/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.confsubsyst.rev160727;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.coretutorials.clustering.SingletonAppSampleCssProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;

public class ConfigSubsystemApplicationModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.coretutorials.singleton.app.confsubsyst.rev160727.AbstractConfigSubsystemApplicationModule {

    public ConfigSubsystemApplicationModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ConfigSubsystemApplicationModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver,
            final ConfigSubsystemApplicationModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final ClusterSingletonServiceProvider cssProvider = getClusterSingletonServiceProviderDependency();
        final RpcProviderRegistry rpcProviderRegistry = getRpcRegistryDependency();
        final SingletonAppSampleCssProvider provider = new SingletonAppSampleCssProvider(rpcProviderRegistry, cssProvider);
        provider.init();
        return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                provider.close();
            }
        };
    }

}
