/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;

import sharding.simple.impl.ShardingSimpleProvider;

public class ShardingSimpleTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        ShardedDOMDataTree dataTreeShardingService = new ShardedDOMDataTree();

        ShardingSimpleProvider shardingSimpleProvider =
                new ShardingSimpleProvider(null, dataTreeShardingService, dataTreeShardingService);
        shardingSimpleProvider.init();
    }

}
