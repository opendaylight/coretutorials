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
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.DisJoinTopicInput;
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
 * HelloWorldEventSource is an example of event source.
 * To create own event source you need to implement {@link EventSource} interface.
 * This example shows how to implement all necessary methods.
 * 
 * For simulation of occurrence of notification HelloWorldEventSource periodically creates messages
 * and if there is an joined topic then publish notifications with created messages.
 *
 * Event source is identified by NodeKey (see method getNodeKey). Actual implementation can obtain node
 * as a constructor parameter (like as this example) or it can build own node by implementation specific
 * naming policy.
 *
 * Messages are generated in internal class (MessageGenerator). 
 * Message generator is started in constructor.
 * Messages are generated periodically in given interval and contain given text
 * (see constructor parameters Short messageGeneratePeriod and String messageText)
 *
 * There is method joinTopic, that is called by {@link EventSourceRegistry} when event topic
 * is created and HelloWorldEventSource could publish notification. Implementation of joinTopic
 * has to analyze input and compare it with HelloWorldEventSource's available notification.
 * Method joinTopic registers only topic that match with at least one available notification.
 *
 * Follow comments and docs in code, to learn more.
 *
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
    private final List<TopicId> listAcceptedTopics = new ArrayList<>();
    private final String messageText;

    public HelloWorldEventSource(DOMNotificationPublishService domPublish, Node sourceNode, Short messageGeneratePeriod, String messageText) {
        this.messageGeneratePeriod = messageGeneratePeriod;
        this.sourceNode = sourceNode;
        this.domPublish = domPublish;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.messageText = messageText;
        // internally set available notifications
        setAvailableNotifications();
        // start generator of messages
        // There will be registered Listener(s) of notification in actual event source implementation.
        startMessageGenerator();
    }

    private void startMessageGenerator(){
        // message generator is started as scheduled task
        scheduler.scheduleAtFixedRate(new MessageGenerator(sourceNode.getNodeId().getValue(), this.messageText), messageGeneratePeriod, messageGeneratePeriod, TimeUnit.SECONDS);
    }

    /*
     * Implementation of joinTopic is most important to core function of event source. 
     * Event source obtains information about created topic by this method.
     * JoinTopicInput input contains next parameters:
     *    - TopicId - it is identifier of topic (see input.getTopicId())
     *    - NotificationPattern - it contain wildcard pattern to match with available notification
     * @see org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService#joinTopic(org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput)
     */
    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(JoinTopicInput input) {

        LOG.info("Start join Topic {} {}",getSourceNodeKey().getNodeId().getValue(), input.getTopicId().getValue());

        final NotificationPattern notificationPattern = input.getNotificationPattern();

        // obtaining list of SchamePath of notifications which match with notification pattern
        final List<SchemaPath> matchingNotifications = getMatchingNotifications(notificationPattern);

        JoinTopicStatus joinTopicStatus = JoinTopicStatus.Down;
        if(Util.isNullOrEmpty(matchingNotifications) == false){
            // if there is at least one SchemaPath matched with NotificationPattern then topic is add into the list
            LOG.info("Node {} Join topic {}", sourceNode.getNodeId().getValue(), input.getTopicId().getValue());
            listAcceptedTopics.add(input.getTopicId());
            joinTopicStatus = JoinTopicStatus.Up;
        }
        final JoinTopicOutput output = new JoinTopicOutputBuilder().setStatus(joinTopicStatus).build();
        return immediateFuture(RpcResultBuilder.success(output).build());
    }

    /*
     * This method is called when event source is requested to finish your work.
     * Actual implementation will unregister listener(s) and release all other sources if any
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        this.scheduler.shutdown();
    }

    /*
     * NodeKey is identifier of event source.
     * @see org.opendaylight.controller.messagebus.spi.EventSource#getSourceNodeKey()
     */
    @Override
    public NodeKey getSourceNodeKey() {
        return sourceNode.getKey();
    }

    /*
     * Method has to return list of SchemaPath. Each SchemaPath represents one type of notification that event source can produce.
     * @see org.opendaylight.controller.messagebus.spi.EventSource#getAvailableNotifications()
     */
    @Override
    public List<SchemaPath> getAvailableNotifications() {
        return Collections.unmodifiableList(this.listSchemaPaths);
    }

    /*
     * This method internally set list of SchemaPath(s) that represents all types of notification that event source can produce.
     * In actual implementation event source can set this list same way as this example code or it can obtain it from other sources
     * (e.g. configuration parameters, device capabilities etc.)
     */
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

    /*
     * Method return list of SchemaPath matched by notificationPattern
     */
    private List<SchemaPath> getMatchingNotifications(NotificationPattern notificationPattern){
        // wildcard notification pattern is converted into regex pattern
        // notification pattern could be changed into regex syntax in the future
        final String regex = Util.wildcardToRegex(notificationPattern.getValue());

        final Pattern pattern = Pattern.compile(regex);

        return Util.selectSchemaPath(getAvailableNotifications(), pattern);
    }

    /*
     * This private class is responsible to generate messages in given interval and publish notification if an topic has been joined
     * Text of message is composed by constructor parameter String messageText and time. Time is added to simulate
     * changes of message content.
     */
    private class MessageGenerator implements Runnable {

        private final String messageText;
        private final String eventSourceIdent;

        public MessageGenerator(String EventSourceIdent, String messageText) {
            this.messageText = messageText;
            this.eventSourceIdent = EventSourceIdent;
        }

        /*
         * Method is run periodically (see method startMessageGenerator in parent class)
         * Create messages and publish notification
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            // message is generated every run of method
            String message = this.messageText + " [" + Calendar.getInstance().getTime().toString() +"]";
            LOG.debug("Sample message generated: {}",message);

            for(TopicId jointTopic : listAcceptedTopics){
                // notification is published for each accepted topic 
                // if there is no accepted topic, no notification will publish

                // notification is created by builder and contain identification of eventSource and text of message
                // SampleEventSourceNotification has been defined for this example purposes only
                // Actual implementation should define own / suitable notification
                SampleEventSourceNotificationBuilder builder = new SampleEventSourceNotificationBuilder();
                builder.setMessage(message);
                builder.setSourceId(new SourceIdentifier(this.eventSourceIdent));
                SampleEventSourceNotification notification = builder.build();

                final String topicId = jointTopic.getValue();

                // notification is encapsulated into TopicDOMNotification and publish via DOMNotificationPublisherService
                TopicDOMNotification topicNotification = createNotification(notification,this.eventSourceIdent,topicId);

                ListenableFuture<? extends Object> notifFuture;
                try {
                    notifFuture = domPublish.putNotification(topicNotification);
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

        /* 
         * Method encapsulates specific SampleEventSourceNotification into TopicDOMNotification
         * TopicDOMNotification carries next informations
         *   - TopicId
         *   - identifier of event source
         *   - SampleEventSourceNotification encapsulated into XML form (see AnyXmlNode encapsulate(...))
         */
        private TopicDOMNotification createNotification(SampleEventSourceNotification notification, String eventSourceIdent, String topicId){

            final ContainerNode topicNotification = Builders.containerBuilder()
                    .withNodeIdentifier(TOPIC_NOTIFICATION_ARG)
                    .withChild(ImmutableNodes.leafNode(TOPIC_ID_ARG, new TopicId(topicId)))
                    .withChild(ImmutableNodes.leafNode(EVENT_SOURCE_ARG, eventSourceIdent))
                    .withChild(encapsulate(notification))
                    .build();
            return new TopicDOMNotification(topicNotification);

        }

        /*
         * Result of this method is encapsulated SampleEventSourceNotification into AnyXMLNode
         * SampleEventSourceNotification is XML fragment in output
         */
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

        // Helper to create root XML element with correct namespace and attribute
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

    @Override
    public Future<RpcResult<Void>> disJoinTopic(DisJoinTopicInput input) {
        listAcceptedTopics.remove(input.getTopicId());
        return immediateFuture(RpcResultBuilder.success((Void) null).build());
    }
}
