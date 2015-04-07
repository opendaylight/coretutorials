/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.hweventsource.sample;

import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleEventSourceGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(SampleEventSourceGenerator.class);

    private static final int create_event_source_delay = 200;
    private static final String node_id_prefix = "EventSourceSample";

    private final Short numberSampleEventSources;
    private final Short messageGeneratePeriod;
    private final HelloWorldEventSourceManager eventSourceManager;
    private final DOMNotificationPublishService domPublish;

    public SampleEventSourceGenerator(HelloWorldEventSourceManager eventSourceManager,DOMNotificationPublishService domPublish, Short numberSampleEventSources, Short messageGeneratePeriod) {
        this.messageGeneratePeriod = messageGeneratePeriod;
        this.numberSampleEventSources = numberSampleEventSources;
        this.eventSourceManager = eventSourceManager;
        this.domPublish = domPublish;
        generateEventSources();
    }
    
    private void generateEventSources(){
        try {
            Thread.sleep(create_event_source_delay);
            for(int i = 0; i< this.numberSampleEventSources; i++){
                String identifier = String.format(node_id_prefix+ "%02d", i);
                eventSourceManager.addNewEventSource(getNewEvetSource(identifier));
                Thread.sleep(create_event_source_delay);
            }
        } catch (InterruptedException e) {
            LOG.warn("Can not generate new sample event sources. {}", e);
        }
    }

    private HelloWorldEventSource getNewEvetSource(String identifier){
        Node sourceNode = getNewNode(identifier);
        HelloWorldEventSource eventSource = new HelloWorldEventSource(domPublish, sourceNode,  messageGeneratePeriod);
        return eventSource;
    }

    private Node getNewNode(String nodeIdent){
        NodeId nodeId = new NodeId(nodeIdent);
        NodeBuilder nb = new NodeBuilder();
        nb.setKey(new NodeKey(nodeId));
        return nb.build();
    }

}

