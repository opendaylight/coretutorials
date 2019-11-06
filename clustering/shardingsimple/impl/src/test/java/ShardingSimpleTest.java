/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import sharding.simple.impl.ShardingSimpleProvider;

public class ShardingSimpleTest {

    // TODO add models to test resources, change the path
    private static final String DATASTORE_TEST_YANG = "/shardingsimple.yang";

    MockSchemaService schemaService = new MockSchemaService();

    @Before
    public void setUp() {
        schemaService.changeSchema(createTestContext());
    }

    @Test
    public void test() {
        final ShardedDOMDataTree dataTreeShardingService = new ShardedDOMDataTree();

        final RpcProviderRegistry rpcRegistry = new RpcProviderRegistryMock();

        final ShardingSimpleProvider shardingSimpleProvider =
                new ShardingSimpleProvider(rpcRegistry, dataTreeShardingService, dataTreeShardingService,
                        schemaService);
        shardingSimpleProvider.init();
    }

    public static SchemaContext createTestContext() {
        return YangParserTestUtils.parseYangResources( ShardingSimpleTest.class, DATASTORE_TEST_YANG);
    }

    public static final class MockSchemaService implements DOMSchemaService, SchemaContextProvider {

        private SchemaContext schemaContext;

        ListenerRegistry<SchemaContextListener> listeners = ListenerRegistry.create();

        @Override
        public synchronized SchemaContext getGlobalContext() {
            return schemaContext;
        }

        @Override
        public synchronized SchemaContext getSessionContext() {
            return schemaContext;
        }

        @Override
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
                final SchemaContextListener listener) {
            return listeners.register(listener);
        }

        @Override
        public synchronized SchemaContext getSchemaContext() {
            return schemaContext;
        }

        public synchronized void changeSchema(final SchemaContext newContext) {
            schemaContext = newContext;
            for (final ListenerRegistration<SchemaContextListener> listener : listeners) {
                listener.getInstance().onGlobalContextUpdated(schemaContext);
            }
        }

        @Override
        public ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
            return ImmutableClassToInstanceMap.of();
        }
    }

}
