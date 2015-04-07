/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.hweventsource.sample;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.sample.event.source.notification.rev150318.SampleEventSourceNotification;
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

    private static final NodeIdentifier TOPIC_NOTIFICATION_ARG = new NodeIdentifier(TopicNotification.QNAME);
    private static final NodeIdentifier EVENT_SOURCE_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "node-id"));
    private static final NodeIdentifier TOPIC_ID_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "topic-id"));
//    private static final NodeIdentifier PAYLOAD_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "payload"));


    private static final String message_prefix = "Hello World ";
    private final Short messageGeneratePeriod;
    private final Node sourceNode;
    private final ScheduledExecutorService scheduler;
    private final DOMNotificationPublishService domPublish;
    private final List<SchemaPath> listSchemaPaths = new ArrayList<>();
    private final List<JoinTopicInput> listAcceptedTopics = new ArrayList<>();

    public HelloWorldEventSource(DOMNotificationPublishService domPublish, Node sourceNode, Short messageGeneratePeriod) {
        this.messageGeneratePeriod = messageGeneratePeriod;
        this.sourceNode = sourceNode;
        this.domPublish = domPublish;
        this.scheduler = Executors.newScheduledThreadPool(1);
        setAvailableNotifications();
        startMessageGenerator();
    }

    private void startMessageGenerator(){
        scheduler.scheduleAtFixedRate(new MessageGenerator(sourceNode.getNodeId().getValue()), messageGeneratePeriod, messageGeneratePeriod, TimeUnit.SECONDS);
    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(JoinTopicInput input) {

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
        QName qn = QName.create(SampleEventSourceNotification.QNAME, "sample-message");
        SchemaPath sp = SchemaPath.create(true, qn);
        listSchemaPaths.add(sp);
    }
    private List<SchemaPath> getMatchingNotifications(NotificationPattern notificationPattern){
        // FIXME: default language should already be regex
        final String regex = Util.wildcardToRegex(notificationPattern.getValue());

        final Pattern pattern = Pattern.compile(regex);
        return Util.expandQname(getAvailableNotifications(), pattern);
    }

    private class MessageGenerator implements Runnable {

        private final String messagePrefix;
        private final String eventSourceIdent;

        public MessageGenerator(String EventSourceIdent) {
            this.messagePrefix = message_prefix + EventSourceIdent;
            this.eventSourceIdent = EventSourceIdent;
        }

        @Override
        public void run() {
            String message = this.messagePrefix + " [" + Calendar.getInstance().getTime().toString() +"]";
            LOG.info("Sample message generated: {}",message);
//            SampleEventSourceNotificationBuilder builder = new SampleEventSourceNotificationBuilder();
//            builder.setMessage(message);
//            builder.setSourceId(new SourceIdentifier(this.EventSourceIdent));
//            SampleEventSourceNotification notification = builder.build();
            //TODO: write SampleEventSourceNotification serializer and use it in createNotification
            for(JoinTopicInput jointTopicInput : listAcceptedTopics){
                final String topicId = jointTopicInput.getTopicId().getValue();
                ContainerNode cn = createNotification(message,this.eventSourceIdent,topicId);
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

        private ContainerNode createNotification(String message, String eventSourceIdent, String topicId){
            final QName payloadQname = QName.create("payload");
            final QName nodeQname = QName.create(eventSourceIdent);
            final Map<QName, String> map = new HashMap<>();
            map.put(payloadQname, message);
            final AnyXmlNode anyXmlNode = Builders.anyXmlBuilder()
                    .withNodeIdentifier(new NodeIdentifier(nodeQname))
                    .withAttributes(Collections.unmodifiableMap(map))
                    .build();
            final ContainerNode topicNotification = Builders.containerBuilder()
                    .withNodeIdentifier(TOPIC_NOTIFICATION_ARG)
                    .withChild(ImmutableNodes.leafNode(TOPIC_ID_ARG, new TopicId(topicId)))
                    .withChild(ImmutableNodes.leafNode(EVENT_SOURCE_ARG, eventSourceIdent))
                    .withChild(anyXmlNode)
                    .build();
            return topicNotification;
        }
    }
}
