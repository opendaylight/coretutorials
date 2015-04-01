/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials.agent.xmpp.impl;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.XmppUserAgents;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.XmppUserAgent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.XmppUserAgentKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmppUserAgentFactory implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(XmppUserAgentFactory.class);
    private static final InstanceIdentifier<XmppUserAgent> AGENT_PATH = InstanceIdentifier.create(XmppUserAgents.class).child(XmppUserAgent.class);

    private final ListenerRegistration<DataChangeListener> xmppAgentsConfigReg;
    private final Map<XmppUserAgentKey,XmppUserAgentImpl> publishers = new HashMap<>();
    private final DOMNotificationService notificationService;

    public XmppUserAgentFactory(final DataBroker broker,final DOMNotificationService domNotification) {
        xmppAgentsConfigReg = broker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                AGENT_PATH,
                this,
                DataBroker.DataChangeScope.SUBTREE);
        this.notificationService = domNotification;
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        final Map<InstanceIdentifier<?>, DataObject> data = change.getCreatedData();
        for (final DataObject dataObject : data.values()) {
            if (dataObject instanceof XmppUserAgent) {
                LOG.info("XMPP User agent created in configuration {}", dataObject);
                createUserAgent((XmppUserAgent) dataObject);
            }
        }
    }

    private void createUserAgent(final XmppUserAgent agent) {
        final XmppUserAgentKey id = agent.getKey();
        publishers.put(id, new XmppUserAgentImpl(agent,notificationService));
    }

    @Override
    public void close() throws Exception {
        xmppAgentsConfigReg.close();
    }
}
