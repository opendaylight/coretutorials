/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutput.Status;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardingsimpleService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.ShardTestFactory.ShardTest;
import sharding.simple.impl.ShardTestFactory.TestStats;

/**
 * @author jmedved
 *
 */
public class ShardingsimpleServiceImpl implements ShardingsimpleService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ShardingsimpleServiceImpl.class);

    private final RpcProviderRegistry rpcRegistry;
    private final ShardTestFactory testFactory;
    private RpcRegistration<ShardingsimpleService> rpcReg;

    /** Constructor.
     * @param rpcRegistry: reference to MD-SAL RPC Registry
     * @param testFactory: reference to the Shard Test Factory
     */
    public ShardingsimpleServiceImpl(RpcProviderRegistry rpcRegistry, ShardTestFactory testFactory) {
        LOG.info("Creating Shardingsimple RPC service");
        this.rpcRegistry = rpcRegistry;
        this.testFactory = testFactory;
    }

    /** Initialization - called when Blueprint container is coming up.
     *
     */
    public void init() {
        LOG.info("Registering Shardingsimple RPC service");
        rpcReg = rpcRegistry.addRpcImplementation(ShardingsimpleService.class, this);
    }

    /** cleaning-up everything.
     *
     */
    @Override
    public void close() {
        rpcReg.close();
    }

    @Override
    public Future<RpcResult<ShardTestOutput>> shardTest(ShardTestInput input) {
        LOG.info("Input: {}", input);

        LogicalDatastoreType dst = input.getDataStore() == DataStore.CONFIG
                ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL;
        try {
            ShardTest shardTest = testFactory.createTest(input.getShards(), input.getDataItems(), dst);
            TestStats stats = shardTest.executeTest();

            List<Long> shardExecTime = new ArrayList<>();
            shardExecTime.add((long)1);
            shardExecTime.add((long)2);
            shardExecTime.add((long)3);

            ShardTestOutput output = new ShardTestOutputBuilder()
                                            .setStatus(Status.OK)
                                            .setListBuildTime((long)0)
                                            .setTotalExecTime((long)0)
                                            .setShardExecTime(shardExecTime)
                                            .setTxError(stats.getTxError())
                                            .setTxOk(stats.getTxOk())
                                            .setTxSubmitted(stats.getTxSubmitted())
                                            .build();
            return RpcResultBuilder.success(output).buildFuture();

        } catch (DOMDataTreeShardingConflictException | DOMDataTreeProducerException e) {
            LOG.error("Failed to create/execute Shard Test, {}", e);
            return RpcResultBuilder.success(new ShardTestOutputBuilder()
                                                    .setStatus(Status.FAILED)
                                                    .build()).buildFuture();
        }
    }
}
