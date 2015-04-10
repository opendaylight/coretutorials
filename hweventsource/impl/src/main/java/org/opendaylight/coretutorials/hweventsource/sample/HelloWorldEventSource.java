/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.hweventsource.sample;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.api.rev150408.SampleEventSourceNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.api.rev150408.SampleEventSourceNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.api.rev150408.SourceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author madamjak
 *
 */
public class HelloWorldEventSource implements EventSource {

    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldEventSource.class);

    public static final QName sample_notification_QNAME = org.opendaylight.yangtools.yang.common.QName.cachedReference(org.opendaylight.yangtools.yang.common.QName.create("urn:cisco:params:xml:ns:yang:messagebus:sample","2015-03-16","sample-notification"));
    public static final String XMLNS_ATTRIBUTE_KEY = "xmlns";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    private static final NodeIdentifier TOPIC_NOTIFICATION_ARG = new NodeIdentifier(TopicNotification.QNAME);
    private static final NodeIdentifier EVENT_SOURCE_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "node-id"));
    private static final NodeIdentifier TOPIC_ID_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "topic-id"));
    private static final NodeIdentifier PAYLOAD_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "payload"));

    private final Short messageGeneratePeriod;
    private final Node sourceNode;
    private final ScheduledExecutorService scheduler;
    private final DOMNotificationPublishService domPublish;
    private final List<SchemaPath> listSchemaPaths = new ArrayList<>();
    private final List<JoinTopicInput> listAcceptedTopics = new ArrayList<>();
    private final String messageText;
    
    public HelloWorldEventSource(DOMNotificationPublishService domPublish, Node sourceNode, Short messageGeneratePeriod, String messageText) {
        this.messageGeneratePeriod = messageGeneratePeriod;
        this.sourceNode = sourceNode;
        this.domPublish = domPublish;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.messageText = messageText;
        setAvailableNotifications();
        startMessageGenerator();
    }

    private void startMessageGenerator(){
        scheduler.scheduleAtFixedRate(new MessageGenerator(sourceNode.getNodeId().getValue(), this.messageText), messageGeneratePeriod, messageGeneratePeriod, TimeUnit.SECONDS);
    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(JoinTopicInput input) {
        LOG.info("Start join Topic {} {}",getSourceNodeKey().getNodeId().getValue(), input.getTopicId().getValue());
        final NotificationPattern notificationPattern = input.getNotificationPattern();
        final List<SchemaPath> matchingNotifications = getMatchingNotifications(notificationPattern);
        JoinTopicStatus joinTopicStatus = JoinTopicStatus.Down;
        if(Util.isNullOrEmpty(matchingNotifications) == false){
            LOG.info("Node {} Join topic {}", sourceNode.getNodeId().getValue(), input.getTopicId().getValue());
            listAcceptedTopics.add(input);
            joinTopicStatus = JoinTopicStatus.Up;
        }
        final JoinTopicOutput output = new JoinTopicOutputBuilder().setStatus(joinTopicStatus).build();
        return immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public void close() throws Exception {
        this.scheduler.shutdown();
    }

    @Override
    public NodeKey getSourceNodeKey() {
        return sourceNode.getKey();
    }

    Node getSourceNode(){
        return sourceNode;
    }

    @Override
    public List<SchemaPath> getAvailableNotifications() {
        return Collections.unmodifiableList(this.listSchemaPaths);
    }

    private void setAvailableNotifications(){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(2015, 4, 8, 0, 0, 0);
        Date revisionDate = cal.getTime();

        URI uriSample = null;
        URI uriTest = null;
        try {
            uriSample = new URI("urn:opendaylight:coretutorials:hweventsource:sample:notification");
            uriTest = new URI("urn:opendaylight:coretutorials:hweventsource:test:notification");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Bad URI for notification", e);
        }

        QName qnSample = QName.create(uriSample,revisionDate,"sample-message");
        QName qnTest = QName.create(uriTest,revisionDate,"sample-message");

        SchemaPath spSample = SchemaPath.create(true, qnSample);
        SchemaPath spTest = SchemaPath.create(true, qnTest);

        listSchemaPaths.add(spSample);
        listSchemaPaths.add(spTest);
    }
    private List<SchemaPath> getMatchingNotifications(NotificationPattern notificationPattern){
        // FIXME: default language should already be regex
        final String regex = Util.wildcardToRegex(notificationPattern.getValue());

        final Pattern pattern = Pattern.compile(regex);
        return Util.expandQname(getAvailableNotifications(), pattern);
    }

    private class MessageGenerator implements Runnable {

        private final String messageText;
        private final String eventSourceIdent;

        public MessageGenerator(String EventSourceIdent, String messageText) {
            this.messageText = messageText;
            this.eventSourceIdent = EventSourceIdent;
        }

        @Override
        public void run() {
            String message = this.messageText + " [" + Calendar.getInstance().getTime().toString() +"]";
            LOG.debug("Sample message generated: {}",message);

            for(JoinTopicInput jointTopicInput : listAcceptedTopics){
                SampleEventSourceNotificationBuilder builder = new SampleEventSourceNotificationBuilder();
                builder.setMessage(message);
                builder.setSourceId(new SourceIdentifier(this.eventSourceIdent));
                SampleEventSourceNotification notification = builder.build();

                final String topicId = jointTopicInput.getTopicId().getValue();
                ContainerNode cn = createNotification(notification,this.eventSourceIdent,topicId);

                ListenableFuture<? extends Object> notifFuture;
                try {
                    notifFuture = domPublish.putNotification(new TopicDOMNotification(cn));
                    Futures.addCallback(notifFuture, new FutureCallback<Object>(){

                        @Override
                        public void onSuccess(Object result) {
                             LOG.info("Sample message published for topic [TopicId: {}]",topicId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                             LOG.error("Sample message has not published for topic [TopicId: {}], Exception: {}",topicId,t);
                        }
                    });
                } catch (InterruptedException e) {
                    LOG.error("Sample message has not published for topic [TopicId: {}], Exception: {}",topicId,e);
                }

            }
        }

        private ContainerNode createNotification(SampleEventSourceNotification notification, String eventSourceIdent, String topicId){

            final ContainerNode topicNotification = Builders.containerBuilder()
                    .withNodeIdentifier(TOPIC_NOTIFICATION_ARG)
                    .withChild(ImmutableNodes.leafNode(TOPIC_ID_ARG, new TopicId(topicId)))
                    .withChild(ImmutableNodes.leafNode(EVENT_SOURCE_ARG, eventSourceIdent))
                    .withChild(encapsulate(notification))
                    .build();
            return topicNotification;

        }

        private AnyXmlNode encapsulate(SampleEventSourceNotification notification){

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;

            try {
                docBuilder = docFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Can not create XML DocumentBuilder");
            }

            Document doc = docBuilder.newDocument();

            final Optional<String> namespace = Optional.of(PAYLOAD_ARG.getNodeType().getNamespace().toString());
            final Element rootElement = createElement(doc , "payload", namespace);

            final Element notifElement = doc.createElement("SampleEventSourceNotification");
            rootElement.appendChild(notifElement);

            final Element sourceElement = doc.createElement("Source");
            sourceElement.appendChild(doc.createTextNode(notification.getSourceId().getValue()));
            notifElement.appendChild(sourceElement);

            final Element messageElement = doc.createElement("Message");
            messageElement.appendChild(doc.createTextNode(notification.getMessage()));
            notifElement.appendChild(messageElement);

            return Builders.anyXmlBuilder().withNodeIdentifier(PAYLOAD_ARG)
                         .withValue(new DOMSource(rootElement))
                         .build();

        }

        private Element createElement(final Document document, final String qName, final Optional<String> namespaceURI) {
            if(namespaceURI.isPresent()) {
                final Element element = document.createElementNS(namespaceURI.get(), qName);
                String name = XMLNS_ATTRIBUTE_KEY;
                if(element.getPrefix() != null) {
                    name += ":" + element.getPrefix();
                }
                element.setAttributeNS(XMLNS_URI, name, namespaceURI.get());
                return element;
            }
            return document.createElement(qName);
        }
    }
}
