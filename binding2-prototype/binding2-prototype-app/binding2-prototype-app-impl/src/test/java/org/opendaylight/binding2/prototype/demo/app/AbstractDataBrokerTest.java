/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.binding2.prototype.demo.app;

import com.google.common.annotations.Beta;
import org.opendaylight.mdsal.binding.javav2.api.DataBroker;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@Beta
public class AbstractDataBrokerTest extends AbstractSchemaAwareTest {

    private DataBroker dataBroker;

    @Override
    protected void setupWithSchema(final SchemaContext context) {
        DataBrokerTestCustomizer testCustomizer = createDataBrokerTestCustomizer();
        dataBroker = testCustomizer.createDataBroker();
        testCustomizer.updateSchema(context);
        setupWithDataBroker(dataBroker);
    }

    private void setupWithDataBroker(final DataBroker dataBroker) {
        // Intentionally left No-op, subclasses may customize it
    }

    private DataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new DataBrokerTestCustomizer();
    }

    DataBroker getDataBroker() {
        return dataBroker;
    }
}
