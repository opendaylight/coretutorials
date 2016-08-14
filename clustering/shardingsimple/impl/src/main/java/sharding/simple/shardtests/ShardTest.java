/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.TestData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.DomListBuilder;
import sharding.simple.impl.ShardHelper;
import sharding.simple.impl.ShardHelper.ShardData;

/**
 * @author jmedved
 *
 */
public class ShardTest implements AutoCloseable {
    static final YangInstanceIdentifier TEST_DATA_ROOT_YID =
            YangInstanceIdentifier.builder().node(TestData.QNAME).node(OuterList.QNAME).build();

    private static final Logger LOG = LoggerFactory.getLogger(ShardTest.class);

    private final List<ShardData> shardData = Lists.newArrayList();
    private final List<ListenerRegistration<ShardTestListener>> testListenerRegs = Lists.newArrayList();
    private ListenerRegistration<ValidationListener> validationListenerReg = null;

    private final long numShards;
    private final long numItems;
    private final long opsPerTx;
    private final Boolean preCreateTestData;
    private final AtomicInteger txOk = new AtomicInteger();
    private final AtomicInteger txError = new AtomicInteger();
    private final DOMDataTreeService dataTreeService;

    /** Constructor for the ShardTest class.
     * @param numShards: number of shards to use in the test
     * @param numItems: number of data items to store
     * @param dataStoreType: CONFIG or OPERATIONAL
     * @param shardHelper: reference to the Shard Helper
     * @param dataTreeService: reference to the MD-SAL dataTreeService
     * @throws ShardTestException when shards or data tree listeners could not be created/registered
     */
    ShardTest(Long numShards, Long numItems, Long numListeners, Long opsPerTx, LogicalDatastoreType dataStoreType,
            Boolean precreateTestData, ShardHelper shardHelper, DOMDataTreeService dataTreeService)
            throws ShardTestException {
        LOG.info("Creating ShardTest");

        this.dataTreeService = dataTreeService;
        this.numShards = numShards;
        this.numItems = numItems;
        this.preCreateTestData = precreateTestData;
        this.opsPerTx = opsPerTx;

        // Create the specified number of shards/producers and register them
        // with MD-SAL
        try {
            for (Long i = (long)0; i < numShards; i++) {
                final YangInstanceIdentifier yiId =
                                TEST_DATA_ROOT_YID.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                                QName.create(OuterList.QNAME, "oid"),
                                i));
                final ShardData sd = shardHelper.createAndInitShard(dataStoreType, yiId);
                shardData.add(sd);
            }
        } catch (DOMDataTreeShardingConflictException e) {
            LOG.error("Failed to create shard, exception {}", e);
            throw new ShardTestException(e.getMessage(), e.getCause());
        }

        // Create the specified number of test listeners and register them
        // with MD-SAL
        try {
            final ArrayList<DOMDataTreeIdentifier> treeIds = Lists.newArrayList();
            shardData.forEach(sd -> treeIds.add(sd.getDOMDataTreeIdentifier()));
            for (long i = 0; i < numListeners; i++) {
                testListenerRegs.add(dataTreeService.registerListener(new ShardTestListener(),
                        treeIds, false, Collections.emptyList()));
            }
        } catch (DOMDataTreeLoopException e) {
            LOG.error("Failed to register a test listener, exception {}", e);
            throw new ShardTestException(e.getMessage(), e.getCause());
        }
    }

    /** Creates a root "anchor" node (actually an InnerList hanging off an
     *  outer list item) in each shard.
     *
     */
    private void createListAnchors() {
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
    }

    private static MapEntryNode createListEntry(NodeIdentifierWithPredicates nodeId, int shardIndex, int elementIndex) {
        return ImmutableNodes.mapEntryBuilder()
                .withNodeIdentifier(nodeId)
                .withChild(ImmutableNodes.leafNode(DomListBuilder.IL_NAME, (long)elementIndex))
                .withChild(ImmutableNodes.leafNode(DomListBuilder.IL_VALUE,
                        "Item-" + String.valueOf(shardIndex) + "-" + String.valueOf(elementIndex)))
                .build();
    }

    private List<MapEntryNode> preCreateTestData() {
        final List<MapEntryNode> testData = Lists.newArrayList();
        for (int i = 0; i < numItems; i++) {
            for (int s = 0; s < numShards; s++) {
                NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                        DomListBuilder.IL_NAME, (long)i);
                testData.add(createListEntry(nodeId, s, i));
            }
        }
        return testData;
    }

    /** Registers the validation listener with MD-SAL.
     * @throws DOMDataTreeLoopException when registration fails
     *
     */
    public void registerValidationListener() throws DOMDataTreeLoopException {
        LOG.info("Registering validation listener");
        if (validationListenerReg == null) {
            final ArrayList<DOMDataTreeIdentifier> treeIds = Lists.newArrayList();
            shardData.forEach(sd -> treeIds.add(sd.getDOMDataTreeIdentifier()));
            validationListenerReg = dataTreeService.registerListener(new ValidationListener(),
                    treeIds, false, Collections.emptyList());
        } else {
            LOG.warn("Validation listener already registered");
        }
    }

    @Override
    public void close() throws Exception {
        LOG.info("Closing ShardTest");
        testListenerRegs.forEach( lReg -> lReg.close());
        validationListenerReg.close();
    }

    /** Performs a test where data items are created on fly and written
     *  round-robin into the data store.
     * @return: performance statistics from the test.
     */
    public ShardTestStats runRoundRobinTest() {
        LOG.info("Running runRoundRobinTest");

        createListAnchors();

        final List<MapEntryNode> testData;
        if (preCreateTestData) {
            LOG.info("Pre-creating test data...");
            testData = preCreateTestData();
            LOG.info("Done. {} elements created.", testData.size());
        } else {
            testData = null;
        }

        int txSubmitted = 0;
        int testDataIdx = 0;
        final Semaphore semaphore = new Semaphore((int) (numItems * numShards));
        final long startTime = System.nanoTime();

        for (int i = 0; i < numItems; i++) {
            for (int s = 0; s < numShards; s++) {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    LOG.error("Failed to acquire semaphore, {}", e);
                }
                ShardData sd = shardData.get(s);

                final DOMDataTreeCursorAwareTransaction tx = sd.getProducer().createTransaction(false);
                final DOMDataTreeWriteCursor cursor = tx.createCursor(sd.getDOMDataTreeIdentifier());
                cursor.enter(new NodeIdentifier(InnerList.QNAME));

                NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                        DomListBuilder.IL_NAME, (long)i);
                MapEntryNode element;
                if (preCreateTestData) {
                    element = testData.get(testDataIdx++);
                } else {
                    element = createListEntry(nodeId, s, i);
                }

                txSubmitted++;
                cursor.write(nodeId, element);
                cursor.close();
                Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        // txOk.incrementAndGet();
                        semaphore.release();
                    }

                    @Override
                    public void onFailure(final Throwable t1) {
                        LOG.error("Transaction failed, {}", t1);
                        txError.incrementAndGet();
                        semaphore.release();
                    }
                });
            }
        }

        LOG.info("waiting for tx submits to finish...");
        try {
            semaphore.acquire((int) (numItems * numShards));
        } catch (InterruptedException e) {
            LOG.error("Failed to acquire semaphore, {}", e);
        }
        final long endTime = System.nanoTime();

        int listenerEventsOk = 0;
        int listenerEventsFail = 0;
        for ( ListenerRegistration<ShardTestListener> lreg : testListenerRegs) {
            listenerEventsOk += lreg.getInstance().getDataTreeEventsOk();
            listenerEventsFail += lreg.getInstance().getDataTreeEventsFail();
        }

        LOG.info("runRoundRobinTest finished");
        return new ShardTestStats(txOk.intValue(), txError.intValue(), txSubmitted,
                (endTime - startTime) / 1000, listenerEventsOk, listenerEventsFail);
    }

    public ShardTestStats runMultiThreadedTest() {
        LOG.info("Running runMultiThreadedTest");

        LOG.info("runMultiThreadedTest finished");
        return null;
    }

}
