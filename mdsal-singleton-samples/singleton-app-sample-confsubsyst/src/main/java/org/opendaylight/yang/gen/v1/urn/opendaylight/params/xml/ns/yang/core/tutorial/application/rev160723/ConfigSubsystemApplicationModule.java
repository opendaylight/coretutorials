/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.core.tutorial.application.rev160723;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;

/**
 *
 */
public class ConfigSubsystemApplicationModule extends AbstractConfigSubsystemApplicationModule {

    public ConfigSubsystemApplicationModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ConfigSubsystemApplicationModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver, final ConfigSubsystemApplicationModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
