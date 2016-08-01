/*
 * Copyright © 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package  org.opendaylight.coretutorials.hweventsource.impl;

import static org.mockito.Mockito.mock;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

public class HweventsourceProviderTest {
    @Test
    public void testOnSessionInitiated() {
        final HweventsourceBAProvider provider = new HweventsourceBAProvider();

        // ensure no exceptions
        // currently this method is empty
        provider.onSessionInitiated(mock(BindingAwareBroker.ProviderContext.class));
    }

    @Test
    public void testClose() throws Exception {
        final HweventsourceBAProvider provider = new HweventsourceBAProvider();

        // ensure no exceptions
        // currently this method is empty
        provider.close();
    }
}
