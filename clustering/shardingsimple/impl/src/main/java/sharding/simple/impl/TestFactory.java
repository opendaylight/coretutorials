/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package sharding.simple.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.TestData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sharding.simple.impl.ShardManager.ShardData;

/**
 * @author jmedved
 *
 */
public class TestFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TestFactory.class);
    private static final YangInstanceIdentifier TEST_DATA_ROOT_YID =
            YangInstanceIdentifier.builder().node(TestData.QNAME).node(OuterList.QNAME).build();


    private final ShardManager shardManager;

    public TestFactory(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    public ShardTest createTest(Long numShards, Long numItems, LogicalDatastoreType dataStoreType) {
        LOG.info("Creating ShardTest");
        try {
            return new ShardTest(numShards, numItems, dataStoreType, shardManager);

        } catch (DOMDataTreeShardingConflictException e) {
            LOG.error("Could not create ShardTest, exception {}", e);
            return null;
        }
    }

    /**
     * @author jmedved
     *
     */
    public static class ShardTest {
        private List<ShardData> shardData;
        private List<InnerList> testData;

        private ShardTest(Long numShards, Long numItems, LogicalDatastoreType dataStoreType, ShardManager shardManager)
                throws DOMDataTreeShardingConflictException {
            shardData = new ArrayList<>();

            for (int i = 0; i < numShards; i++) {
                YangInstanceIdentifier yiId =
                        TEST_DATA_ROOT_YID.node(new NodeIdentifierWithPredicates(OuterList.QNAME,
                                QName.create(OuterList.QNAME, "oid"),
                                i));
                shardData.add(shardManager.createAndInitShard(dataStoreType, yiId));
            }

            testData = new ArrayList<>();
        }
    }
}
