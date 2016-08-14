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
import java.util.List;
import java.util.concurrent.Callable;

import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.DomListBuilder;
import sharding.simple.impl.ShardHelper.ShardData;

/** Provides function for multi-threaded pushing of test data to data store.
 * @author jmedved
 *
 */
public class ShardTestCallable implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(ShardTestCallable.class);

    private final ShardData sd;
    private final int shardNum;
    private final long numItems;
    private final long opsPerTx;
    private final List<MapEntryNode> testData;

    int txSubmitted = 0;
    int txOk = 0;
    int txError = 0;

    ShardTestCallable(ShardData sd, int shardNum, long numItems, long opsPerTx, List<MapEntryNode> testData) {
        this.sd = sd;
        this.shardNum = shardNum;
        this.numItems = numItems;
        this.opsPerTx = opsPerTx;
        this.testData = testData;
        LOG.info("ShardTestCallable created");
    }

    @Override
    public Void call() throws Exception {
        int testDataIdx = 0;

        int writeCnt = 0;
        DOMDataTreeCursorAwareTransaction tx = sd.getProducer().createTransaction(false);
        DOMDataTreeWriteCursor cursor = tx.createCursor(sd.getDOMDataTreeIdentifier());
        cursor.enter(new NodeIdentifier(InnerList.QNAME));

        for (int i = 0; i < numItems; i++) {
            NodeIdentifierWithPredicates nodeId = new NodeIdentifierWithPredicates(InnerList.QNAME,
                    DomListBuilder.IL_NAME, (long)i);
            MapEntryNode element;
            if (testData != null) {
                element = testData.get(testDataIdx++);
            } else {
                element = AbstractShardTest.createListEntry(nodeId, shardNum, i);
            }
            writeCnt++;
            cursor.write(nodeId, element);

            if (writeCnt == opsPerTx) {
                // We have reached the limit of writes-per-transaction.
                // Submit the current outstanding transaction and create
                // a new one in its place.
                txSubmitted++;
                cursor.close();
                Futures.addCallback(tx.submit(), new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        txOk++;
                    }

                    @Override
                    public void onFailure(final Throwable t1) {
                        LOG.error("Transaction failed, shard {}, exception {}", shardNum, t1);
                        txError++;
                    }
                });

                writeCnt = 0;
                tx = sd.getProducer().createTransaction(false);
                cursor = tx.createCursor(sd.getDOMDataTreeIdentifier());
                cursor.enter(new NodeIdentifier(InnerList.QNAME));
            }
        }

        // Submit the last outstanding transaction even if it's empty and wait
        // for it to complete. This will flush all outstanding transactions to
        // the data store. Note that all tx submits except for the last one are
        // asynchronous.
        txSubmitted++;
        cursor.close();
        try {
            tx.submit().checkedGet();
            txOk++;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Last transaction submit failed, shard {}, exception {}", shardNum, e);
            txError++;
        }
        return null;
    }

    public int getTxSubmitted() {
        return txSubmitted;
    }

    public int getTxOk() {
        return txOk;
    }

    public int getTxError() {
        return txError;
    }

}
