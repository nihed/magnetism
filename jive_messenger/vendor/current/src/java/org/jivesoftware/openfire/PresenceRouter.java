/**
 * $RCSfile: PresenceRouter.java,v $
 * $Revision: 3138 $
 * $Date: 2005-12-01 02:13:26 -0300 (Thu, 01 Dec 2005) $
 *
 * Copyright (C) 2007 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.openfire;

import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.container.BasicModule;
import org.jivesoftware.openfire.handler.PresenceSubscribeHandler;
import org.jivesoftware.openfire.handler.PresenceUpdateHandler;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

/**
 * <p>Route presence packets throughout the server.</p>
 * <p>Routing is based on the recipient and sender addresses. The typical
 * packet will often be routed twice, once from the sender to some internal
 * server component for handling or processing, and then back to the router
 * to be delivered to it's final destination.</p>
 *
 * @author Iain Shigeoka
 */
public class PresenceRouter extends BasicModule {

    private RoutingTable routingTable;
    private PresenceUpdateHandler updateHandler;
    private PresenceSubscribeHandler subscribeHandler;
    private PresenceManager presenceManager;
    private SessionManager sessionManager;
    private MulticastRouter multicastRouter;
    private String serverName;

    /**
     * Constructs a presence router.
     */
    public PresenceRouter() {
        super("XMPP Presence Router");
    }

    /**
     * Routes presence packets.
     *
     * @param packet the packet to route.
     * @throws NullPointerException if the packet is null.
     */
    public void route(Presence packet) {
        if (packet == null) {
            throw new NullPointerException();
        }
        ClientSession session = sessionManager.getSession(packet.getFrom());
        try {
            // Invoke the interceptors before we process the read packet
            InterceptorManager.getInstance().invokeInterceptors(packet, session, true, false);
            if (session == null || session.getStatus() == Session.STATUS_AUTHENTICATED) {
                handle(packet);
            }
            else {
                packet.setTo(session.getAddress());
                packet.setFrom((JID)null);
                packet.setError(PacketError.Condition.not_authorized);
                session.process(packet);
            }
            // Invoke the interceptors after we have processed the read packet
            InterceptorManager.getInstance().invokeInterceptors(packet, session, true, true);
        }
        catch (PacketRejectedException e) {
            if (session != null) {
                // An interceptor rejected this packet so answer a not_allowed error
                Presence reply = new Presence();
                reply.setID(packet.getID());
                reply.setTo(session.getAddress());
                reply.setFrom(packet.getTo());
                reply.setError(PacketError.Condition.not_allowed);
                session.process(reply);
                // Check if a message notifying the rejection should be sent
                if (e.getRejectionMessage() != null && e.getRejectionMessage().trim().length() > 0) {
                    // A message for the rejection will be sent to the sender of the rejected packet
                    Message notification = new Message();
                    notification.setTo(session.getAddress());
                    notification.setFrom(packet.getTo());
                    notification.setBody(e.getRejectionMessage());
                    session.process(notification);
                }
            }
        }
    }

    private void handle(Presence packet) {
        JID recipientJID = packet.getTo();
        // Check if the packet was sent to the server hostname
        if (recipientJID != null && recipientJID.getNode() == null &&
                recipientJID.getResource() == null && serverName.equals(recipientJID.getDomain())) {
            if (packet.getElement().element("addresses") != null) {
                // Presence includes multicast processing instructions. Ask the multicastRouter
                // to route this packet
                multicastRouter.route(packet);
                return;
            }
        }
        try {
            Presence.Type type = packet.getType();
            // Presence updates (null is 'available')
            if (type == null || Presence.Type.unavailable == type) {
                // check for local server target
                if (recipientJID == null || recipientJID.getDomain() == null ||
                        "".equals(recipientJID.getDomain()) || (recipientJID.getNode() == null &&
                        recipientJID.getResource() == null) &&
                        serverName.equals(recipientJID.getDomain())) {
                    updateHandler.process(packet);
                }
                else {
                    // Check that sender session is still active
                    Session session = sessionManager.getSession(packet.getFrom());
                    if (session != null && session.getStatus() == Session.STATUS_CLOSED) {
                        Log.warn("Rejected available presence: " + packet + " - " + session);
                        return;
                    }

                    // The user sent a directed presence to an entity
                    // Broadcast it to all connected resources
                    for (ChannelHandler route : routingTable.getRoutes(recipientJID)) {
                        // Register the sent directed presence
                        updateHandler.directedPresenceSent(packet, route, recipientJID.toString());
                        // Route the packet
                        route.process(packet);
                    }
                }

            }
            else if (Presence.Type.subscribe == type // presence subscriptions
                    || Presence.Type.unsubscribe == type
                    || Presence.Type.subscribed == type
                    || Presence.Type.unsubscribed == type)
            {
                subscribeHandler.process(packet);
            }
            else if (Presence.Type.probe == type) {
                // Handle a presence probe sent by a remote server
                if (!XMPPServer.getInstance().isLocal(recipientJID)) {
                    // Target is a component of the server so forward it
                    ChannelHandler route = routingTable.getRoute(recipientJID);
                    if (route != null) {
                        route.process(packet);
                    }
                }
                else {
                    // Handle probe to a local user
                    presenceManager.handleProbe(packet);
                }
            }
            else {
                // It's an unknown or ERROR type, just deliver it because there's nothing
                // else to do with it
                ChannelHandler route = routingTable.getRoute(recipientJID);
                if (route != null) {
                    route.process(packet);
                }
            }

        }
        catch (Exception e) {
            Log.error(LocaleUtils.getLocalizedString("admin.error.routing"), e);
            Session session = sessionManager.getSession(packet.getFrom());
            if (session != null) {
                Connection conn = session.getConnection();
                if (conn != null) {
                    conn.close();
                }
            }
        }
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        serverName = server.getServerInfo().getName();
        routingTable = server.getRoutingTable();
        updateHandler = server.getPresenceUpdateHandler();
        subscribeHandler = server.getPresenceSubscribeHandler();
        presenceManager = server.getPresenceManager();
        multicastRouter = server.getMulticastRouter();
        sessionManager = server.getSessionManager();
    }
}
