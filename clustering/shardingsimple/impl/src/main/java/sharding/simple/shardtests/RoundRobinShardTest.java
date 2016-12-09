/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.shardtests;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.opendaylight.controller.cluster.sharding.DOMDataTreeShardCreationFailedException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.ShardTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.DomListBuilder;
import sharding.simple.impl.ShardFactory;
import sharding.simple.impl.ShardHelper;
import sharding.simple.impl.ShardHelper.ShardData;

/** Implements the shard performance test.
 * @author jmedved
 *
 */
public class RoundRobinShardTest extends AbstractShardTest {
    private static final Logger LOG = LoggerFactory.getLogger(RoundRobinShardTest.class);

    RoundRobinShardTest(Long numShards, Long numItems, Long numListeners, Long opsPerTx,
            LogicalDatastoreType dataStoreType, Boolean precreateTestData, ShardHelper shardHelper,
            DOMDataTreeService dataTreeService, ShardFactory shardFactory) throws ShardTestException {

        super(numShards, numItems, numListeners, opsPerTx, dataStoreType, precreateTestData,
                shardHelper, dataTreeService, shardFactory);
        LOG.info("Created RoundRobinShardTest");
    }

    /** Performs a test where data items are created on fly and written
     *  round-robin into the data store.
     * @return: performance statistics from the test.
     */
    @Override
    public ShardTestStats runTest() {
        LOG.info("Running RoundRobinShardTest");

        final List<MapEntryNode> testData = preCreateTestData();

        List<SingleShardTest> singleShardTests = new ArrayList<>();
        try {
            singleShardTests = createTestShardLayout();
        } catch (ShardTestException e) {
            e.printStackTrace();
        } catch (DOMDataTreeShardCreationFailedException e) {
            e.printStackTrace();
        } catch (DOMDataTreeProducerException e) {
            e.printStackTrace();
        } catch (DOMDataTreeShardingConflictException e) {
            e.printStackTrace();
        }

        final long startTime = System.nanoTime();

        long txOk = 0;
        long txSubmitted = 0;
        long txError = 0;
        for (int i = 0; i < numItems; i++) {
            int s = 0;
            for (SingleShardTest singleTest : singleShardTests) {
                NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                        DomListBuilder.IL_NAME, (long) i);
                MapEntryNode element;
                if (preCreateTestData) {
                    element = testData.get(i);
                } else {
                    element = createListEntry(nodeId, s, (long) i);
                }
                s++;
                singleTest.executeSingleWrite(element);
                ShardTestOutput testOutput = singleTest.getTestResults();
                txOk += testOutput.getTxOk();
                txSubmitted += testOutput.getTxSubmitted();
                txError += testOutput.getTxError();
            }
        }

        final long endTime = System.nanoTime();
        LOG.info("RoundRobinShardTest finished");

        return new ShardTestStats(ShardTestStats.TestStatus.OK, txOk, txError, txSubmitted,
                (endTime - startTime) / 1000, getListenerEventsOk(), getListenerEventsFail());
    }
}
