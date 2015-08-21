/*
 * Copyright (c) 2015 Cisco Systems, and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.Monitor;
import java.util.Collections;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.test.exec.OuterList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DscrudProvider implements BindingAwareProvider, DscrudService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DscrudProvider.class);

    private static final InstanceIdentifier<TestExec> TEST_EXEC_IID = InstanceIdentifier.builder(TestExec.class).build();
    private BindingAwareBroker.RpcRegistration<DscrudService> dstReg;
    private DataBroker dataBroker;
    private Monitor crudMonitor;
    private SimpleTxCrudMonitor simpletxCrudMonitor;
    private long testsCompleted = 0;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        this.dataBroker = session.getSALService(DataBroker.class);
        this.dstReg = session.addRpcImplementation(DscrudService.class, this);

        // this is for the simple tx crud monitor where Create/Delete are "semaphore" protected, and reads run concurrently
        crudMonitor = new Monitor();
        simpletxCrudMonitor = new SimpleTxCrudMonitor(this.dataBroker, crudMonitor);
        LOG.info("DscrudProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        dstReg.close();
        LOG.info("DscrudProvider Closed");
    }

    /**
     * clean data store
     *
     */
    @Override
    public Future<RpcResult<Void>> cleanupStore() {
        cleanupTestStore();
        LOG.info("Data Store cleaned up");
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    /**
     * Perform a crud test
     *
     */
    @Override
    public Future<RpcResult<DoCrudOutput>> doCrud(DoCrudInput input) {
        //LOG.info("Starting the data store benchmark test, input: {}", input);

        DoCrudInput.Operation oper = input.getOperation();
        int outerListId = input.getOuterElementId().intValue();
        int numInnerListElements = input.getInnerElements().intValue();
        
        // Run the test and measure the execution time
        try {
            simpletxCrudMonitor.doOper(oper, outerListId, numInnerListElements);

        } catch ( Exception e ) {
            LOG.error( "Test error: {}", e.toString());
            return RpcResultBuilder.success(new DoCrudOutputBuilder()
                    .setStatus(DoCrudOutput.Status.FAILED)
                    .build()).buildFuture();
        }
        
        DoCrudOutput output = new DoCrudOutputBuilder()
                .setStatus(DoCrudOutput.Status.OK)
                .setTxOk((long) simpletxCrudMonitor.txOk)
                .setTxError((long) simpletxCrudMonitor.txError)
                .setVerifyError((long) simpletxCrudMonitor.verifyError)
                .setVerifyOk((long) simpletxCrudMonitor.verifyOk)
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    private void cleanupTestStore() {
        TestExec data = new TestExecBuilder()
                .setOuterList(Collections.<OuterList>emptyList())
                .build();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, TEST_EXEC_IID, data);
        try {
            tx.submit().checkedGet();
            LOG.info("DataStore test data cleaned up");
        } catch (TransactionCommitFailedException e) {
            LOG.info("Failed to cleanup DataStore test data");
            throw new IllegalStateException(e);
        }
    }
}
