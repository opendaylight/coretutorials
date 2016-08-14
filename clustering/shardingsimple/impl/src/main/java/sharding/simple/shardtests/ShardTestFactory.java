/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import java.util.Collection;
import java.util.Map;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.ShardHelper;
import sharding.simple.impl.ShardHelper.ShardData;

/**
 * @author jmedved
 *
 */
public class ShardTestFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ShardTestFactory.class);

    private final ShardHelper shardHelper;
    private final DOMDataTreeService dataTreeService;

    /** Constructor for the TestFactory.
     * @param shardHelper: Reference to the ShardHelper
     * @param dataTreeService: Reference to the MD-SAL Data Tree Service
     */
    public ShardTestFactory(ShardHelper shardHelper, DOMDataTreeService dataTreeService) {
        this.shardHelper = shardHelper;
        this.dataTreeService = dataTreeService;
        LOG.info("TestFactory created.");
    }

    /** Creates new test with parameters.
     * @param numShards: number of shards to create and register
     * @param numItems: number of items in each shard
     * @param numListeners: number of listeners to create
     * @param dataStoreType: CONFIG | OPERATIONAL
     * @return: newly created ShardTest
     * @throws ShardTestException when test creation failed
     */
    public ShardTest createTest(Long numShards, Long numItems, Long numListeners,
            LogicalDatastoreType dataStoreType, Boolean preCreateTestData) throws ShardTestException {
        LOG.info("Creating ShardTest, numShards {], numItems {}, dataStoreType {}",
                numShards, numItems, dataStoreType);

        try {
            shardHelper.clear();
            verifyProducerRights();
            return new ShardTest(numShards, numItems, numListeners,
                    dataStoreType, preCreateTestData, shardHelper, dataTreeService);
        } catch (ShardTestException | ShardVerifyException e) {
            LOG.error("Exception creating test, {}", e);
            throw new ShardTestException(e.getMessage(), e.getCause());
        }
    }

    /** Verifies that we can register shards from the root.
     * @throws ShardVerifyException when shard verification failed
     */
    public void verifyProducerRights() throws ShardVerifyException {
        // Verify that we have producer rights to the root shard,
        // so that we can create sub-shards
        LOG.info("Registering shard at CONFIG data store root");

        try {
            ShardData sd = shardHelper.createAndInitShard(LogicalDatastoreType.CONFIGURATION,
                    YangInstanceIdentifier.EMPTY);
            sd.getProducer().close();
        } catch (DOMDataTreeShardingConflictException | DOMDataTreeProducerException e) {
            LOG.error("Exception verifying shard, {}", e);
            throw new ShardVerifyException(e.getMessage(), e.getCause());
        }
    }
}
