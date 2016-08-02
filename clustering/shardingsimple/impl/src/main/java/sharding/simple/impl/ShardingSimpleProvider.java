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
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteCursor;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.TestData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

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

        YangInstanceIdentifier shard1Yid = testDataYid.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                                                            QName.create(OuterList.QNAME, "oid"),
                                                            1));

        DOMDataTreeIdentifier dti = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                                              YangInstanceIdentifier.EMPTY);

        LOG.info("Registgering shard at CONFIG data store root");
        DOMDataTreeProducer producer = createAndInitShard(dti,
                                                          DCL_EXECUTOR_MAX_POOL_SIZE,
                                                          DCL_EXECUTOR_MAX_QUEUE_SIZE,
                                                          DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE,
                                                          COMMIT_MAX_QUEUE_SIZE);

        LOG.info("Creating a transaction chain for the producer");
        DOMDataTreeCursorAwareTransaction tx = producer.createTransaction(false);
        DOMDataTreeWriteCursor cursor = tx.createCursor(dti);
        // cursor.write(child, data);
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