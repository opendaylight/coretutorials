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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.DomListBuilder;
import sharding.simple.impl.ShardHelper;
import sharding.simple.impl.ShardHelper.ShardData;

public class ShardTest implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ShardTest.class);
    private static final YangInstanceIdentifier TEST_DATA_ROOT_YID =
            YangInstanceIdentifier.builder().node(TestData.QNAME).node(OuterList.QNAME).build();

    private final List<ShardData> shardData = Lists.newArrayList();
    private final List<ListenerRegistration<TestListener>> testListenerRegs = Lists.newArrayList();
    private ListenerRegistration<ValidationListener> validationListenerReg = null;

    private final long numShards;
    private final long numItems;
    private final long numListeners;
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
    ShardTest(Long numShards, Long numItems, Long numListeners, LogicalDatastoreType dataStoreType,
            ShardHelper shardHelper, DOMDataTreeService dataTreeService)
            throws ShardTestException {
        LOG.info("Creating ShardTest");

        this.dataTreeService = dataTreeService;
        this.numShards = numShards;
        this.numItems = numItems;
        this.numListeners = numListeners;

        // Create the specified number of shards/producers and register them with MD-SAL
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

        // Create the specified number of listeners and register them with MD-SAL
        try {
            final ArrayList<DOMDataTreeIdentifier> treeIds = Lists.newArrayList();
            shardData.forEach(sd -> treeIds.add(sd.getDOMDataTreeIdentifier()));
            for (long i = 0; i < numListeners; i++) {
                testListenerRegs.add(dataTreeService.registerListener(new TestListener(),
                        treeIds, false, Collections.emptyList()));
            }
        } catch (DOMDataTreeLoopException e) {
            LOG.error("Failed to register listener, exception {}", e);
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

    /**
     * @author jmedved
     *
     */
    private static final class TestListener implements DOMDataTreeListener {
        private Long dataTreeEventsOk = (long)0;
        private Long dataTreeEventsFail = (long)0;

        @Override
        public void onDataTreeChanged(final Collection<DataTreeCandidate> collection,
                final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> map) {
            dataTreeEventsOk++;
        }

        @Override
        public void onDataTreeFailed(final Collection<DOMDataTreeListeningException> collection) {
            dataTreeEventsFail++;
        }
    }

    /**
     * @author jmedved
     *
     */
    private static final class ValidationListener implements DOMDataTreeListener {

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

    /** Registers (eventually) one or more listeners.
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
        testListenerRegs.forEach( lReg -> lReg.close());
        validationListenerReg.close();
    }

    public ShardTestStats runRoundRobinTest() {
        LOG.info("Executing shard test");

        createListAnchors();

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

        return new ShardTestStats(txOk.intValue(), txError.intValue(), txSubmitted,
                (endTime - startTime) / 1000);
    }

    /** Verifies the very basic sharding function: writing data into
     *  two shards.
     * @return: Statistics from the test
     */
    public ShardTestStats verifyBaselineFunctionality() {
        LOG.info("Creating transaction chain tx1 for producer shardData[0]");
        final long startTime = System.nanoTime();

        ShardData sd1 = shardData.get(0);
        final DOMDataTreeCursorAwareTransaction tx1 = sd1.getProducer().createTransaction(false);
        final DOMDataTreeWriteCursor cursor1 = tx1.createCursor(sd1.getDOMDataTreeIdentifier());
        final YangInstanceIdentifier list1Yid =
                sd1.getDOMDataTreeIdentifier().getRootIdentifier().node(InnerList.QNAME);
        cursor1.write(list1Yid.getLastPathArgument(), DomListBuilder.buildInnerList((long)2, (long)10));
        cursor1.enter(new NodeIdentifier(InnerList.QNAME));
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
        cursor2.write(new NodeIdentifier(InnerList.QNAME), DomListBuilder.buildInnerList((long)2, (long)10));
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

        return new ShardTestStats(txOk.intValue(), txError.intValue(), txSubmitted,
                (endTime - startTime) / 1000);
    }
}
