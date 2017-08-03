/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.binding2.prototype.demo.app;

import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.mdsal.binding.javav2.api.WriteTransaction;
import org.opendaylight.mdsal.binding.javav2.spec.base.InstanceIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.test.model.rev170711.data.ContTestModel;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.test.model.rev170711.dto.ContTestModelBuilder;

public class ReadWriteTest extends AbstractDataBrokerTest {

    private static final InstanceIdentifier<ContTestModel> CONT_TEST_NODE_PATH
            = InstanceIdentifier.create(ContTestModel.class);

    @Test
    public void testContTestNode() throws ExecutionException, InterruptedException {
        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();

        final ContTestModelBuilder hello = new ContTestModelBuilder().setAugmentedLeaf("Hello");

        writeTx.put(LogicalDatastoreType.OPERATIONAL, CONT_TEST_NODE_PATH, hello.build());
        writeTx.submit().get();
    }
}