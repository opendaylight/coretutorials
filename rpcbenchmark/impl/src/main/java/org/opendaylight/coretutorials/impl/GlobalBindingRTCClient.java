package org.opendaylight.coretutorials.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchInput;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchInputBuilder;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchOutput;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.OdlTestRpcbenchPayloadService;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.payload.Payload;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.payload.PayloadBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalBindingRTCClient {
	
    private static final Logger LOG = LoggerFactory.getLogger(GlobalBindingRTCClient.class);
    private final OdlTestRpcbenchPayloadService service;
	private final AtomicLong rpcOk = new AtomicLong(0);
	private final AtomicLong rpcError = new AtomicLong(0);
	
	public long getRpcOk() {
		return rpcOk.get();
	}
	
	public long getRpcError() {
		return rpcError.get();
	}

	public GlobalBindingRTCClient(RpcConsumerRegistry registry) {
		if (registry != null) {
			this.service = registry.getRpcService(OdlTestRpcbenchPayloadService.class);
		} else {
			this.service = null;
		}
	}
	
	void runTest(int iterations) {
		int rpcOk = 0;
		int rpcError = 0;
		LOG.info("Test Started");
		List<Payload> inVal = new ArrayList<>();
		inVal.add(new PayloadBuilder().setId(1).build());
		
		GlobalRpcBenchInput input = new GlobalRpcBenchInputBuilder()
											.setPayload(inVal)
											.build();
		
		for (int i = 0; i < iterations; i++) {
			Future<RpcResult<GlobalRpcBenchOutput>> output = service.globalRpcBench(input);
			try {
				RpcResult<GlobalRpcBenchOutput> rpcResult = output.get();
				
				if (rpcResult.isSuccessful()) {
					List<Payload> retVal = rpcResult.getResult().getPayload();	
					if (retVal.size() == inVal.size()) {
						rpcOk++;
					}
					else {
						rpcError++;
					}
				}
			} catch (InterruptedException e) {
				rpcError++;
				LOG.error("Execution failed: ", e); 
			} catch (ExecutionException e) {
				rpcError++;
				LOG.error("Execution failed: ", e); 
			}
		}
		
		this.rpcOk.addAndGet(rpcOk);
		this.rpcError.addAndGet(rpcError);
	}

}
