/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials.agent.xmpp.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.XmppUserAgents;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.XmppUserAgent;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmppUserAgentFactory implements DataTreeChangeListener<XmppUserAgent>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(XmppUserAgentFactory.class);
    private static final InstanceIdentifier<XmppUserAgent> AGENT_PATH = InstanceIdentifier.create(XmppUserAgents.class)
            .child(XmppUserAgent.class);
    private static final DataTreeIdentifier<XmppUserAgent> AGENT_CONFIG_PATH = new DataTreeIdentifier<>(
            LogicalDatastoreType.CONFIGURATION, AGENT_PATH);

    private final ListenerRegistration<XmppUserAgentFactory> xmppAgentsConfigReg;
    private final Map<InstanceIdentifier<XmppUserAgent>, XmppUserAgentImpl> agents = new HashMap<>();
    private final DOMNotificationService notificationService;
    private final DataBroker dataBroker;

    public XmppUserAgentFactory(final DataBroker broker, final DOMNotificationService domNotification) {
        this.dataBroker = Preconditions.checkNotNull(broker, "broker");
        this.notificationService = Preconditions.checkNotNull(domNotification, "domNotification");
        xmppAgentsConfigReg = broker.registerDataTreeChangeListener(AGENT_CONFIG_PATH, this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<XmppUserAgent>> changed) {
        for (final DataTreeModification<XmppUserAgent> change : changed) {
            final InstanceIdentifier<XmppUserAgent> agentKey = change.getRootPath().getRootIdentifier();
            final DataObjectModification<XmppUserAgent> changeDiff = change.getRootNode();
            switch (changeDiff.getModificationType()) {
                case WRITE:
                    createOrReplace(agentKey, changeDiff.getDataAfter());
                    break;
                case DELETE:
                    removeAndClose(agentKey);
                default:
                    LOG.info("Unsupported change type {} for {}", changeDiff.getModificationType(), agentKey);
                    break;
            }
        }

    }

    private synchronized void removeAndClose(final InstanceIdentifier<XmppUserAgent> agentKey) {
        LOG.info("Removing agent {}", agentKey);
        final XmppUserAgentImpl removed = agents.remove(agentKey);
        if (removed != null) {
            removed.close();
        } else {
            LOG.warn("Agent {} was not removed.", agentKey);
        }
    }


    private synchronized void createOrReplace(final InstanceIdentifier<XmppUserAgent> agentKey,
            final XmppUserAgent configuration) {
        LOG.info("Going to create / replace agent {}", agentKey);
        final XmppUserAgentImpl previous = agents.get(agentKey);
        if (previous != null) {
            LOG.info("Previous instance of {} found. Closing it.", agentKey);
            previous.close();
        }
        try {
            final XmppUserAgentImpl newAgent =
                    XmppUserAgentImpl.create(agentKey, configuration, dataBroker, notificationService);
            agents.put(agentKey, newAgent);
        } catch (final IllegalStateException e) {
            LOG.error("Unable to create agent {} with configuration {}", agentKey, configuration, e);
        }
    }

    @Override
    public synchronized void close() throws Exception {
        xmppAgentsConfigReg.close();
        for (final Entry<InstanceIdentifier<XmppUserAgent>, XmppUserAgentImpl> agent : agents.entrySet()) {
            agent.getValue().close();
        }
    }
}
