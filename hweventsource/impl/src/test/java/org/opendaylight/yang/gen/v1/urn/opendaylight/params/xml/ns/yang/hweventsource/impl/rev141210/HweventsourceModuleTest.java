/*
 *  Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.impl.rev141210;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;

public class HweventsourceModuleTest {
    @Test
    public void testCustomValidation() {
        HweventsourceModule module = new HweventsourceModule(mock(ModuleIdentifier.class), mock(DependencyResolver.class));

        // ensure no exceptions on validation
        // currently this method is empty
        module.customValidation();
    }

//    @Test
//    public void testCreateInstance() throws Exception {
//        // configure mocks
//        DependencyResolver dependencyResolver = mock(DependencyResolver.class);
//        BindingAwareBroker broker = mock(BindingAwareBroker.class);
//        when(dependencyResolver.resolveInstance(eq(BindingAwareBroker.class), any(ObjectName.class), any(JmxAttribute.class))).thenReturn(broker);
//
//        // create instance of module with injected mocks
//        HweventsourceModule module = new HweventsourceModule(mock(ModuleIdentifier.class), dependencyResolver);
//
//        // getInstance calls resolveInstance to get the broker dependency and then calls createInstance
//        AutoCloseable closeable = module.getInstance();
//
//        // verify that the module registered the returned provider with the broker
//        verify(broker).registerProvider((HweventsourceBAProvider)closeable);
//
//        // ensure no exceptions on close
//        closeable.close();
//    }
}
