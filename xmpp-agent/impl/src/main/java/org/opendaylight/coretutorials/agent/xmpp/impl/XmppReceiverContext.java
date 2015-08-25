/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.coretutorials.agent.xmpp.impl;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.user.agent.rev151014.xmpp.user.agents.xmpp.user.agent.ReceiverKey;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmppReceiverContext implements AutoCloseable, MessageListener, Identifiable<ReceiverKey> {

    private static final Logger LOG = LoggerFactory.getLogger(XmppReceiverContext.class);

    private final Chat chat;
    private final ReceiverKey user;

    private XmppReceiverContext(final ChatManager chatManager, final ReceiverKey userName) {
        this.user = userName;
        this.chat = chatManager.createChat(userName.getJabberId(), this);
    }

    static XmppReceiverContext create(final ChatManager chatManager, final ReceiverKey userName) {
        return new XmppReceiverContext(chatManager,userName);
    }

    void sendMessage(final String text) throws NotConnectedException, XMPPException {
        LOG.debug("Sending notification to {} using {}", user, chat);
        chat.sendMessage(text);
    }

    @Override
    public void processMessage(final Chat chat, final Message message) {
        LOG.info("Received XMPP message from {}, ignoring it.", user);
    }

    @Override
    public void close() {
        LOG.info("Closing chat.");
        chat.close();
    }

    @Override
    public ReceiverKey getIdentifier() {
        return user;
    }
}
