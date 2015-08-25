/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.agent.xmpp.impl;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.XmppUserAgent;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.xmpp.user.agent.Receiver;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.xmpp.user.agent.ReceiverKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmppUserAgentImpl implements DOMNotificationListener, AutoCloseable, DataTreeChangeListener<Receiver> {

    private static final Logger LOG = LoggerFactory.getLogger(XmppUserAgentImpl.class);


    private static final NodeIdentifier EVENT_SOURCE_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME,
            "node-id"));
    private static final NodeIdentifier PAYLOAD_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME,
            "payload"));


    private static final SchemaPath TOPIC_NOTIFICATION_PATH = SchemaPath.create(true, TopicNotification.QNAME);


    private final InstanceIdentifier<XmppUserAgent> identifier;

    private final Map<ReceiverKey, XmppReceiverContext> receivers = new HashMap<>();
    private final ChatManager chatManager;

    private final ListenerRegistration<XmppUserAgentImpl> notificationReg;
    private final ListenerRegistration<XmppUserAgentImpl> configurationReg;

    private XmppUserAgentImpl(final InstanceIdentifier<XmppUserAgent> id, final XmppUserAgent userAgent, final DataBroker dataBroker,
            final DOMNotificationService notificationService) {
        identifier = id;
        final ConnectionConfiguration connectionConfig = new ConnectionConfiguration(userAgent.getHost());
        try {
            final XMPPConnection connection = new XMPPTCPConnection(connectionConfig);
            connection.connect();
            connection.login(userAgent.getUsername(), userAgent.getPassword());
            chatManager = ChatManager.getInstanceFor(connection);
        } catch (final Exception e) {
            throw new IllegalStateException("Unable to connect to XMPP server", e);
        }
        notificationReg = notificationService.registerNotificationListener(this, TOPIC_NOTIFICATION_PATH);
        final InstanceIdentifier<Receiver> receiverPath = identifier.child(Receiver.class);
        final DataTreeIdentifier<Receiver> receiverConfigPath = new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, receiverPath);
        configurationReg = dataBroker.registerDataTreeChangeListener(receiverConfigPath, this);
        LOG.info("XMPP user agent initialized. id: {}", id);

    }


    static XmppUserAgentImpl create(final InstanceIdentifier<XmppUserAgent> id, final XmppUserAgent configuration, final DataBroker dataBroker,
            final DOMNotificationService notificationService) {
        return new XmppUserAgentImpl(id,configuration, dataBroker, notificationService);
    }


    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Receiver>> changed) {

        for (final DataTreeModification<Receiver> change : changed) {
            final ReceiverKey receiverKey = getReceiverKey(change.getRootPath());
            final DataObjectModification<Receiver> rootChange = change.getRootNode();
            switch (rootChange.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    createOrModifyReceiver(receiverKey, rootChange.getDataAfter());
                    break;
                case DELETE:
                    removeReceiver(receiverKey);
                    break;
            }
        }
    }

    private synchronized void createOrModifyReceiver(final ReceiverKey receiverKey, final Receiver dataAfter) {
        final XmppReceiverContext preexisting = receivers.get(receiverKey);
        if (preexisting == null) {
            final XmppReceiverContext receiver = XmppReceiverContext.create(chatManager, receiverKey);
            LOG.info("Created publishing context for receiver {}", receiverKey);
            receivers.put(receiverKey, receiver);
        }
    }

    private synchronized void removeReceiver(final ReceiverKey receiverKey) {
        final XmppReceiverContext receiver = receivers.remove(receiverKey);
        if (receiver != null) {
            LOG.debug("Removing receiver {}.", receiverKey.getJabberId());
            receiver.close();
        }
    }


    private static ReceiverKey getReceiverKey(final DataTreeIdentifier<Receiver> rootPath) {
        return rootPath.getRootIdentifier().firstKeyOf(Receiver.class, ReceiverKey.class);
    }

    @Override
    public void onNotification(final DOMNotification notification) {
        final String nodeName = notification.getBody().getChild(EVENT_SOURCE_ARG).get().getValue().toString();

        try {
            final AnyXmlNode encapData = (AnyXmlNode) notification.getBody().getChild(PAYLOAD_ARG).get();
            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.transform(encapData.getValue(), result);
            writer.flush();
            final String message = writer.toString();


            synchronized (this) {
                for (final Entry<ReceiverKey, XmppReceiverContext> receiver : receivers.entrySet()) {
                    receiver.getValue().sendMessage(message);
                }
            }
            LOG.info("Published notification for Agent {}: \nN otification {} ", nodeName, message);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void close() {
        configurationReg.close();
        notificationReg.close();
        for (final Entry<ReceiverKey, XmppReceiverContext> receiver : receivers.entrySet()) {
            receiver.getValue().close();
        }
    }

}
