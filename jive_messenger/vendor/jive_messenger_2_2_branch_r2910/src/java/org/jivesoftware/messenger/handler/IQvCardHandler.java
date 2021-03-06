/**
 * $RCSfile$
 * $Revision: 1653 $
 * $Date: 2005-07-19 23:21:40 -0400 (Tue, 19 Jul 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.handler;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.messenger.IQHandlerInfo;
import org.jivesoftware.messenger.PacketException;
import org.jivesoftware.messenger.XMPPServer;
import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.jivesoftware.messenger.user.User;
import org.jivesoftware.messenger.user.UserManager;
import org.jivesoftware.messenger.user.UserNotFoundException;
import org.jivesoftware.messenger.vcard.VCardManager;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

/**
 * Implements the TYPE_IQ vcard-temp protocol. Clients
 * use this protocol to set and retrieve the vCard information
 * associated with someone's account.
 * <p/>
 * A 'get' query retrieves the vcard for the addressee.
 * A 'set' query sets the vcard information for the sender's account.
 * <p/>
 * Currently an empty implementation to allow usage with normal
 * clients. Future implementation needed.
 * <p/>
 * <h2>Assumptions</h2>
 * This handler assumes that the request is addressed to the server.
 * An appropriate TYPE_IQ tag matcher should be placed in front of this
 * one to route TYPE_IQ requests not addressed to the server to
 * another channel (probably for direct delivery to the recipient).
 * <p/>
 * <h2>Warning</h2>
 * There should be a way of determining whether a session has
 * authorization to access this feature. I'm not sure it is a good
 * idea to do authorization in each handler. It would be nice if
 * the framework could assert authorization policies across channels.
 * <p/>
 * <h2>Warning</h2>
 * I have noticed incompatibility between vCard XML used by Exodus and Psi.
 * There is a new vCard standard going through the JSF JEP process. We might
 * want to start either standardizing on clients (probably the most practical),
 * sending notices for non-conformance (useful),
 * or attempting to translate between client versions (not likely).
 *
 * @author Iain Shigeoka
 */
public class IQvCardHandler extends IQHandler {

    private IQHandlerInfo info;
    private UserManager userManager;

    public IQvCardHandler() {
        super("XMPP vCard Handler");
        info = new IQHandlerInfo("vCard", "vcard-temp");
    }

    public IQ handleIQ(IQ packet) throws UnauthorizedException, PacketException {
        IQ result = null;
        try {
            IQ.Type type = packet.getType();
            if (type.equals(IQ.Type.set)) {
                result = IQ.createResultIQ(packet);
                User user = userManager.getUser(packet.getFrom().getNode());
                // Proper format
                Element vcard = packet.getChildElement();
                if (vcard != null) {
                    try {
                        VCardManager.getInstance().setVCard(user.getUsername(), vcard);
                    }
                    catch (Exception e) {
                        Log.error(e);
                        result.setError(PacketError.Condition.internal_server_error);
                    }
                }
            }
            else if (type.equals(IQ.Type.get)) {
                JID recipient = packet.getTo();
                // If no TO was specified then get the vCard of the sender of the packet
                if (recipient == null) {
                    recipient = packet.getFrom();
                }

                result = IQ.createResultIQ(packet);

                Element vcard = DocumentHelper.createElement(QName.get("vCard", "vcard-temp"));
                result.setChildElement(vcard);
                // Only try to get the vCard values of non-anonymous users 
                if (recipient != null && recipient.getNode() != null) {
                    User user = userManager.getUser(recipient.getNode());
                    VCardManager vManager = VCardManager.getInstance();
                    Element userVCard = vManager.getVCard(user.getUsername());
                    if (userVCard != null) {
                        result.setChildElement(userVCard);
                    }
                }
            }
            else {
                result = IQ.createResultIQ(packet);
                result.setChildElement(packet.getChildElement().createCopy());
                result.setError(PacketError.Condition.not_acceptable);
            }
        }
        catch (UserNotFoundException e) {
            result = IQ.createResultIQ(packet);
            result.setChildElement(packet.getChildElement().createCopy());
            result.setError(PacketError.Condition.item_not_found);
        }
        return result;
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        userManager = server.getUserManager();
    }

    public IQHandlerInfo getInfo() {
        return info;
    }
}
