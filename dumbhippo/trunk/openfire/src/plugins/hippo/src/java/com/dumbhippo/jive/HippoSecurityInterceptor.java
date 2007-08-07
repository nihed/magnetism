package com.dumbhippo.jive;

import org.jivesoftware.openfire.RoutableChannelHandler;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.server.OutgoingSessionPromise;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

/**
 * The role of this interceptor is to block spoofing attempts where packets are
 * sent to a user in hopes that the recipient won't check the source and think that 
 * it was sent from the system  
 * 
 * @author otaylor
 */
public class HippoSecurityInterceptor implements PacketInterceptor {

	enum Role {
		UNKNOWN,
		LOCAL_USER,
		REMOTE_USER,
		COMPONENT,
		ADMIN
	};
	
	private Role classifyAddress(JID address) {
		XMPPServer server = XMPPServer.getInstance();
		
		if (address == null || address.getDomain() == null)
			return Role.UNKNOWN;
		
		if (server.isLocal(address)) {
			if ("admin".equals(address.getNode()))
				return Role.ADMIN;
			else
				return Role.LOCAL_USER;
		}

		String domain = address.getDomain();
		
		// Find the domain relevant to the JID, if it's a local alias
		String serverDomain = server.getDomainForHostname(address.getDomain());
		
		if (domain.equals(serverDomain) ||
			(domain.endsWith(serverDomain) && domain.charAt(domain.length() - serverDomain.length() - 1) == '.')) {
			// Candidate for a component

			// I don't really like this way of checking for an external domain at all, but
			// it's how some of the Openfire code handles it. The check above basically makes
			// it not that important since we've restricted ourselves to subdomains of one
			// of the server domains
			
			RoutableChannelHandler route = server.getRoutingTable().getRoute(new JID(address.getDomain()));
	        if (route != null && !(route instanceof OutgoingSessionPromise))
	            return Role.COMPONENT;
		} 
	        
	    return Role.REMOTE_USER;
	}
	
	public void interceptPacket(Packet packet, Session session,
								boolean incoming, boolean processed) throws PacketRejectedException {
		if (incoming && !processed)
			interceptIncomingPacket(packet);
	}

	private void interceptIncomingPacket(Packet packet) throws PacketRejectedException {
		Role fromRole = classifyAddress(packet.getFrom());
		Role toRole = classifyAddress(packet.getTo());
		
		if (fromRole == Role.REMOTE_USER) {
			if (toRole != Role.ADMIN) {
				throw new PacketRejectedException("Packets can only be sent to admin user");
			}
			
			// Would need more auditing of IQ handlers
			if (packet instanceof IQ)
				throw new PacketRejectedException("IQs from external users not currently allowed");
		}
		
		if (fromRole == Role.LOCAL_USER) {
			if (toRole == Role.LOCAL_USER)
				throw new PacketRejectedException("Not allowed to send messages directly to other users");
		}
	}
}
