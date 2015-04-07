package org.opendaylight.coretutorials.hweventsource.sample;

import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class HelloWorldEventSourceManager implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(HelloWorldEventSourceManager.class);
	
    private final EventSourceRegistry eventSourceRegistry;
    private final ConcurrentHashMap<NodeKey, EventSourceRegistration<HelloWorldEventSource>> registrationMap = new ConcurrentHashMap<>();
    public HelloWorldEventSourceManager(final EventSourceRegistry eventSourceRegistry) {
        this.eventSourceRegistry = eventSourceRegistry;
    }

    void addNewEventSource(HelloWorldEventSource eventSource){
        Preconditions.checkNotNull(eventSource);
        if(registrationMap.containsKey(eventSource.getSourceNodeKey()) == false){
            registerEventSource(eventSource);
        }
    }

    private void registerEventSource(HelloWorldEventSource eventSource){
        EventSourceRegistration<HelloWorldEventSource> esr = eventSourceRegistry.registerEventSource(eventSource);
        registrationMap.putIfAbsent(eventSource.getSourceNodeKey(), esr);
        LOG.info("Event source {} has been registered.", eventSource.getSourceNode().getNodeId().getValue());
    }

    @Override
    public void close() throws Exception {
        for(EventSourceRegistration<HelloWorldEventSource> esr : registrationMap.values()){
            esr.close();
        }
    }

}
