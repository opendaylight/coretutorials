/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.hweventsource.sample;

import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * HelloWorldEventSourceManager manage event sources in this example application
 * Event sources are created by {@link SampleEventSourceGenerator}.
 * This generating process simulates origin of event source.
 * Actual implementation will identify origin of event source
 * by various ways (e.g. listen on changes in topology, create connection of devices etc.).
 *
 * Method addNewEventSource(HelloWorldEventSource eventSource) is called by {@link SampleEventSourceGenerator}
 * when generator created new event source and it want to register it.
 *
 * Manager registers all added event sources in {@link EventSourceRegistry} and store {@link EventSourceRegistration} objects.
 * {@link EventSourceRegistration} objects are used to unregister event sources.
 *
 * @author madamjak
 *
 */
public class HelloWorldEventSourceManager implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldEventSourceManager.class);

    private final EventSourceRegistry eventSourceRegistry;
    private final ConcurrentHashMap<NodeKey, EventSourceRegistration<HelloWorldEventSource>> registrationMap = new ConcurrentHashMap<>();
    public HelloWorldEventSourceManager(final EventSourceRegistry eventSourceRegistry) {
        this.eventSourceRegistry = eventSourceRegistry;
    }

    /*
     * This method is called by by {@link SampleEventSourceGenerator} when generator
     * wants to add new event source.
     */
    void addNewEventSource(HelloWorldEventSource eventSource){
        Preconditions.checkNotNull(eventSource);
        if(registrationMap.containsKey(eventSource.getSourceNodeKey()) == false){
            // if there is no EventSourceRegistration object in registrationMap
            // then event source is registered
            registerEventSource(eventSource);
        }
    }

    /*
     * Method register event source in EventSourceRegistry and store EventSourceRegistration object
     * in registration map. This method should by synchronized in actual application or
     * concurrence issues can be solved by other suitable way.
     */
    private void registerEventSource(HelloWorldEventSource eventSource){
        EventSourceRegistration<HelloWorldEventSource> esr = eventSourceRegistry.registerEventSource(eventSource);
        registrationMap.putIfAbsent(eventSource.getSourceNodeKey(), esr);
        LOG.info("Event source {} has been registered.", eventSource.getSourceNodeKey().getNodeId().getValue());
    }

    /*
     * HelloWorldEventSourceManager unregisters all registered event sources when it finishes its work.
     */
    @Override
    public void close() throws Exception {
        for(EventSourceRegistration<HelloWorldEventSource> esr : registrationMap.values()){
            esr.close();
        }
    }

}
