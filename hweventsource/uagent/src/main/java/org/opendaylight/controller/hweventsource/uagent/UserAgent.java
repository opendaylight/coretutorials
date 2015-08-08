/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
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

/**
 * Class is an example of user agent to listen and process TopicDOMNotifications.
 * User agent implements UagentTopicReadService and process "read topic" REST requests.
 * Client asks user agent to process TopicNotification with given TopicId by "read topic REST request".
 * User agent listens all TopicNotification notifications, but it will process only notification with requested TopicID.
 * Process of notification consist of two phases:
 *     - convert "payload" into string
 *     - write string into output file
 * Instance of UserAgent is created by static method (see create(...)). Parameter File outputFile represents
 * output file where notifications will write.
 */
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
            // try to create writer
            ua.createWriter();
        } catch (IOException e) {
            // if there an exception then no user agent is returned
            return null;
        }
        // register created user agent as a listener of TopicNotification
        ua.registerListener(notifyService);
        // register user agent as implementation of UagentTopicReadService
        rpcRegistry.addRpcImplementation(UagentTopicReadService.class, ua);
        return ua;
    }

    private UserAgent(File outputFile) {
        this.outputFile = outputFile;
    }


    private void registerListener(DOMNotificationService notifyService){
        this.listenerReg = notifyService.registerNotificationListener(this,SchemaPath.create(true, TopicNotification.QNAME));
    }

    /*
     * create writer for output file
     * If file exists then file is rewrite
     */
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

    /*
     * This method is called when TopicNotification is coming.
     * @see org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener#onNotification(org.opendaylight.controller.md.sal.dom.api.DOMNotification)
     */
    @Override
    public void onNotification(DOMNotification notification) {
        LOG.debug("Notification arrived...");
        String nodeName = null;
        TopicId topicId = null;
        // get the nodeName (identifier of event source) from notification
        if(notification.getBody().getChild(EVENT_SOURCE_ARG).isPresent()){
            nodeName = notification.getBody().getChild(EVENT_SOURCE_ARG).get().getValue().toString();
        }
        // get the TopicId from notification
        if(notification.getBody().getChild(TOPIC_ID_ARG).isPresent()){;
            topicId = (TopicId) notification.getBody().getChild(TOPIC_ID_ARG).get().getValue();
        }
        if( nodeName != null && topicId != null ){
        	// if nodeName and TopicId are present and TopicId has been requested to process (TopicId is in registeredTopic)
        	// then notification is parsed and written into the file.
            if(registeredTopic.contains(topicId.getValue())){
                final String payLoadString = parsePayLoad(notification);
                if(payLoadString != null){
                    writeOutputLine(nodeName + " : " + payLoadString);
                    LOG.debug("Notification write to FILE");
                }
            }
        }
    }

    /*
     * Method is called when "read topic" REST request has been sent
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.uagent.topic.rev150408.UagentTopicReadService#readTopic(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hweventsource.uagent.topic.rev150408.ReadTopicInput)
     */
    @Override
    public synchronized Future<RpcResult<Void>> readTopic(ReadTopicInput input) {
        String topicId = input.getTopicId().getValue();
        // if requested TopicId has not been requested before then it is added into to register
        if(registeredTopic.contains(topicId) == false){
            registeredTopic.add(topicId);
            LOG.info("UserAgent start read notification with TopicId {}", topicId);
        }
        return immediateFuture(RpcResultBuilder.success((Void) null).build());
    }

    // Helper for notification parse
    private String parsePayLoad(DOMNotification notification){

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

    // Helper for write line into output file
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
