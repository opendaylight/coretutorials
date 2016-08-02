/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package sharding.simple.impl;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShardingSimpleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ShardingSimpleProvider.class);

    private static final int DCL_EXECUTOR_MAX_POOL_SIZE = 20;
    private static final int DCL_EXECUTOR_MAX_QUEUE_SIZE = 1000;
    private static final int MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE = 1000;
    private static final int MAX_COMMIT_QUEUE_SIZE = 1000;

    private final DataBroker dataBroker;
    private final DOMDataTreeShardingService dataTreeShardingService;
    private final DOMDataTreeService dataTreeService;

    private ListenerRegistration<InMemoryDOMDataTreeShard> dataTreeShardReg;

    public ShardingSimpleProvider(final DataBroker dataBroker,
                                  final DOMDataTreeShardingService dataTreeShardingService,
                                  final DOMDataTreeService dataTreeService) {
        this.dataBroker = dataBroker;
        this.dataTreeShardingService = dataTreeShardingService;
        this.dataTreeService = dataTreeService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ShardingSimpleProvider Session Initiated");

        LOG.info("Registgering shard at CONFIG data store root");
        DOMDataTreeIdentifier dtiConfig = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                YangInstanceIdentifier.EMPTY);

        ExecutorService configRootShardExecutor =
                SpecialExecutors.newBlockingBoundedFastThreadPool(DCL_EXECUTOR_MAX_POOL_SIZE,
                        DCL_EXECUTOR_MAX_QUEUE_SIZE, LogicalDatastoreType.CONFIGURATION + "RootShard-DCL");

        InMemoryDOMDataTreeShard configRootShard = InMemoryDOMDataTreeShard.create(dtiConfig, configRootShardExecutor,
                MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, MAX_COMMIT_QUEUE_SIZE);

        LOG.info("Creating producer for name space claim");
        DOMDataTreeProducer producer = dataTreeService.createProducer(Collections.singletonList(dtiConfig));

        try {
            dataTreeShardReg = dataTreeShardingService.registerDataTreeShard(dtiConfig, configRootShard, producer);
        } catch (DOMDataTreeShardingConflictException e) {
            // TODO Auto-generated catch block
            LOG.error("Exception in dataTreeShardingService, {}", e);
        }

        LOG.info("Creating a transaction chain for the producer");
        DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(false);
        DOMDataTreeWriteCursor cursor = tx.createCursor(dtiConfig);
        // cursor.write(child, data);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ShardingSimpleProvider Closed");

        dataTreeShardReg.close();
    }
}