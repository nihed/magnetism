/**
 * 
 */
package com.dumbhippo.hungry.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Authentication;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

/**
 * Utility class for connecting to the message daemon as 
 * some user and interacting with it.
 *  
 * @author hp
 *
 */
public class JabberClient {
	
	private String userId;
	private String jiveUser;
	private String authKey;
	private String host;
	private int port;
	private BlockingQueue<PacketWrapper> incoming;
	private JabberThread thread;
	
	// this is purely to allow null packets in the queue
	private static class PacketWrapper {
		private Packet packet;
		
		PacketWrapper(Packet packet) {
			this.packet = packet;
		}
		
		public Packet getPacket() {
			return packet;
		} 
	}
	
	public JabberClient(String userId, String authKey) {
		XMPPConnection.DEBUG_ENABLED = false;
		this.userId = userId;
		this.authKey = authKey;
	
		jiveUser = guidToJiveUserName(userId);
		
		Config config = Config.getDefault();
		host = config.getValue(ConfigValue.JIVE_HOST);
		port = config.getIntValue(ConfigValue.JIVE_PORT);
		
		incoming = new LinkedBlockingQueue<PacketWrapper>();
	}
	
	public JabberClient(String userId) {
		this(userId, CheatSheet.getReadOnly().getUserAuthKey(userId));
	}
	
	public Packet take() {
		return poll(60000); // 60 seconds
	}
	
	public Packet poll(long timeout) {
		while (true) {
			if (!thread.isConnected())
				return null;

			try {
				PacketWrapper wrapper = incoming.poll(timeout, TimeUnit.MILLISECONDS);
				if (wrapper != null)
					return wrapper.getPacket();
				else
					return null;
			} catch (InterruptedException e) {
			}
		}		
	}
	
	private void put(Packet packet) {
		while (true) {
			try {
				incoming.put(new PacketWrapper(packet));
				return;
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void login() {
		
		// because we do auth by hand, XMPPConnection is a little bit 
		// broken/confused (isAuthenticated won't work for example)
		
		thread = new JabberThread();
		
		thread.open();
		
		@SuppressWarnings("unused") IQ reply;
		try {
			reply = thread.sendGetAuthentication();
		} catch (XMPPException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		//System.out.println("Got auth options: " + reply.toXML());
		
		try {
			reply = thread.sendDigestAuthentication();
		} catch (XMPPException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		//System.out.println("Logged in, auth reply packet: " + reply.toXML());
		
		thread.sendAvailable();
		
		System.out.println(userId + " signed in to jive");
	}
	
	public boolean isConnected() {
		return thread != null && thread.isConnected();
	}
	
	public void close() {
		if (thread != null) {
			thread.close();
			thread = null;
		}
	}
	
	@SuppressWarnings("unused")
	private String jiveUserNameToGuid(String username) {
		StringBuilder transformedName = new StringBuilder();
		for (int i = 0; i < username.length(); i++) {
			if (i+1 < username.length() && username.charAt(i+1) == '_') {
				transformedName.append(Character.toLowerCase(username.charAt(i)));
				i++;
			} else {
				transformedName.append(Character.toUpperCase(username.charAt(i)));
			}
		}
		return transformedName.toString();
	}
	
	private String guidToJiveUserName(String guid) {
		// guid aBc becomes jive a_bc_
		StringBuilder sb = new StringBuilder();
		for (char c : guid.toCharArray()) {
			if (Character.isDigit(c)) {
				sb.append(c);
			} else if (Character.isLowerCase(c)) {
				sb.append(c);
				sb.append('_');
			} else if (Character.isUpperCase(c)) {
				sb.append(Character.toLowerCase(c));
			} else {
				throw new RuntimeException("weird character '" + c + "' in guid");
			}
		}
		return sb.toString();
	}
	
	private class JabberThread implements PacketListener, ConnectionListener {
		
		private XMPPConnection connection; 
		
		public JabberThread() {
		}

		public void open() {
			try {
				//System.out.println("Smack login timeout " + SmackConfiguration.getPacketReplyTimeout());
				
				connection = new XMPPConnection(host, port);
				
				connection.addPacketListener(this, null);
				connection.addConnectionListener(this);
			} catch (XMPPException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not login as guid " + userId + " jid " + jiveUser, e);
			}
		}
		
		public void close() {
			if (isConnected()) {
				connection.close();
			}
		}
		
		private IQ doIq(Packet request) throws XMPPException {
			PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(request.getPacketID()));
			connection.sendPacket(request);
			IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
			if (response == null) {
				throw new XMPPException("No response from server to request: " + request.toXML());
			} else if (response.getType() == IQ.Type.ERROR) {
				throw new XMPPException(response.getError());
			}
			collector.cancel();
			return response;
		}
		
		public IQ sendGetAuthentication() throws XMPPException {
			// Ask what is supported, though we don't care, 
			// the server seems to like it
	        Authentication askAuth = new Authentication();
	        askAuth.setType(IQ.Type.GET);
	        askAuth.setUsername(jiveUser);
			
	        return doIq(askAuth);
		}
	
		public IQ sendDigestAuthentication() throws XMPPException {
			// Hardcode use of digest auth; we only work with our own server
			
			Authentication auth = new Authentication();
			auth.setUsername(jiveUser);
			
			Digest digest = new Digest();
			auth.setDigest(digest.computeDigest(connection.getConnectionID(), authKey));
			
			auth.setResource("hungry");
			
			return doIq(auth);
		}

		public void sendAvailable() {
			connection.sendPacket(new Presence(Presence.Type.AVAILABLE));
		}
		
		public boolean isConnected() {
			return connection != null && connection.isConnected();
		}

		public void processPacket(Packet packet) {
			//System.out.println("Got a packet " + packet.toXML());
	
			if (packet instanceof IQ) {
				return;
			}
			
			put(packet);
		}

		public void connectionClosed() {
			//System.out.println("Jabber connection closed");
			connection = null;
			put(null); // wake everyone up
		}

		public void connectionClosedOnError(Exception e) {
			System.out.println("Jabber connection closed on error: " + e.getMessage());
			connection = null;
			put(null); // for wakeup
		}
	}
	
	private static String xmlEscape(String text) {
		StringBuilder sb = new StringBuilder(text.length());
		for (char c : text.toCharArray()){
			if (c == '&')
				sb.append("&amp;");
			else if (c == '<')
				sb.append("&lt;");
			else if (c == '>')
				sb.append("&gt;");
			else if (c == '\'')
				sb.append("&#39;"); // &apos; is valid XML but not valid HTML
			else if (c == '"')
				sb.append("&quot;");
			else
				sb.append(c);
		}
		return sb.toString();
	}

	public static boolean packetContains(Packet p, String text) {
		String xml = p.toXML();
		return xml.contains(xmlEscape(text));
	}
	
	public static void main(String[] args) {
		// note that this is "destructive" in that it can have side effects
		// on the database
		JabberClient c = new JabberClient(CheatSheet.getReadOnly().getOneSampleUserId());
		c.login();
		while (c.isConnected()) {
			/* Packet p = */ c.take();
			System.out.println("...");
		}
		System.out.println("Disconnected");
	}
}
