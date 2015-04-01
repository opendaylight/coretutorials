/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.agent.xmpp.impl;

import com.google.common.base.Throwables;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.XmppUserAgent;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmppUserAgentImpl implements DOMNotificationListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(XmppUserAgentImpl.class);

    private static final NodeIdentifier EVENT_SOURCE_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "node-id"));
    private static final NodeIdentifier PAYLOAD_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "payload"));

    private final String id;
    private final PseudoPubSub xmppClient;
    private final Set<String> nodes = new HashSet<>();
    private final ListenerRegistration<XmppUserAgentImpl> listenerReg;


    public XmppUserAgentImpl(final XmppUserAgent userAgent, final DOMNotificationService notificationService) {
        this.id = userAgent.getAgentId();
        final String username = userAgent.getUsername();
        final String password = userAgent.getPassword();


        try {
            xmppClient = new PseudoPubSub(userAgent.getIpAddress(), userAgent.getPort(), username, password);
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
        listenerReg = notificationService.registerNotificationListener(this,SchemaPath.create(true, TopicNotification.QNAME));

        LOG.info("XMPP user agent initialized. id: {}", id);

    }


    @Override
    public void onNotification(final DOMNotification notification) {
        // We extract node-id from topic notification
        final String nodeName = notification.getBody().getChild(EVENT_SOURCE_ARG).get().getValue().toString();

        synchronized(this) {
            if (nodes.contains(nodeName) == Boolean.FALSE) {
                try {
                    xmppClient.createPublisher(nodeName);
                    nodes.add(nodeName);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            final AnyXmlNode encapData = (AnyXmlNode) notification.getBody().getChild(PAYLOAD_ARG).get();

            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.transform(encapData.getValue(), result);
            writer.flush();
            final String message = writer.toString();


            xmppClient.publishMessage(nodeName, message);
            LOG.info("Published notification for Agent {}: \nNotification {} ", nodeName, message);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        listenerReg.close();
    }

}
