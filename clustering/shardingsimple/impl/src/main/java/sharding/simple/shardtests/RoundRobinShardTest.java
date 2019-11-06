/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sharding.simple.impl.DomListBuilder;
import sharding.simple.impl.ShardHelper;
import sharding.simple.impl.ShardHelper.ShardData;

/** Implements the shard performance test.
 * @author jmedved
 *
 */
public class RoundRobinShardTest extends AbstractShardTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinShardTest.class);

    private final AtomicInteger txOk = new AtomicInteger();
    private final AtomicInteger txError = new AtomicInteger();

    RoundRobinShardTest(Long numShards, Long numItems, Long numListeners, Long opsPerTx,
            LogicalDatastoreType dataStoreType, Boolean precreateTestData, ShardHelper shardHelper,
            DOMDataTreeService dataTreeService) throws ShardTestException {

        super(numShards, numItems, numListeners, opsPerTx, dataStoreType, precreateTestData,
                shardHelper, dataTreeService);
        LOG.info("Created RoundRobinShardTest");
    }

    /** Performs a test where data items are created on fly and written
     *  round-robin into the data store.
     * @return: performance statistics from the test.
     */
    @Override
    public ShardTestStats runTest() {
        LOG.info("Running RoundRobinShardTest");

        createListAnchors();
        final List<MapEntryNode> testData = preCreateTestData();

        DOMDataTreeCursorAwareTransaction[] tx = new DOMDataTreeCursorAwareTransaction[(int) numShards];
        DOMDataTreeWriteCursor[] cursor = new DOMDataTreeWriteCursor[(int) numShards];
        int[] writeCnt = new int[(int) numShards];

        for (int s = 0; s < numShards; s++) {
            writeCnt[s] = 0;
            ShardData sd = shardData.get(s);
            tx[s] = sd.getProducer().createTransaction(false);
            cursor[s] = tx[s].createCursor(sd.getDOMDataTreeIdentifier());
            cursor[s].enter(new NodeIdentifier(InnerList.QNAME));
        }

        int txSubmitted = 0;
        int testDataIdx = 0;
        final long startTime = System.nanoTime();

        for (int i = 0; i < numItems; i++) {
            for (int s = 0; s < numShards; s++) {
                NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                        DomListBuilder.IL_NAME, (long)i);
                MapEntryNode element;
                if (preCreateTestData) {
                    element = testData.get(testDataIdx++);
                } else {
                    element = createListEntry(nodeId, s, i);
                }
                writeCnt[s]++;
                cursor[s].write(nodeId, element);

                if (writeCnt[s] == opsPerTx) {
                    // We have reached the limit of writes-per-transaction.
                    // Submit the current outstanding transaction and create
                    // a new one in its place.
                    txSubmitted++;
                    cursor[s].close();
                    tx[s].commit().addCallback(new FutureCallback<CommitInfo>() {
                        @Override
                        public void onSuccess(final CommitInfo result) {
                            txOk.incrementAndGet();
                        }

                        @Override
                        public void onFailure(final Throwable t1) {
                            LOG.error("Transaction failed, {}", t1);
                            txError.incrementAndGet();
                        }
                    }, MoreExecutors.directExecutor());

                    writeCnt[s] = 0;
                    ShardData sd = shardData.get(s);
                    tx[s] = sd.getProducer().createTransaction(false);
                    cursor[s] = tx[s].createCursor(sd.getDOMDataTreeIdentifier());
                    cursor[s].enter(new NodeIdentifier(InnerList.QNAME));
                }
            }
        }

        // Submit the last outstanding transaction even if it's empty and wait
        // for it to complete. This will flush all outstanding transactions to
        // the data store. Note that all tx submits except for the last one are
        // asynchronous.
        for (int s = 0; s < numShards; s++) {
            txSubmitted++;
            cursor[s].close();
            try {
                tx[s].commit().get();
                // txOk.incrementAndGet();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Transaction failed, {}", e);
                txError.incrementAndGet();
            }
        }

        final long endTime = System.nanoTime();
        LOG.info("RoundRobinShardTest finished");
        return new ShardTestStats(ShardTestStats.TestStatus.OK, txOk.intValue(), txError.intValue(), txSubmitted,
                (endTime - startTime) / 1000, getListenerEventsOk(), getListenerEventsFail());
    }
}
