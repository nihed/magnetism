/**
 * $RCSfile$
 * $Revision: 1761 $
 * $Date: 2005-08-09 19:34:09 -0300 (Tue, 09 Aug 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire.handler;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.wildfire.*;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.disco.*;
import org.jivesoftware.wildfire.forms.DataForm;
import org.jivesoftware.wildfire.forms.FormField;
import org.jivesoftware.wildfire.forms.spi.XDataFormImpl;
import org.jivesoftware.wildfire.forms.spi.XFormFieldImpl;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implements JEP-0013: Flexible Offline Message Retrieval. Allows users to request number of
 * messages, request message headers, retrieve specific messages, remove specific messages,
 * retrieve all messages and remove all messages.
 *
 * @author Gaston Dombiak
 */
public class IQOfflineMessagesHandler extends IQHandler implements ServerFeaturesProvider,
        DiscoInfoProvider, DiscoItemsProvider {

    private static final String NAMESPACE = "http://jabber.org/protocol/offline";

    private SimpleDateFormat dateFormat;
    private IQHandlerInfo info;
    private IQDiscoInfoHandler infoHandler;
    private IQDiscoItemsHandler itemsHandler;

    private SessionManager sessionManager;
    private OfflineMessageStore messageStore;

    public IQOfflineMessagesHandler() {
        super("Flexible Offline Message Retrieval Handler");
        info = new IQHandlerInfo("offline", NAMESPACE);
        dateFormat = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ss.SSS'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException {
        IQ reply = IQ.createResultIQ(packet);
        Element offlineRequest = packet.getChildElement();

        if (offlineRequest.element("purge") != null) {
            // User requested to delete all offline messages
            messageStore.deleteMessages(packet.getFrom().getNode());
        }
        else if (offlineRequest.element("fetch") != null) {
            // Mark that offline messages shouldn't be sent when the user becomes available
            stopOfflineFlooding(packet.getFrom());
            ClientSession session = sessionManager.getSession(packet.getFrom());
            // User requested to receive all offline messages
            for (OfflineMessage offlineMessage : messageStore.getMessages(
                    packet.getFrom().getNode(), false)) {
                sendOfflineMessage(offlineMessage, session);
            }
        }
        else {
            for (Iterator it = offlineRequest.elementIterator("item"); it.hasNext();) {
                Element item = (Element) it.next();
                Date creationDate = null;
                synchronized (dateFormat) {
                    try {
                        creationDate = dateFormat.parse(item.attributeValue("node"));
                    }
                    catch (ParseException e) {
                        Log.error("Error parsing date", e);
                    }
                }
                if ("view".equals(item.attributeValue("action"))) {
                    // User requested to receive specific message
                    OfflineMessage offlineMsg = messageStore.getMessage(packet.getFrom().getNode(),
                            creationDate);
                    if (offlineMsg != null) {
                        ClientSession session = sessionManager.getSession(packet.getFrom());
                        sendOfflineMessage(offlineMsg, session);
                    }
                }
                else if ("remove".equals(item.attributeValue("action"))) {
                    // User requested to delete specific message
                    messageStore.deleteMessage(packet.getFrom().getNode(), creationDate);
                }
            }
        }
        return reply;
    }

    private void sendOfflineMessage(OfflineMessage offlineMessage, ClientSession session) {
        Element offlineInfo = offlineMessage.addChildElement("offline", NAMESPACE);
        synchronized (dateFormat) {
            offlineInfo.addElement("item").addAttribute("node",
                    dateFormat.format(offlineMessage.getCreationDate()));
        }
        session.process(offlineMessage);
    }

    public IQHandlerInfo getInfo() {
        return info;
    }

    public Iterator getFeatures() {
        ArrayList<String> features = new ArrayList<String>();
        features.add(NAMESPACE);
        return features.iterator();
    }

    public Iterator<Element> getIdentities(String name, String node, JID senderJID) {
        ArrayList<Element> identities = new ArrayList<Element>();
        Element identity = DocumentHelper.createElement("identity");
        identity.addAttribute("category", "automation");
        identity.addAttribute("type", "message-list");
        identities.add(identity);
        return identities.iterator();
    }

    public Iterator<String> getFeatures(String name, String node, JID senderJID) {
        return Arrays.asList(NAMESPACE).iterator();
    }

    public XDataFormImpl getExtendedInfo(String name, String node, JID senderJID) {
        // Mark that offline messages shouldn't be sent when the user becomes available
        stopOfflineFlooding(senderJID);

        XDataFormImpl dataForm = new XDataFormImpl(DataForm.TYPE_RESULT);

        XFormFieldImpl field = new XFormFieldImpl("FORM_TYPE");
        field.setType(FormField.TYPE_HIDDEN);
        field.addValue(NAMESPACE);
        dataForm.addField(field);

        field = new XFormFieldImpl("number_of_messages");
        field.addValue(String.valueOf(messageStore.getMessages(senderJID.getNode(), false).size()));
        dataForm.addField(field);

        return dataForm;
    }

    public boolean hasInfo(String name, String node, JID senderJID) {
        return NAMESPACE.equals(node) && senderJID.getNode() != null;
    }

    public Iterator<Element> getItems(String name, String node, JID senderJID) {
        // Mark that offline messages shouldn't be sent when the user becomes available
        stopOfflineFlooding(senderJID);
        List<Element> answer = new ArrayList<Element>();
        Element item;
        for (OfflineMessage offlineMessage : messageStore.getMessages(senderJID.getNode(), false)) {
            item = DocumentHelper.createElement("item");
            item.addAttribute("jid", senderJID.toBareJID());
            item.addAttribute("name", offlineMessage.getFrom().toString());
            synchronized (dateFormat) {
                item.addAttribute("node", dateFormat.format(offlineMessage.getCreationDate()));
            }

            answer.add(item);
        }

        return answer.iterator();
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        infoHandler = server.getIQDiscoInfoHandler();
        itemsHandler = server.getIQDiscoItemsHandler();
        messageStore = server.getOfflineMessageStore();
        sessionManager = server.getSessionManager();
    }

    public void start() throws IllegalStateException {
        super.start();
        infoHandler.setServerNodeInfoProvider(NAMESPACE, this);
        itemsHandler.setServerNodeInfoProvider(NAMESPACE, this);
    }

    public void stop() {
        super.stop();
        infoHandler.removeServerNodeInfoProvider(NAMESPACE);
        itemsHandler.removeServerNodeInfoProvider(NAMESPACE);
    }

    private void stopOfflineFlooding(JID senderJID) {
        ClientSession session = sessionManager.getSession(senderJID);
        if (session != null) {
            session.setOfflineFloodStopped(true);
        }
    }
}
