/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.impl;

import com.google.common.collect.Lists;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import sharding.simple.impl.ShardManager.ShardData;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.TestData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author jmedved
 *
 */
public class ShardingSimpleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ShardingSimpleProvider.class);

    private final ShardManager shardManager;
    private final RpcProviderRegistry rpcRegistry;
    private final DOMDataTreeService dataTreeService;

    /** Public constructor - references to MD-SAL services injected through here.
     * @param rpcRegistry: reference to MD-SAL RPC Registry
     * @param dataTreeShardingService: reference to MD-SAL Data Tree Sharding Service
     * @param dataTreeService: reference to MD-SAL Data Tree  Service
     * @param schemaService: reference to MD-SAL Schema Service
     */
    public ShardingSimpleProvider(final RpcProviderRegistry rpcRegistry,
            final DOMDataTreeShardingService dataTreeShardingService,
            final DOMDataTreeService dataTreeService,
            final SchemaService schemaService) {
        this.rpcRegistry = rpcRegistry;
        this.shardManager = new ShardManager(dataTreeShardingService, dataTreeService, schemaService);
        this.dataTreeService = dataTreeService;

        LOG.info("ShardingSimpleProvider Constructor finished");
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ShardingSimpleProvider Session Initiated");

        final YangInstanceIdentifier testDataYid =
                YangInstanceIdentifier.builder().node(TestData.QNAME).node(OuterList.QNAME).build();

        final DOMDataTreeIdentifier dtiRoot = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                                              YangInstanceIdentifier.EMPTY);

        // Verify that we have producer rights to the root shard, so that we can create sub-shards
        LOG.info("Registgering shard at CONFIG data store root");
        final ShardData sd = shardManager.createAndInitShard(dtiRoot);
        if (sd != null) {
            try {
                sd.getProducer().close();
            } catch (final DOMDataTreeProducerException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.error("Could not register root shard");
        }

        LOG.info("Registering shard at Shard1");
        final YangInstanceIdentifier shard1Yid = testDataYid.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                                                            QName.create(OuterList.QNAME, "oid"),
                                                            1));
        final DOMDataTreeIdentifier dtiShard1 = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                                                    shard1Yid);
        final ShardData sd1 = shardManager.createAndInitShard(dtiShard1);

        LOG.info("Creatinglist to write to Shard1");
        final MapNode list1 = DomListBuilder.buildInnerList(1, 10);

        LOG.info("Creating a transaction chain for the producer");
        final DOMDataTreeCursorAwareTransaction tx1 = sd1.getProducer().createTransaction(false);
        final DOMDataTreeWriteCursor cursor1 = tx1.createCursor(dtiShard1);
        final YangInstanceIdentifier list1Yid = shard1Yid.node(InnerList.QNAME);
        cursor1.write(list1Yid.getLastPathArgument(), list1);
        cursor1.enter(new NodeIdentifier(InnerList.QNAME));

        for ( MapEntryNode item : list1.getValue()) {
            cursor1.write(new NodeIdentifierWithPredicates(InnerList.QNAME, item.getIdentifier().getKeyValues()), item);
        }
        cursor1.close();

        LOG.info("Submitting transaction tx1");
        try {
            tx1.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Registering shard at Shard2");
        final YangInstanceIdentifier shard2Yid = testDataYid.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                QName.create(OuterList.QNAME, "oid"),
                2));
        final DOMDataTreeIdentifier dtiShard2 = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                                                    shard2Yid);
        final ShardData sd2 = shardManager.createAndInitShard(dtiShard2);
        final DOMDataTreeCursorAwareTransaction tx2 = sd2.getProducer().createTransaction(false);
        final DOMDataTreeWriteCursor cursor2 = tx2.createCursor(dtiShard2);

        LOG.info("Creatinglist to write to Shard2");
        final MapNode list2 = DomListBuilder.buildInnerList(2, 10);
        cursor2.write(new NodeIdentifier(InnerList.QNAME), list2);

        cursor2.close();

        LOG.info("Submitting transaction tx2");
        try {
            tx2.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new RuntimeException(e);
        }

        try {
            dataTreeService.registerListener(new TestListener(),
                    Lists.newArrayList(dtiShard1, dtiShard2), false, Collections.emptyList());
        } catch (final DOMDataTreeLoopException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ShardingSimpleProvider Closed");
        shardManager.close();
    }

    /**
     * @author jmedved
     *
     */
    public static final class TestListener implements DOMDataTreeListener {

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

}