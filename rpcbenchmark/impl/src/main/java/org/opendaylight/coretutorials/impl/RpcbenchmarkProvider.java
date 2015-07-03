/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchInput;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchInputBuilder;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.OdlTestRpcbenchPayloadService;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.payload.Payload;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.payload.PayloadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.RpcbenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.StartTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutput.ExecStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rpcbenchmark.rev150702.TestStatusOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcbenchmarkProvider implements BindingAwareProvider, AutoCloseable, RpcbenchmarkService {

    private static final Logger LOG = LoggerFactory.getLogger(RpcbenchmarkProvider.class);
    private static final GlobalBindingRTCServer gServer = new GlobalBindingRTCServer();
    private static final int testTimeout = 5;
    private final AtomicReference<ExecStatus> execStatus = new AtomicReference<ExecStatus>(ExecStatus.Idle );
    private static RpcConsumerRegistry registry;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("RpcbenchmarkProvider Session Initiated");
        registry = session.getSALService(RpcConsumerRegistry.class);
        // Register the benchmark Global RPC
        session.addRpcImplementation(OdlTestRpcbenchPayloadService.class, gServer);
        // Register RPC Benchmark's control REST API
        session.addRpcImplementation(RpcbenchmarkService.class, this);
    }

    @Override
    public void close() throws Exception {
        LOG.info("RpcbenchmarkProvider Closed");
    }

	@Override
	public Future<RpcResult<StartTestOutput>> startTest(final StartTestInput input) {
        LOG.info("startTest {}", input);
        
        final GlobalBindingRTCClient client = new GlobalBindingRTCClient(registry);
        ExecutorService executor = Executors.newFixedThreadPool(input.getNumThreads().intValue());
        
		final List<Payload> inVal = new ArrayList<>();
		for (int i = 0; i < input.getPayloadSize().intValue(); i++) {
			inVal.add(new PayloadBuilder().setId(i).build());
		}
		final GlobalRpcBenchInput rpcInput = new GlobalRpcBenchInputBuilder()
													.setPayload(inVal)
													.build();

		final Runnable testRun = new Runnable() {
			@Override
			public void run() {
				client.runTest(input.getIterations().intValue(), rpcInput, inVal.size());					
			}
		};
 
		LOG.info("Test Started");
        long startTime = System.nanoTime();
 
        for (int i = 0; i < input.getNumThreads().intValue(); i++ ) {
        	executor.submit(testRun);
        }
                
        executor.shutdown();
        try {
			executor.awaitTermination(testTimeout, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			LOG.error("Out of time: test did not finish within the {} min deadline ", testTimeout); 
		}
        
        long endTime = System.nanoTime();
		LOG.info("Test Done");
        
        long elapsedTime = endTime - startTime;

        StartTestOutput output = new StartTestOutputBuilder()
        								.setRate((long)0)
        								.setGlobalRtcClientError((long)client.getRpcError())
        								.setGlobalRtcClientOk((long)client.getRpcOk())
        								.setExecTime(TimeUnit.NANOSECONDS.toMillis(elapsedTime))
        								.setRate(((client.getRpcOk() + client.getRpcError()) * 1000000000) / elapsedTime)
        								.build();
		return RpcResultBuilder.success(output).buildFuture();
	}

	@Override
	public Future<RpcResult<TestStatusOutput>> testStatus() {
        LOG.info("testStatus");
        TestStatusOutput output = new TestStatusOutputBuilder()
        								.setGlobalServerCnt((long)gServer.getNumRpcs())
        								.setExecStatus(execStatus.get())       								
        								.build();
		return RpcResultBuilder.success(output).buildFuture();
	}

}
