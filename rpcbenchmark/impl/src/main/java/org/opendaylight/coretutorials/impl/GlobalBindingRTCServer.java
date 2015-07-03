package org.opendaylight.coretutorials.impl;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchInput;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchOutput;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.GlobalRpcBenchOutputBuilder;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.OdlTestRpcbenchPayloadService;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.RoutedRpcBenchInput;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.RoutedRpcBenchOutput;
import org.opendaylight.yang.gen.v1.odl.test.rpcbench.payload.rev150702.RoutedRpcBenchOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;



public class GlobalBindingRTCServer implements OdlTestRpcbenchPayloadService {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalBindingRTCServer.class);
    private int numRpcs = 0;

    public GlobalBindingRTCServer() {
    	LOG.info("GlobalBindingRTCServer created.");
    }
    
	@Override
	public Future<RpcResult<GlobalRpcBenchOutput>> globalRpcBench(
			GlobalRpcBenchInput input) {
		GlobalRpcBenchOutput output = new GlobalRpcBenchOutputBuilder(input).build();
		RpcResult<GlobalRpcBenchOutput> result = RpcResultBuilder.success(output).build();
		numRpcs++;
		return Futures.immediateFuture(result);
	}

	@Override
	public Future<RpcResult<RoutedRpcBenchOutput>> routedRpcBench(
			RoutedRpcBenchInput input) {
		RoutedRpcBenchOutput output = new RoutedRpcBenchOutputBuilder(input).build();
		RpcResult<RoutedRpcBenchOutput> result = RpcResultBuilder.success(output).build();
		numRpcs++;
		return Futures.immediateFuture(result);
	}

	public int getNumRpcs() {
		return numRpcs;
	}
}
