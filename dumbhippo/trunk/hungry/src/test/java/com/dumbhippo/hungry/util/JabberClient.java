/**
 * 
 */
package com.dumbhippo.hungry.util;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Authentication;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

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
	
	public JabberClient(String userId, String authKey) {
		XMPPConnection.DEBUG_ENABLED = true;
		this.userId = userId;
		this.authKey = authKey;
	
		jiveUser = guidToJiveUserName(userId);
		
		Config config = Config.getDefault();
		host = config.getValue(ConfigValue.JIVE_HOST);
		port = config.getIntValue(ConfigValue.JIVE_PORT);
	}
	
	public void login() {
		Runnable r = new JabberThread();
		Thread thread = new Thread(r);
		thread.setName("jabber " + jiveUser);
		thread.start();
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
	
	private class JabberThread implements Runnable, PacketListener {
		
		private XMPPConnection connection;
		private boolean authorized = false;
		
		public JabberThread() {
		}
		
		public void run() {
			connection = loginToJabber();
			
			while (connection.isConnected()) {
				try {
					synchronized (this) {
						// Smack is supposed to wake us up, 
						// but timeout for paranoia (Smack has its 
						// own thread...)
						wait(2000);
					}
				} catch (InterruptedException e) {
					// ignore
				}
			}
			
			connection = null;
			
			System.out.println("Disconnected Jabber thread");
		}
		
		private XMPPConnection loginToJabber() {
			
			System.out.println("Smack login timeout " + SmackConfiguration.getPacketReplyTimeout());
			
			XMPPConnection connection;
			try { 
				connection = new XMPPConnection(host, port);
				
				// FIXME change this whole mess to use PacketCollector
				
				connection.addPacketListener(this, null);
			
				// Ask what is supported, though we don't care, 
				// the server seems to like it
		        Authentication askAuth = new Authentication();
		        askAuth.setType(IQ.Type.GET);
		        askAuth.setUsername(jiveUser);
				
				connection.sendPacket(askAuth);
				
				// FIXME wait for reply here...
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				
				// Hardcode use of digest auth; we only work with our own server
				
				Authentication auth = new Authentication();
				auth.setUsername(jiveUser);
				
				Digest digest = new Digest();
				auth.setDigest(digest.computeDigest(connection.getConnectionID(), authKey));
				
				auth.setResource("hungry");
				
				connection.sendPacket(auth);
		
				while (!authorized && connection.isConnected()) {
					try {
						synchronized (this) {
							wait(2000);
						}
					} catch (InterruptedException e) {
						// ignore
					}
				}				
				
			} catch (XMPPException e) {
				e.printStackTrace();
				throw new Error("Could not login as guid " + userId + " jid " + jiveUser, e);
			}
		
			System.out.println("Successfully sent login as " + jiveUser);
			return connection;
		}

		public void processPacket(Packet packet) {
			System.out.println("Got a packet " + packet.toXML());
			
			if (packet instanceof IQ) {
				IQ iq = (IQ) packet;
				if (iq.getType() == IQ.Type.ERROR)
					System.err.println(iq.getError());
				
				if (iq instanceof Authentication &&
						iq.getType() == IQ.Type.RESULT) {
					System.out.println("We're probably authorized now");
					authorized = true;
				}
			}
			
			if (connection != null && connection.isAuthenticated())
				connection.close();
			
			synchronized (this) {
				notifyAll();
			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println("does nothing");
	}
}
