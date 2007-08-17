package com.dumbhippo.jive;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketException;
import org.jivesoftware.openfire.RoutableChannelHandler;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;

import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.impl.MessengerGlueBean;
import com.dumbhippo.server.util.EJBUtil;

public class AdminHandler implements RoutableChannelHandler{
	JID address;
	private Map<String, IQHandler> iqHandlers = new HashMap<String, IQHandler>();
	
	public AdminHandler() {
		String serverName = XMPPServer.getInstance().getServerInfo().getName();
		
		address = new JID("admin", serverName, null);
	}
	
	public JID getAddress() {
		return address;
	}

	public void process(Packet packet) throws UnauthorizedException, PacketException {
		Log.debug("processing packet sent to admin user: " + packet);
		
		if (packet instanceof Message) {
			Message message = (Message)packet;

			// It's important to not to auto-reply to messages of type error!
			if (message.getType() == Message.Type.normal) {
		        Message reply = new Message();
		        reply.setFrom(packet.getTo());
		        reply.setTo(packet.getFrom());
		        reply.setType(Message.Type.normal);
		        
		        reply.setBody("You sent me something, here's my reply");
		        
		        XMPPServer.getInstance().getPacketRouter().route(reply);
			}
		} else if (packet instanceof IQ) {
			IQ iq = (IQ)packet;
			Element child = iq.getChildElement();
			String namespace = child.getNamespaceURI();
			if (namespace != null) {
				IQHandler handler = iqHandlers.get(namespace);
				if (handler != null) {
					handler.process(iq);
					return;
				}
			}
			
			IQ reply = IQ.createResultIQ(iq);
			reply.setError(new PacketError(PacketError.Condition.service_unavailable,
						   				   PacketError.Type.modify,
										   "No handler found for IQ"));
			
			XMPPServer.getInstance().getPacketRouter().route(reply);
		} else if (packet instanceof Presence) {
			Presence presence = (Presence)packet;
			
			if (presence.getType() == Presence.Type.subscribe) {
				Presence reply = new Presence();
				
		        reply.setFrom(packet.getTo());
		        reply.setTo(packet.getFrom());
		        reply.setType(Presence.Type.subscribed);
		        
		        XMPPServer.getInstance().getPacketRouter().route(reply);
		        
		        EJBUtil.defaultLookup(MessengerGlue.class).sendQueuedXmppMessages(packet.getTo().toString(), packet.getFrom().toString());
		        
			} else if (presence.getType() == Presence.Type.probe) {
				Presence reply = new Presence();
				
		        reply.setFrom(packet.getTo());
		        reply.setTo(packet.getFrom());
		        
		        XMPPServer.getInstance().getPacketRouter().route(reply);
			}
		}
	}

	public void addIQHandler(IQHandler handler) {
		iqHandlers.put(handler.getInfo().getNamespace(), handler);
	}
}
