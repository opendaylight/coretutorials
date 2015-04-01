/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials.agent.xmpp.impl;
/*
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;


public class PseudoPubSub {
    private static final String DEFAULT_PASSWORD = "NODE_PASSWD";  // CONSTANT
    private static final String NODE_PREFIX = "node-";  // CONSTANT
    private static final Presence AVAILABLE_PRESENCE = new Presence(Presence.Type.available);

    private final String domain;
    private final int jabberPort;
    private final String suAccount;
    private final String suPassword;

    private final Map<String, XMPPConnection> publisherConnections = new HashMap<>();
    private final ConnectionConfiguration connectionConfiguration;
    private final XMPPConnection adminConnection;
    // TODO: implement correct cache invalidation logic
    private final List<String> subscribers = new ArrayList<>();


    public PseudoPubSub(final String domain, final int jabberPort, final String adminUsername, final String adminPassword) {
        this.domain = domain;
        this.jabberPort = jabberPort;
        this.suAccount = adminUsername;
        this.suPassword = adminPassword;

        connectionConfiguration = new ConnectionConfiguration(this.domain, this.jabberPort);
        adminConnection = createConnection(suAccount, suPassword);
        initSubscribersCache();
    }

    public void createPublisher(final String publisherAccount) throws Exception {
        final AccountManager am = AccountManager.getInstance(adminConnection);
        try {
            am.createAccount(NODE_PREFIX + publisherAccount, DEFAULT_PASSWORD);
        } catch (final XMPPException.XMPPErrorException e) {
            // FIXME: Check for account instead of solving conflict situation. Current solution works but is quite nasty.
            if (e.getXMPPError().getCondition().contains("conflict") == false) {
                 throw e;
            }
        }
        // TODO: We create account connection for every requested account. We must be sure we create connection for
        //       accounts created previously
        final XMPPConnection publisherConnection = createConnection(NODE_PREFIX + publisherAccount, DEFAULT_PASSWORD);
        // We keep only event source connections, since every other account is message recipient only
        publisherConnections.put(publisherAccount, publisherConnection);
    }

    public void publishMessage(final String publisherAccount, final String message) throws Exception {
        // TODO: Solve missing publisherAccount
        // TODO: Solve missing account connection
        // TODO: Solve closed connection
        // TODO: Publish message asynchronously
        final XMPPConnection eventSourceConnection = publisherConnections.get(publisherAccount);
        eventSourceConnection.sendPacket(AVAILABLE_PRESENCE);

        final ChatManager chatmanager = ChatManager.getInstanceFor(eventSourceConnection);

        for (final String subscriber : subscribers) {
            final Chat newChat = chatmanager.createChat(subscriber, new MessageListener() {
               @Override
            public void processMessage(final Chat chat, final Message message) {}
           });

           newChat.sendMessage(message);
        }
    }


    private void initSubscribersCache() {
        final Roster roster = adminConnection.getRoster();
        for (final RosterEntry entry : roster.getEntries()) {
            if (entry.getUser().startsWith(NODE_PREFIX) == Boolean.FALSE
                && entry.getUser().startsWith(suAccount) == Boolean.FALSE) {
                subscribers.add(entry.getUser());
            }
        }
    }

    private XMPPConnection createConnection(final String account, final String password) {
        final XMPPConnection connection = new XMPPTCPConnection(connectionConfiguration);

        try {
            connection.connect();
            connection.login(account, password);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return connection;
    }
}

