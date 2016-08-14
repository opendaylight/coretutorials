/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class ShardHelper implements AutoCloseable, SchemaContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(ShardHelper.class);

    private static final int DCL_EXECUTOR_MAX_POOL_SIZE = 20;
    private static final int DCL_EXECUTOR_MAX_QUEUE_SIZE = 1000;
    private static final int DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE = 1000;
    private static final int COMMIT_MAX_QUEUE_SIZE = 1000;

    private final DOMDataTreeShardingService dataTreeShardingService;
    private final DOMDataTreeService dataTreeService;
    private final SchemaService schemaService;
    private final ListenerRegistration<SchemaContextListener> schemaServiceRegistration;

    private final List<ListenerRegistration<InMemoryDOMDataTreeShard>> dataTreeShardRegistrations = new ArrayList<>();
    private final Map<DOMDataTreeIdentifier, ShardData> shardDb = new HashMap<>();

    /** Constructor.
     * @param dataTreeShardingService: reference to MD-SAL Data Tree Sharding Service
     * @param dataTreeService: reference to MD-SAL Data Tree  Service
     * @param schemaService: reference to MD-SAL Schema Service
     */
    public ShardHelper(DOMDataTreeShardingService dataTreeShardingService,
            DOMDataTreeService dataTreeService,
            SchemaService schemaService) {
        this.dataTreeShardingService = dataTreeShardingService;
        this.dataTreeService = dataTreeService;
        this.schemaService = schemaService;
        schemaServiceRegistration = schemaService.registerSchemaContextListener(this);

        LOG.info("ShardHelper Created & Initialized");
    }

    /** Creates a shard, registers it with dataTreeShardingService and
     * creates a producer for the shard.
     * @param dataStoreType: Data store type (OPER/CONFIG) where to create the shard
     * @param yiId: Instance identifier (subtree) where to create the shard
     * @return: Producer for the shard
     * @throws DOMDataTreeShardingConflictException: somebody else already registered on the shard
     */
    public ShardData createAndInitShard(LogicalDatastoreType dataStoreType, YangInstanceIdentifier yiId)
            throws DOMDataTreeShardingConflictException {

        final DOMDataTreeIdentifier ddtId = new DOMDataTreeIdentifier(dataStoreType, yiId);

        final ExecutorService configRootShardExecutor =
                SpecialExecutors.newBlockingBoundedFastThreadPool(ShardHelper.DCL_EXECUTOR_MAX_POOL_SIZE,
                                                                  ShardHelper.DCL_EXECUTOR_MAX_QUEUE_SIZE,
                                                                  ddtId.getDatastoreType() + "RootShard-DCL");
        final InMemoryDOMDataTreeShard shard =
                InMemoryDOMDataTreeShard.create(ddtId,
                                                configRootShardExecutor,
                                                ShardHelper.DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE,
                                                ShardHelper.COMMIT_MAX_QUEUE_SIZE);

        final DOMDataTreeProducer producer = dataTreeService.createProducer(Collections.singletonList(ddtId));
        final ListenerRegistration<InMemoryDOMDataTreeShard> dataTreeShardReg =
                    dataTreeShardingService.registerDataTreeShard(ddtId, shard, producer);
        dataTreeShardRegistrations.add(dataTreeShardReg);

        shard.onGlobalContextUpdated(schemaService.getGlobalContext());

        ShardData shardData = new ShardData(ddtId, shard, producer);
        shardDb.put(ddtId, shardData);
        LOG.debug("Created shard for {}, shard: {}, provider: {}", ddtId, shard, producer);

        return shardData;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext schemaContext) {
        shardDb.forEach((key, dbEntry) -> dbEntry.getShard().onGlobalContextUpdated(schemaContext));
    }

    /* (non-Javadoc) Close down the Shard Manager.
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close()  {
        clear();
        schemaServiceRegistration.close();
        LOG.info("ShardHelper Closed");
    }

    /** Clear Shard Manager's internal databases.
     *
     */
    public void clear()  {
        LOG.info("clearing databases");
        dataTreeShardRegistrations.forEach(dataTreeShardReg -> dataTreeShardReg.close());
        dataTreeShardRegistrations.clear();
        shardDb.clear();
    }

    /**
     * @author jmedved
     *
     */
    public static class ShardData {
        private final DOMDataTreeIdentifier ddtId;
        private final InMemoryDOMDataTreeShard shard;
        private final DOMDataTreeProducer producer;

        private ShardData(DOMDataTreeIdentifier ddtId, InMemoryDOMDataTreeShard shard, DOMDataTreeProducer producer) {
            this.shard = shard;
            this.ddtId = ddtId;
            this.producer = producer;
        }

        private InMemoryDOMDataTreeShard getShard() {
            return shard;
        }

        public DOMDataTreeIdentifier getDOMDataTreeIdentifier() {
            return ddtId;
        }

        public DOMDataTreeProducer getProducer() {
            return producer;
        }
    }
}
