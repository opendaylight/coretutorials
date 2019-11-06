/*
 * Copyright (c) 2015 Cisco Systems, and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrud.rev150105.DscrudService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrudbenchmark.rev150105.DscrudbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrudbenchmark.rev150105.RpcCrudTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrudbenchmark.rev150105.RpcCrudTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dscrudbenchmark.rev150105.RpcCrudTestOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DscrudbenchmarkProvider implements DscrudbenchmarkService, BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DscrudbenchmarkProvider.class);
    private DataBroker dataBroker;
    protected DscrudService dscrudService;
    @Override
    public void onSessionInitiated(ProviderContext session) {
        session.addRpcImplementation(DscrudbenchmarkService.class, this);
        dscrudService = session.getRpcService(DscrudService.class);
        this.dataBroker = session.getSALService(DataBroker.class);
        LOG.info("Dscrudbenchmark Session Initiated");    }

    @Override
    public void close() throws Exception {
        LOG.info("Dscrudbenchmark Closed");
    }

    /**
     * RPC to start tests.
     * @param input
     * @return
     */
    @Override
    public ListenableFuture<RpcResult<RpcCrudTestOutput>> rpcCrudTest(RpcCrudTestInput input) {

        long numResources;
        long numThreads;
        RpcCrudTestOutput output;


        numResources = input.getNumResources();
        if (numResources <= 0) numResources = 1;
        numThreads = input.getNumThreads();
        if (numThreads <= 0) numThreads = 1;

        LOG.info("Test started: numResources: {} numThreads: {}",
                numResources, numThreads);
        PerfCrudRpc perfCrudRpc = new PerfCrudRpc(dscrudService);
        boolean status = perfCrudRpc.runPerfTest((int) numResources, (int) numThreads);

        output = new RpcCrudTestOutputBuilder()
                .setStatus(status ? RpcCrudTestOutput.Status.OK : RpcCrudTestOutput.Status.FAILED)
                .setCreatesPerSec(perfCrudRpc.createsPerSec)
                .setRetrievesPerSec(perfCrudRpc.retrievesPerSec)
                .setCrudsPerSec(perfCrudRpc.crudsPerSec)
                .setDeletesPerSec(perfCrudRpc.deletesPerSec)
                .build();

        return RpcResultBuilder.success(output).buildFuture();

    }


}
