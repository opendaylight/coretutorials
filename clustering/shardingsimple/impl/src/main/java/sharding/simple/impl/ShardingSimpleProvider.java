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
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jmedved
 *
 */
public class ShardingSimpleProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ShardingSimpleProvider.class);

    private static final int DCL_EXECUTOR_MAX_POOL_SIZE = 20;
    private static final int DCL_EXECUTOR_MAX_QUEUE_SIZE = 1000;
    private static final int DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE = 1000;
    private static final int COMMIT_MAX_QUEUE_SIZE = 1000;

    private final DataBroker dataBroker;
    private final DOMDataTreeShardingService dataTreeShardingService;
    private final DOMDataTreeService dataTreeService;

    private final List<ListenerRegistration<InMemoryDOMDataTreeShard>> dataTreeShardRegistrations = new ArrayList<>();

    /** Public constructor - references to MD-SAL services injected through here.
     * @param dataBroker:
     * @param dataTreeShardingService:
     * @param dataTreeService:
     */
    public ShardingSimpleProvider(final DataBroker dataBroker,
                                  final DOMDataTreeShardingService dataTreeShardingService,
                                  final DOMDataTreeService dataTreeService) {
        this.dataBroker = dataBroker;
        this.dataTreeShardingService = dataTreeShardingService;
        this.dataTreeService = dataTreeService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("ShardingSimpleProvider Session Initiated");

        YangInstanceIdentifier testDataYid =
                YangInstanceIdentifier.builder().node(TestData.QNAME).node(OuterList.QNAME).build();

        DOMDataTreeIdentifier dtiRoot = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                                              YangInstanceIdentifier.EMPTY);

        // Verify that we have producer rights to the root shard, so that we can create sub-shards
        LOG.info("Registgering shard at CONFIG data store root");
        DOMDataTreeProducer producer = createAndInitShard(dtiRoot,
                                                          DCL_EXECUTOR_MAX_POOL_SIZE,
                                                          DCL_EXECUTOR_MAX_QUEUE_SIZE,
                                                          DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE,
                                                          COMMIT_MAX_QUEUE_SIZE);
        if (producer != null) {
            try {
                producer.close();
            } catch (DOMDataTreeProducerException e) {
                LOG.error("Exception closing root producer: {}", e);
            }
        } else {
            LOG.error("Could not register root shard");
        }

        LOG.info("Registering shard at Shard1");
        YangInstanceIdentifier shard1Yid = testDataYid.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                                                            QName.create(OuterList.QNAME, "oid"),
                                                            1));
        DOMDataTreeIdentifier dtiShard1 = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                                                    shard1Yid);
        DOMDataTreeProducer producerShard1 = createAndInitShard(dtiShard1,
                                                                DCL_EXECUTOR_MAX_POOL_SIZE,
                                                                DCL_EXECUTOR_MAX_QUEUE_SIZE,
                                                                DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE,
                                                                COMMIT_MAX_QUEUE_SIZE);

        LOG.info("Registering shard at Shard2");
        YangInstanceIdentifier shard2Yid = testDataYid.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                QName.create(OuterList.QNAME, "oid"),
                2));
        DOMDataTreeIdentifier dtiShard2 = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                                                    shard2Yid);
        DOMDataTreeProducer producerShard2 = createAndInitShard(dtiShard2,
                                                                DCL_EXECUTOR_MAX_POOL_SIZE,
                                                                DCL_EXECUTOR_MAX_QUEUE_SIZE,
                                                                DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE,
                                                                COMMIT_MAX_QUEUE_SIZE);

        LOG.info("Creatinglist to write to Shard1");
        MapNode list1 = DomListBuilder.buildInnerList(1, 10);

        /*
        LOG.info("Creating a transaction chain for the producer");
        DOMDataTreeCursorAwareTransaction tx = producerShard1.createTransaction(false);
        DOMDataTreeWriteCursor cursor = tx.createCursor(dtiShard1);
        YangInstanceIdentifier list1Yid = shard1Yid.node(InnerList.QNAME);
        cursor.write(list1Yid.getLastPathArgument(), list1);
        cursor.close();
        LOG.info("Submitting transaction");
        tx.submit();
        */
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("ShardingSimpleProvider Closed");

        for (ListenerRegistration<InMemoryDOMDataTreeShard> dataTreeShardReg : dataTreeShardRegistrations) {
            dataTreeShardReg.close();
        }
    }

    /** Creates a shard, registers it with dataTreeShardingService and
     * creates a producer for the shard.
     *
     * @param dti: DOM Data Tree identifier for the root of the shard
     * @param dclExecutorMaxPoolSize:
     * @param dclExecutorMaxQueueSize:
     * @param dcListenerMaxQueueSize:
     * @param commitMaxQueueSize:
     * @return: producer created for the shard
     */
    private DOMDataTreeProducer createAndInitShard(DOMDataTreeIdentifier dti,
                                                   final int dclExecutorMaxPoolSize,
                                                   final int dclExecutorMaxQueueSize,
                                                   final int dcListenerMaxQueueSize,
                                                   final int commitMaxQueueSize) {

        ExecutorService configRootShardExecutor =
                SpecialExecutors.newBlockingBoundedFastThreadPool(dclExecutorMaxPoolSize,
                                                                  dclExecutorMaxQueueSize,
                                                                  dti.getDatastoreType() + "RootShard-DCL");
        InMemoryDOMDataTreeShard shard =
                InMemoryDOMDataTreeShard.create(dti,
                                                configRootShardExecutor,
                                                dcListenerMaxQueueSize,
                                                commitMaxQueueSize);

        DOMDataTreeProducer producer = dataTreeService.createProducer(Collections.singletonList(dti));

        try {
            ListenerRegistration<InMemoryDOMDataTreeShard> dataTreeShardReg =
                    dataTreeShardingService.registerDataTreeShard(dti, shard, producer);
            dataTreeShardRegistrations.add(dataTreeShardReg);
        } catch (DOMDataTreeShardingConflictException e) {
            LOG.error("Exception in registering shard with dataTreeShardingService, {}", e);
            return null;
        }

        return producer;
    }
}