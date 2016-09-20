/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestInput.DataStore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestInput.TestType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.ShardHelper;
import sharding.simple.impl.ShardHelper.ShardData;

/** Creates ShardTest instances.
 * @author jmedved
 *
 */
public class ShardTestFactory {
    /** Defines test type.
     * @author jmedved
     *
     */
    public enum ShardTestType { ROUND_ROBIN, MULTI_THREAD, SOAK_TEST, RANDOM_SHARD }

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

    /** Converts external test type to internal shard test type.
     * @param testType: Binding-aware yang-generated test type
     * @return internal ShardTestType
     * @throws ShardTestException when yang-generated test type is unknown
     */
    private ShardTestType getShardTestType(TestType testType) throws ShardTestException {
        switch (testType) {
            case MULTITHREADED:
                return ShardTestType.MULTI_THREAD;
            case ROUNDROBIN:
                return ShardTestType.ROUND_ROBIN;
            case SOAKTEST:
                return ShardTestType.SOAK_TEST;
            case RANDOMSHARD:
                return ShardTestType.RANDOM_SHARD;
            default:
                throw new ShardTestException("Invalid test type ".concat(String.valueOf(testType)));
        }
    }

    /** Converts external data store type to internal LogicalDatastoreType type.
     * @param dataStore: Binding-aware yang-generated data store type
     * @return LogicalDatastoreType (CONFIG or OPERATIONAL)
     */
    private LogicalDatastoreType getLogicalDatastoreType(DataStore dataStore) {
        return dataStore == DataStore.CONFIG ? LogicalDatastoreType.CONFIGURATION : LogicalDatastoreType.OPERATIONAL;
    }

    /** Creates new test with parameters.
     * @param input: input parameters for the test
     * @return newly created ShardTest
     * @throws ShardTestException when test creation failed
     */
    public AbstractShardTest createTest(ShardTestInput input) throws ShardTestException {
        ShardTestType testType = getShardTestType(input.getTestType());

        try {
            shardHelper.clear();
            verifyProducerRights();
            switch (testType) {
                case ROUND_ROBIN:
                    return new RoundRobinShardTest(input.getShards(), input.getDataItems(), input.getListeners(),
                            input.getPutsPerTx(), getLogicalDatastoreType(input.getDataStore()),
                            input.isPrecreateData(), shardHelper, dataTreeService);
                case MULTI_THREAD:
                    return new MultiThreadShardTest(input.getShards(), input.getDataItems(), input.getListeners(),
                            input.getPutsPerTx(), getLogicalDatastoreType(input.getDataStore()),
                            input.isPrecreateData(), shardHelper, dataTreeService);
                case SOAK_TEST:
                    return new SoakShardTest(input.getShards(), input.getDataItems(), input.getOperations(),
                            input.getListeners(), input.getPutsPerTx(), getLogicalDatastoreType(input.getDataStore()),
                            input.isPrecreateData(), shardHelper, dataTreeService);
                case RANDOM_SHARD:
                    return new RandomShardTest(input.getShards(), input.getDataItems(), input.getListeners(),
                            input.getPutsPerTx(), getLogicalDatastoreType(input.getDataStore()),
                            input.isPrecreateData(), shardHelper, dataTreeService);
                default:
                    throw new ShardTestException("Invalid test type ".concat(String.valueOf(testType)));
            }
        } catch (ShardTestException | ShardVerifyException e) {
            LOG.error("Exception creating test, {}", e);
            throw new ShardTestException(e.getMessage(), e.getCause());
        }
    }

    /** Verifies that we can register shards from the root.
     * @throws ShardVerifyException when shard verification failed
     */
    private void verifyProducerRights() throws ShardVerifyException {
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
