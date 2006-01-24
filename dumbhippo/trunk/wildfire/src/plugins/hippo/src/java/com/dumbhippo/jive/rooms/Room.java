package com.dumbhippo.jive.rooms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.ObjectMessage;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.XMPPServer;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.MessengerGlueRemote.ChatRoomInfo;
import com.dumbhippo.server.MessengerGlueRemote.ChatRoomMessage;
import com.dumbhippo.server.MessengerGlueRemote.ChatRoomUser;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.xmppcom.XmppEvent;
import com.dumbhippo.xmppcom.XmppEventChatMessage;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Room {
	private static class UserInfo {
		private String username;
		private String name;
		private int version;
		private int participantCount;
		private int presentCount;
		
		public UserInfo(String username, int version, String name) {
			this.username = username;
			this.version = version;
			this.name = name;
		}
		
		public String getUsername() {
			return username;
		}
		
		public int getVersion() {
			return version;
		}
		
		public String getName() {
			return name;
		}
		
		public int getParticipantCount() {
			return participantCount;
		}

		public void setParticipantCount(int participantCount) {
			this.participantCount = participantCount;
		}

		public int getPresentCount() {
			return presentCount;
		}
		
		public void setPresentCount(int presentCount) {
			this.presentCount = presentCount; 
		}
	}
	
	private static class MessageInfo {
		private UserInfo user;
		private String text;
		private Date timestamp;
		private int serial;
		
		public MessageInfo(UserInfo user, String text, Date timestamp, int serial) {
			this.user = user;
			this.text = text;
			this.timestamp = timestamp;
			this.serial = serial;
		}
		
		public UserInfo getUser() {
			return user;
		}
		
		public int getSerial() {
			return serial;
		}
		
		public String getText() {
			return text;
		}
		
		public Date getTimestamp() {
			return timestamp;
		}
	}

	private Map<String, UserInfo> allowedUsers;
	private Map<JID, UserInfo> participantResources;
	private Map<JID, UserInfo> presentResources;
	private Map<String, UserInfo> presentUsers;
	private List<MessageInfo> messages;
	private int nextSerial;
	
	private String roomName;
	
	private JmsProducer queue;

	private Room(ChatRoomInfo info) {
		queue = new JmsProducer(XmppEvent.QUEUE, false);
		
		allowedUsers = new HashMap<String, UserInfo>();
		participantResources = new HashMap<JID, UserInfo>();
		presentResources = new HashMap<JID, UserInfo>();
		presentUsers = new HashMap<String, UserInfo>();
		messages = new ArrayList<MessageInfo>();
		
		this.roomName = info.getPostId();
		for (ChatRoomUser user : info.getAllowedUsers()) {
			Log.debug("Allowed user: " + user.getUsername());
			allowedUsers.put(user.getUsername(), 
						     new UserInfo(user.getUsername(), user.getVersion(), user.getName()));
		}
		
		for (ChatRoomMessage message : info.getHistory()) {
			UserInfo userInfo = allowedUsers.get(message.getFromUsername());
			if (userInfo == null) {
				Log.debug("Ignoring message from unknown user " + message.getFromUsername());
				continue;
			}
			MessageInfo messageInfo = new MessageInfo(userInfo, message.getText(), message.getTimestamp(), message.getSerial());
			messages.add(messageInfo);
			
			if (message.getSerial() >= nextSerial)
				nextSerial = message.getSerial() + 1;
		}
	}
	
	private String getServiceDomain() {
		return "rooms." + XMPPServer.getInstance().getServerInfo().getName();
	}

	public static Room loadFromServer(String roomName, String username) {
		Log.debug("Querying server for information on post " + roomName);
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		
		ChatRoomInfo info = glue.getChatRoomInfo(roomName, username);
		if (info == null) {
			Log.debug("  no such room");
		}

		Log.debug("  got response from server, title is " + info.getPostTitle());
		
		return new Room(info);
	}
	
	private void sendPresenceAvailable(JID to, UserInfo userInfo) {
		Presence presence = new Presence((Presence.Type)null);
		presence.setTo(to);
		presence.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		
		Log.debug("Sending presence available from " + presence.getFrom() + " to " + presence.getTo());
		
		// Sending the (human-readable) name of the user here is a significant optimization, 
		// since otherwise the client would have to query it from the user before
		// displaying it
		Element child = presence.addChildElement("x", "http://jabber.org/protocol/muc#user");
		Element info = child.addElement("userInfo", "http://dumbhippo.com/protocol/rooms");
		info.addAttribute("name", userInfo.getName());
		info.addAttribute("version", Integer.toString(userInfo.getVersion()));
		info.addAttribute("role", userInfo.getParticipantCount() > 0 ? "participant" : "visitor");
		
		XMPPServer.getInstance().getPacketRouter().route(presence);
	}
	
	private void sendPresenceUnavailable(JID to, UserInfo userInfo) {
		Presence presence = new Presence(Presence.Type.unavailable);
		presence.setTo(to);
		presence.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		
		Log.debug("Sending presence unavailable from " + presence.getFrom() + " to " + presence.getTo());
		
		XMPPServer.getInstance().getPacketRouter().route(presence);
	}
	
	private void processPresenceAvailable(Presence packet) {
		JID jid = packet.getFrom();
		String username = jid.getNode();

		// The resource passed in this presence message is the requested
		// nickname in the groupchat protocol, but we ignore it and just
		// make them use their userid as their nickname; we'll cover
		// it up with the real name in the client.
		Log.debug("Got available presence from : " + jid);

		UserInfo userInfo = allowedUsers.get(username);
		if (userInfo == null) {
			throw new RuntimeException("User " + username + " isn't in allowedUsers");
		}
		
		// Look for our userInfo tag which will distinguish whethr the
		// user wants to join as a 'visitor' or a 'participant'
		boolean participant = true;
		Element xElement = packet.getChildElement("x", "http://jabber.org/protocol/muc");
		if (xElement != null) {
	        for (Iterator i = xElement.elementIterator("userInfo"); i.hasNext(); ) {
	            Element element = (Element)i.next();
	            if (element.getNamespaceURI().equals("http://dumbhippo.com/protocol/rooms")) {
	            	Attribute attr = element.attribute("role");
	            	if (attr != null)
	            		participant = attr.getText().equals("participant");
	            }
	        }			
		}
		
		boolean resourceWasPresent = (presentResources.get(jid) != null);
		boolean resourceWasParticipant = (participantResources.get(jid) != null);
		
		boolean needNotify = false; // Do we need to broadcast a change to other users

		if (!resourceWasPresent) {
			presentResources.put(jid, userInfo);
			userInfo.setPresentCount(userInfo.getPresentCount() + 1);
		
			// User is joining the channel for the first time
			if (userInfo.getPresentCount() == 1) {
				presentUsers.put(username, userInfo);
				needNotify = true;
			}
		}

		if (participant && !resourceWasParticipant) {
			userInfo.setParticipantCount(userInfo.getParticipantCount() + 1);
			if (userInfo.getParticipantCount() == 1) {
				participantResources.put(jid, userInfo);
				needNotify = true;
			}
		} else if (!participant && resourceWasParticipant) {
			userInfo.setParticipantCount(userInfo.getParticipantCount() - 1);
			if (userInfo.getParticipantCount() == 0) {
				participantResources.remove(jid);
				needNotify = true;
			}
		}

		if (needNotify) {
			for (JID member : presentResources.keySet()) {
				sendPresenceAvailable(member, userInfo);
			}
		}
		
		if (!resourceWasPresent) {
			// Send the newly joined resource the list of current members
			for (UserInfo memberInfo : presentUsers.values()) {
				 // Skip this user if we already notified them
				if (needNotify && !memberInfo.getUsername().equals(username))
					sendPresenceAvailable(jid, memberInfo);
			}
			
			// And a complete history of past messages
			for (MessageInfo messageInfo : messages) {
				Message outgoing = makeMessage(messageInfo);
				sendMessage(outgoing, jid);
			}
		}	
	}
	
	private void processPresenceUnavailable(Presence packet) {
		JID jid = packet.getFrom();
		String username = jid.getNode();
		
		Log.debug("Got unavailable presence from : " + jid);

		UserInfo userInfo = presentResources.get(jid);
		if (userInfo == null)
			return; // Not present, nothing to do
			
		boolean needNotify = false; // Do we need to broadcast a change to other users

		presentResources.remove(jid);
		userInfo.setPresentCount(userInfo.getPresentCount() - 1);
		if (userInfo.getPresentCount() == 0) {
			presentUsers.remove(username);
			needNotify = true;
		}
		
		if (participantResources.get(jid) != null) {
			participantResources.remove(jid);
			userInfo.setParticipantCount(userInfo.getParticipantCount() - 1);
			if (userInfo.getParticipantCount() == 0)
				needNotify = true;
		}

		if (needNotify) {
			for (JID member : presentResources.keySet()) {
				if (userInfo.getPresentCount() > 0)
					sendPresenceAvailable(member, userInfo);
				else
					sendPresenceUnavailable(member, userInfo);
			}
		}
	}
	
	private Message makeMessage(MessageInfo messageInfo) {
		UserInfo userInfo = messageInfo.getUser(); 
		
		Message outgoing = new Message();
		outgoing.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		outgoing.setBody(messageInfo.getText());
		
		Element messageElement = outgoing.getElement();
		Element info = messageElement.addElement("messageInfo", "http://dumbhippo.com/protocol/rooms");
		info.addAttribute("name", userInfo.getName());
		info.addAttribute("version", Integer.toString(userInfo.getVersion()));
		info.addAttribute("timestamp", Long.toString(messageInfo.getTimestamp().getTime()));
		info.addAttribute("serial", Integer.toString(messageInfo.getSerial()));
		
		return outgoing;
	}
	
	private void sendMessage(Message outgoing, JID to) {
		Log.debug("  forwarding message to: " + to);

		outgoing.setTo(to);
		XMPPServer.getInstance().getPacketRouter().route(outgoing);		
	}
	
	private void processMessagePacket(Message packet) {
		JID fromJid = packet.getFrom();
		JID toJid = packet.getTo();
		
		Log.debug("Got message from: " + fromJid);

		// Only handle messages that are sent directly to the room, for broadcast
		// to all present members; we don't support private messages
		if (toJid.getResource() != null)
			return;
		
		String username = fromJid.getNode();
		UserInfo userInfo = allowedUsers.get(username);
		if (userInfo == null)
			throw new RuntimeException("User " + username + " isn't in allowedUsers");
		
		Date timestamp = new Date(); // Current time
		int serial = nextSerial++;
		
		MessageInfo messageInfo = new MessageInfo(userInfo, packet.getBody(), timestamp, serial);
		messages.add(messageInfo);
		
		Message outgoing = makeMessage(messageInfo);
		for (JID member : presentResources.keySet())
			sendMessage(outgoing, member);
		
		// Send over to the server via JMS
		XmppEventChatMessage event = new XmppEventChatMessage(roomName, messageInfo.getUser().getUsername(), messageInfo.getText(), messageInfo.getTimestamp(), messageInfo.getSerial());
        ObjectMessage message = queue.createObjectMessage(event);
        queue.send(message);
	}
	
	/**
	 * Process a packet sent to the room; it has already been checked
	 * that the user given by packet.getFrom().getNode() is a member
	 * of our system and is authorized to join the room.
	 * 
	 * @param packet a packet sent to the room
	 */
	public void processPacket(Packet packet) {
		if (packet instanceof Presence) {
			Presence presence = (Presence)packet;
			if (presence.isAvailable())
				processPresenceAvailable(presence);
			else if (presence.getType() == Presence.Type.unavailable)
				processPresenceUnavailable(presence);
		} else if (packet instanceof Message) {
			processMessagePacket((Message)packet);
		} else
			throw new NotImplementedException();
	}
	
	/**
	 * Check if user can join a particular room and send messages
	 *    to it. This is the same as being able to view the 
	 *    post that the room is about. 
	 * 
	 * @param username
	 * @return true if the user can join this room
	 */
	public boolean userCanJoin(String username) {
		return allowedUsers.containsKey(username);
	}
}
