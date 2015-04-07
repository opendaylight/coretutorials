/*
 *  Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.hweventsource.uagent;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.uagent.topic.rev150408.ReadTopicInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.uagent.topic.rev150408.UagentTopicReadService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAgent implements DOMNotificationListener, UagentTopicReadService, AutoCloseable{

    private static final Logger LOG = LoggerFactory.getLogger(UserAgent.class);

    private static final NodeIdentifier EVENT_SOURCE_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "node-id"));
    private static final NodeIdentifier PAYLOAD_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "payload"));
    private static final NodeIdentifier TOPIC_ID_ARG = new NodeIdentifier(QName.create(TopicNotification.QNAME, "topic-id"));
    private final File outputFile;
    private final Set<String> registeredTopic = new HashSet<>();
    private BufferedWriter bufferedWriter;

    private ListenerRegistration<UserAgent> listenerReg;

    public static UserAgent create( DOMNotificationService notifyService, RpcProviderRegistry rpcRegistry, File outputFile){
        final UserAgent ua = new UserAgent(outputFile);
        try {
            ua.createWriter();
        } catch (IOException e) {
            return null;
        }
        ua.registerListener(notifyService);
        rpcRegistry.addRpcImplementation(UagentTopicReadService.class, ua);
        return ua;
    }

    private UserAgent(File outputFile) {
        this.outputFile = outputFile;
    }

    private void registerListener(DOMNotificationService notifyService){
        this.listenerReg = notifyService.registerNotificationListener(this,SchemaPath.create(true, TopicNotification.QNAME));
    }

    private void createWriter() throws IOException{
        if(outputFile.exists()){
            outputFile.delete();
        }
        FileWriter outFw = null;
        try {
            outFw = new FileWriter (outputFile.getAbsolutePath());
        } catch (IOException e) {
            LOG.error("Can not create writer for {} {}",outputFile.getAbsolutePath(), e);
            throw e;
        }
        this.bufferedWriter = new BufferedWriter(outFw);
        writeOutputLine("User Agent - started");
    }

    @Override
    public void close() throws Exception{
        this.listenerReg.close();
        this.bufferedWriter.close();
    }

    @Override
    public void onNotification(DOMNotification notification) {
    	LOG.debug("Notification arrived...");
    	String nodeName = null;
    	TopicId topicId = null;
    	if(notification.getBody().getChild(EVENT_SOURCE_ARG).isPresent()){
    		nodeName = notification.getBody().getChild(EVENT_SOURCE_ARG).get().getValue().toString();
    	}
    	if(notification.getBody().getChild(TOPIC_ID_ARG).isPresent()){;
    		topicId = (TopicId) notification.getBody().getChild(TOPIC_ID_ARG).get().getValue();
    	}
        if( nodeName != null && topicId != null ){
        	if(registeredTopic.contains(topicId.getValue())){
        		final String payLoadString = parsePayLoad(notification);
                if(payLoadString != null){
                    writeOutputLine(nodeName + " : " + payLoadString);
                    LOG.debug("Notification write to FILE");
                }
        	}
        }
    }

    @Override
    public synchronized Future<RpcResult<Void>> readTopic(ReadTopicInput input) {
        String topicId = input.getTopicId().getValue();
        if(registeredTopic.contains(topicId) == false){
            registeredTopic.add(topicId);
            LOG.info("UserAgent start read notification with TopicId {}", topicId);
        }
        return immediateFuture(RpcResultBuilder.success((Void) null).build());
    }

    private String parsePayLoad(DOMNotification notification){
    	@SuppressWarnings("unused")
		Object obj = notification.getBody().getChild(PAYLOAD_ARG).get();
        final AnyXmlNode encapData = (AnyXmlNode) notification.getBody().getChild(PAYLOAD_ARG).get();
        final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        final TransformerFactory tf = TransformerFactory.newInstance();
        try {
        final Transformer transformer = tf.newTransformer();
            transformer.transform(encapData.getValue(), result);
        } catch (TransformerException e) {
            LOG.error("Can not parse PayLoad data", e);
            return null;
        }
        writer.flush();
        return writer.toString();
    }

    private void writeOutputLine(String textLine){
        String outputText = Calendar.getInstance().getTime().toString();
        if(textLine != null){
            outputText = outputText + " : " + textLine;
        }
        try {
            this.bufferedWriter.write(outputText);
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            LOG.error("Can not write text line.", e);
        }
    }

}
