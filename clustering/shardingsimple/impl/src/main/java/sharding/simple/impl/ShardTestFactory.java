/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.TestData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.ShardHelper.ShardData;

/**
 * @author jmedved
 *
 */
public class ShardTestFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ShardTestFactory.class);
    private static final YangInstanceIdentifier TEST_DATA_ROOT_YID =
            YangInstanceIdentifier.builder().node(TestData.QNAME).node(OuterList.QNAME).build();

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
     * @param dataStoreType: CONFIG | OPERATIONAL
     * @return: newly created ShardTest
     * @throws ShardTestException when test creation failed
     */
    public ShardTest createParametrizedTest(Long numShards, Long numItems, LogicalDatastoreType dataStoreType)
            throws ShardTestException {
        LOG.info("Creating ShardTest, numShards {], numItems {}, dataStoreType {}",
                numShards, numItems, dataStoreType);

        try {
            shardHelper.clear();
            verifyProducerRights();
            return new ShardTest(numShards, numItems, dataStoreType, shardHelper, dataTreeService);
        } catch (DOMDataTreeShardingConflictException | ShardVerifyException e) {
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

    /**
     * @author jmedved
     *
     */
    public static final class TestFactoryListener implements DOMDataTreeListener {

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> collection,
                final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> map) {
            LOG.warn("Received onDataTreeChanged {}, data: {}", collection, map);
        }

        @Override
        public void onDataTreeFailed(final Collection<DOMDataTreeListeningException> collection) {
            LOG.error("Received onDataTreeFailed {}", collection);

        }
    }

    /** Container that holds stats from a test execution.
     * @author jmedved
     *
     */
    public static class TestStats {
        private final long txOk;
        private final long txError;
        private final long txSubmitted;
        private final long execTime;

        private TestStats(int txOk, int txError, int txSubmitted, long execTime) {
            this.txOk = txOk;
            this.txError = txError;
            this.txSubmitted = txSubmitted;
            this.execTime = execTime;
        }

        public long getTxOk() {
            return txOk;
        }

        public long getTxError() {
            return txError;
        }

        public long getTxSubmitted() {
            return txSubmitted;
        }

        public long getExecTime() {
            return execTime;
        }

}

    /** Shard Test data and methods.
     * @author jmedved
     *
     */
    public static class ShardTest {
        private final List<ShardData> shardData = Lists.newArrayList();
        private final List<MapNode> testData = Lists.newArrayList();
        private final long numShards;
        private final long numItems;
        private final AtomicInteger txOk = new AtomicInteger();
        private final AtomicInteger txError = new AtomicInteger();
        private final DOMDataTreeService dataTreeService;

        private ShardTest(Long numShards, Long numItems, LogicalDatastoreType dataStoreType,
                ShardHelper shardManager, DOMDataTreeService dataTreeService)
                throws DOMDataTreeShardingConflictException {
            LOG.info("Creating ShardTest");

            this.dataTreeService = dataTreeService;
            for (Long i = (long)0; i < numShards; i++) {
                final YangInstanceIdentifier yiId =
                                TEST_DATA_ROOT_YID.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                                QName.create(OuterList.QNAME, "oid"),
                                i));
                final ShardData sd = shardManager.createAndInitShard(dataStoreType, yiId);
                shardData.add(sd);
                testData.add(DomListBuilder.buildInnerList(i, numItems));
            }

            this.numShards = numShards;
            this.numItems = numItems;
            // registerListeners();
        }

        /** Registers (eventually) one or more listeners.
         *
         */
        public void registerListeners() {
            LOG.info("Registering listeners");
            final ArrayList<DOMDataTreeIdentifier> treeIds = Lists.newArrayList();
            shardData.forEach(sd -> treeIds.add(sd.getDOMDataTreeIdentifier()));
            LOG.info("treeIds: {}", treeIds);

            try {
                dataTreeService.registerListener(new TestFactoryListener(), treeIds, false, Collections.emptyList());
            } catch (final DOMDataTreeLoopException e) {
                LOG.error("Exception processing listeners, {}", e);
                return;
            }

        }

        public TestStats executeOneByOneTest() {
            LOG.info("Executing shard test");

            MapNode mapNode = ImmutableMapNodeBuilder
                    .create()
                    .withNodeIdentifier(new NodeIdentifier(InnerList.QNAME))
                    .build();
            for (int s = 0; s < numShards; s++) {
                ShardData sd = shardData.get(s);

                final DOMDataTreeCursorAwareTransaction tx = sd.getProducer().createTransaction(false);
                final DOMDataTreeWriteCursor cursor = tx.createCursor(sd.getDOMDataTreeIdentifier());
                final YangInstanceIdentifier shardRootYid = sd.getDOMDataTreeIdentifier().getRootIdentifier();
                cursor.write(shardRootYid.node(InnerList.QNAME).getLastPathArgument(), mapNode);
                cursor.close();
                try {
                    tx.submit().checkedGet();
                } catch (TransactionCommitFailedException e) {
                    LOG.error("Failed to create container for inner list, {}", e);
                    throw new RuntimeException(e);
                }
            }

            final long startTime = System.nanoTime();
            int txSubmitted = 0;
            for (int i = 0; i < numItems; i++) {
                for (int s = 0; s < numShards; s++) {
                    ShardData sd = shardData.get(s);

                    final DOMDataTreeCursorAwareTransaction tx = sd.getProducer().createTransaction(false);
                    final DOMDataTreeWriteCursor cursor = tx.createCursor(sd.getDOMDataTreeIdentifier());
                    cursor.enter(new NodeIdentifier(InnerList.QNAME));

                    NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                            DomListBuilder.IL_NAME, i);
                    // MapEntryNode element = testData.get(s).getChild(nodeId).get();

                    final String itemStr = "Item-" + String.valueOf(s) + "-";
                    MapEntryNode element = ImmutableNodes.mapEntryBuilder()
                            .withNodeIdentifier(new NodeIdentifierWithPredicates(InnerList.QNAME,
                                    DomListBuilder.IL_NAME, i))
                            .withChild(ImmutableNodes.leafNode(DomListBuilder.IL_NAME, i))
                            .withChild(ImmutableNodes.leafNode(DomListBuilder.IL_VALUE, itemStr + String.valueOf(i)))
                            .build();
                    cursor.write(nodeId, element);
                    cursor.close();

                    txSubmitted++;
                    Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(final Void result) {
                            txOk.incrementAndGet();
                        }

                        @Override
                        public void onFailure(final Throwable t1) {
                            LOG.error("Transaction failed, {}", t1);
                            txError.incrementAndGet();
                        }
                    });
                }
            }
            final long endTime = System.nanoTime();

            return new TestStats(txOk.intValue(), txError.intValue(), txSubmitted,
                    (endTime - startTime) / 1000);
        }

            /** Executes a simple validation test.
         * @return: Statistics from the test
         */
        public TestStats executeTest() {
            LOG.info("Executing simple shard test");
            if (shardData.size() != 2) {
                LOG.info("Must have 2 shards for now");
                return null;
            }

            LOG.info("Creating transaction chain tx1 for producer shardData[0]");
            final long startTime = System.nanoTime();

            ShardData sd1 = shardData.get(0);
            final DOMDataTreeCursorAwareTransaction tx1 = sd1.getProducer().createTransaction(false);
            final DOMDataTreeWriteCursor cursor1 = tx1.createCursor(sd1.getDOMDataTreeIdentifier());
            final YangInstanceIdentifier list1Yid =
                    sd1.getDOMDataTreeIdentifier().getRootIdentifier().node(InnerList.QNAME);
            cursor1.write(list1Yid.getLastPathArgument(), testData.get(0));
            cursor1.enter(new NodeIdentifier(InnerList.QNAME));

            for ( MapEntryNode item : testData.get(0).getValue()) {
                cursor1.write(new NodeIdentifierWithPredicates(InnerList.QNAME,
                        item.getIdentifier().getKeyValues()), item);
            }
            cursor1.close();

            int txSubmitted = 0;
            txSubmitted++;
            LOG.info("Submitting transaction tx1");
            Futures.addCallback(tx1.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    txOk.incrementAndGet();
                }

                @Override
                public void onFailure(final Throwable t1) {
                    LOG.error("Transaction failed, {}", t1);
                    txError.incrementAndGet();
                }
            });

            LOG.info("Creating transaction chain tx2 for producer shardData[1]");
            ShardData sd2 = shardData.get(1);
            final DOMDataTreeCursorAwareTransaction tx2 = sd2.getProducer().createTransaction(false);
            final DOMDataTreeWriteCursor cursor2 = tx2.createCursor(sd2.getDOMDataTreeIdentifier());

            LOG.info("Writing entire list2 to Shard2");
            cursor2.write(new NodeIdentifier(InnerList.QNAME), testData.get(1));
            cursor2.close();

            LOG.info("Submitting transaction tx2");
            txSubmitted++;
            Futures.addCallback(tx2.submit(), new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    txOk.incrementAndGet();
                }

                @Override
                public void onFailure(final Throwable t2) {
                    LOG.error("Transaction failed, {}", t2);
                    txError.incrementAndGet();
                }
            });
            final long endTime = System.nanoTime();

            return new TestStats(txOk.intValue(), txError.intValue(), txSubmitted,
                    (endTime - startTime) / 1000);
        }
    }
}
