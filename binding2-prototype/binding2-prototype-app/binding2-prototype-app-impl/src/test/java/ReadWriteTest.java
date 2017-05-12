/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.mdsal.binding.javav2.spec.base.InstanceIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.test.model.rev170511.data.ContTestModel;
import org.opendaylight.mdsal.gen.javav2.urn.opendaylight.params.xml.ns.yang.test.model.rev170511.data.ContTestModelBuilder;

public class ReadWriteTest {

    private static final InstanceIdentifier<ContTestModel> CONT_TEST_NODE_PATH
            = InstanceIdentifier.create(ContTestModel.class);

    @Test
    void testContTestNode() throws TransactionCommitFailedException {
        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();

        final ContTestModelBuilder hello = new ContTestModelBuilder().setLeafTestModel("Hello");

        writeTx.put(LogicalDatastoreType.OPERATIONAL, CONT_TEST_NODE_PATH, hello.build());
        writeTx.submit().checkedGet();

        final ReadTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<ContTestModel> contTestNode = readTx.read(LogicalDatastoreType.OPERATIONAL,
                CONT_TEST_NODE_PATH).get();

        assertTrue(contTestNode.isPresent());

        ContTestModel contTestModelContainer = contTestNode.get();
        assertEquals("Hello", contTestModelContainer.getLeafTestModel());
    }

}
