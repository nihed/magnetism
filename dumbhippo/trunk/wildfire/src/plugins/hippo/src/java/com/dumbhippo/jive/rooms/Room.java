package com.dumbhippo.jive.rooms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.ObjectMessage;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.PacketError.Condition;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatRoomKind;
import com.dumbhippo.server.ChatRoomMessage;
import com.dumbhippo.server.ChatRoomUser;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.xmppcom.XmppEvent;
import com.dumbhippo.xmppcom.XmppEventChatMessage;

public class Room {
	private RoomHandler handler;
	
	static final int MAX_HISTORY_COUNT = 100;
	
	private static class UserInfo {
		private String username;
		private String name;
		private String smallPhotoUrl;		
		private String arrangementName;
		private String artist;
		private boolean musicPlaying;
		private int participantCount;
		private int presentCount;
		
		
		public UserInfo(String username, String name, String smallPhotoUrl) {
			this.username = username;
			this.name = name;
			this.smallPhotoUrl = smallPhotoUrl;			
			this.arrangementName = "";
			this.artist = "";
			this.musicPlaying = false;
		}
		
		public String getUsername() {
			return username;
		}
		
		public String getName() {
			return name;
		}
		
		public String getSmallPhotoUrl() {
			return smallPhotoUrl;
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
		
		public String getArrangementName() {
			return arrangementName;
		}
		
		public void setArrangementName(String arrangementName) {
			if (arrangementName == null) {
				this.arrangementName = "";
			} else {
			    this.arrangementName = arrangementName;

			}
		}
		
		public String getArtist() {
			return artist;
		}
		
		public void setArtist(String artist) {
			if (artist == null) {
				this.artist = "";
			} else {
			    this.artist = artist;
			}
		}
		
		public boolean isMusicPlaying() {
			return musicPlaying;
		}
		
		public void setMusicPlaying(boolean musicPlaying) {
			    this.musicPlaying = musicPlaying;
		}
		
		public RoomUserStatus getStatus() {
			if (presentCount == 0)
				return RoomUserStatus.NONMEMBER;
			else if (participantCount == 0)
				return RoomUserStatus.VISITOR;
			else
				return RoomUserStatus.PARTICIPANT;
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

	private Set<String> recipientsCache;
	private Map<String, UserInfo> userInfoCache;
	private Map<JID, UserInfo> participantResources;
	private Map<JID, UserInfo> presentResources;
	private Map<String, UserInfo> presentUsers;
	private List<MessageInfo> messages;
	private int nextSerial;
	
	private String roomName;
	
	private JmsProducer queue;
	
	private ChatRoomKind kind;

	private String title;
	
	private Room(RoomHandler handler, ChatRoomInfo info) {
		this.handler = handler; 
		queue = new JmsProducer(XmppEvent.QUEUE, false);
		
		userInfoCache = new HashMap<String, UserInfo>();
		participantResources = new HashMap<JID, UserInfo>();
		presentResources = new HashMap<JID, UserInfo>();
		presentUsers = new HashMap<String, UserInfo>();
		messages = new ArrayList<MessageInfo>();
		nextSerial = 0;
		
		this.roomName = info.getChatId();
		this.kind = info.getKind();
		this.title = info.getTitle();
		
		for (ChatRoomMessage message : info.getHistory()) {
			UserInfo userInfo = lookupUserInfo(message.getFromUsername());
			MessageInfo messageInfo = new MessageInfo(userInfo, message.getText(), message.getTimestamp(), message.getSerial());
			messages.add(messageInfo);
			
			if (message.getSerial() >= nextSerial)
				nextSerial = message.getSerial() + 1;
		}
	}
	
	private UserInfo lookupUserInfo(String username) {
		if (!userInfoCache.containsKey(username)) {
			MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
			ChatRoomUser user = glue.getChatRoomUser(roomName, kind, username);
			userInfoCache.put(username, new UserInfo(user.getUsername(), user.getName(), user.getSmallPhotoUrl()));			
		}
		return userInfoCache.get(username);
	}
	
	private void updateRecipientsCache() {
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);		
		for (ChatRoomUser user : glue.getChatRoomRecipients(roomName, this.kind)) {
			Log.debug("Room recipient: " + user.getUsername());
			recipientsCache.add(user.getUsername());
		}					
	}
		
	private Set<String> getRecipientsCache() {
		if (recipientsCache == null) {
			recipientsCache = new HashSet<String>();
			updateRecipientsCache();
		}
		return recipientsCache;
	}
	
	private String getServiceDomain() {
		return "rooms." + XMPPServer.getInstance().getServerInfo().getName();
	}

	/**
	 * Send a packet to a particular resource
	 * 
	 * @param packet packet to send
	 * @param to recipient resource
	 */
	private void sendPacketToResource(Packet packet, JID to) {
		packet.setTo(to);
		XMPPServer.getInstance().getPacketRouter().route(packet);		
	}

	/**
	 * Send a packet to everybody who either received the post initially
	 * or is in the chatroom. The packet is sent
	 * only to resources that are currently logged in to the server; it
	 * will not be queued for offline delivery.
	 * 
	 * For group chat, right now this would send to everyone in the group.
	 * 
	 * @param packet the packet to send
	 */ 
	private void sendPacketToAll(Packet packet) {
		Set<String> targets = new HashSet<String>();
		targets.addAll(getRecipientsCache());
		for (UserInfo userInfo : presentUsers.values()) {
			targets.add(userInfo.getUsername());
		}
		for (String userName : targets) {
			try {
				SessionManager.getInstance().userBroadcast(userName, packet);
			} catch (UnauthorizedException e) {
				// Ignore
			}
		}		
	}
	
	/**
	 * Send a packet to everybody currently in the chatroom. 
	 * 
	 * @param packet the packet to send
	 */
	private void sendPacketToPresent(Packet packet) {
		for (JID member : presentResources.keySet()) {
			sendPacketToResource(packet, member);
		}
	}
	
	public static Room loadFromServer(RoomHandler handler, String roomName) {
		Log.debug("Querying server for information on chat room " + roomName);
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		
		ChatRoomInfo info = glue.getChatRoomInfo(roomName);
		if (info == null) {
			Log.debug("  no such room");
			return null;
		}

		Log.debug("  got response from server, title is " + info.getTitle());
		
		return new Room(handler, info);
	}
	
	public static Map<String, String> getCurrentMusicFromServer(String username) {
		Log.debug("Querying server for current music for " + username);
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		
		Map<String, String> musicInfo = glue.getCurrentMusicInfo(username);
		
		if (musicInfo == null) {
			Log.debug("  user has no music info");
			return null;
		}

		Log.debug("  got response from server with the music info");
		
		return musicInfo;
		
	}
	
	private String roleString(RoomUserStatus status) {
		switch (status) {
		case NONMEMBER:
			return "nonmember";
		case VISITOR:
			return "visitor";
		case PARTICIPANT:
			return "participant";
		}
		
		return null; // Not reached
	}
	
	private void addRoomInfo(Packet outgoing, boolean includeDetails, String viewpointGuid) {
		Element messageElement = outgoing.getElement();
		Element roomInfo = messageElement.addElement("roomInfo", "http://dumbhippo.com/protocol/rooms");
		roomInfo.addAttribute("kind", kind.name().toLowerCase());
		if (includeDetails)
			roomInfo.addAttribute("title", title);
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		String objectXml = null;
		if (includeDetails) {
			String elementName = "objects";
			
			if (kind == ChatRoomKind.GROUP) {
				try {			
					objectXml = glue.getGroupXML(Guid.parseTrustedJabberId(viewpointGuid), 
							                     Guid.parseTrustedJabberId(roomName));
				} catch (NotFoundException e) {
					Log.error("failed to find group", e);				
				}				
			} else if (kind == ChatRoomKind.POST) {
			       objectXml = glue.getPostsXML(Guid.parseTrustedJabberId(viewpointGuid), 
			    		                        Guid.parseTrustedJabberId(roomName), 
			    		                        elementName);
			}
			if (objectXml != null) {
				Document xmlDumpDoc;
				try {
					xmlDumpDoc = DocumentHelper.parseText(objectXml);
				} catch (DocumentException e) {
					throw new RuntimeException("Couldn't parse result for an object associated with a room");
				}
				
				Element childElement = xmlDumpDoc.getRootElement();
				childElement.detach();
				
				if (kind == ChatRoomKind.GROUP) { 
				    Element objectElt = roomInfo.addElement(elementName);
				    objectElt.add(childElement);
				} else if (kind == ChatRoomKind.POST) {
					roomInfo.add(childElement);
				}
			}
		}
	}
	
	private Presence makePresenceAvailable(UserInfo userInfo, RoomUserStatus oldStatus) {
		Presence presence = new Presence((Presence.Type)null);
		presence.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		
		Log.debug("Sending presence available from " + presence.getFrom() + " to " + presence.getTo());
		
		// Sending the (human-readable) name of the user here is a significant optimization, 
		// since otherwise the client would have to query it from the user before
		// displaying it
		Element child = presence.addChildElement("x", "http://jabber.org/protocol/muc#user");
		Element info = child.addElement("userInfo", "http://dumbhippo.com/protocol/rooms");
		info.addAttribute("name", userInfo.getName());
		info.addAttribute("smallPhotoUrl", userInfo.getSmallPhotoUrl());
		info.addAttribute("role", roleString(userInfo.getStatus()));
		if (oldStatus != null) {
			info.addAttribute("oldRole", roleString(oldStatus));
		}
		info.addAttribute("arrangementName", userInfo.getArrangementName());
		info.addAttribute("artist", userInfo.getArtist()); 
		info.addAttribute("musicPlaying", Boolean.toString(userInfo.isMusicPlaying()));

		addRoomInfo(presence, false, null);
		
		return presence;
	}
	
	private Presence makePresenceUnavailable(UserInfo userInfo) {
		Presence presence = new Presence(Presence.Type.unavailable);
		presence.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		
		addRoomInfo(presence, false, null);
		
		return presence;
	}
	
	private void sendRoomDetails(boolean excludeSelf, JID to) {
		// Send the list of current members
		for (UserInfo memberInfo : presentUsers.values()) {
			if (!(excludeSelf && memberInfo.getUsername().equals(to.getNode()))) {
				Presence presence = makePresenceAvailable(memberInfo, null);
				sendPacketToResource(presence, to);
			}
		}
		
		// And a a history of recent messages
		int count = messages.size();
		if (count > MAX_HISTORY_COUNT)
			count = MAX_HISTORY_COUNT;
		
		for (int i = messages.size() - count; i < messages.size(); i++) {
			MessageInfo messageInfo = messages.get(i);
			Message message = makeMessage(messageInfo, true);
			sendPacketToResource(message, to);
		}
	}
	
	private void processPresenceAvailable(Presence packet) {
		JID jid = packet.getFrom();
		String username = jid.getNode();

		// The resource passed in this presence message is the requested
		// nickname in the groupchat protocol, but we ignore it and just
		// make them use their userid as their nickname; we'll cover
		// it up with the real name in the client.
		Log.debug("Got available presence from : " + jid);

		UserInfo userInfo = lookupUserInfo(username);
		
		// Look for our userInfo tag which will distinguish whether the
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
			// we only care about the current music selection when a resource becomes
			// a participant, so get the current music in this case
			Map<String, String> properties = getCurrentMusicFromServer(username);
            if (properties != null) {
			    userInfo.setArrangementName(properties.get("name"));
			    userInfo.setArtist(properties.get("artist"));
			    if (properties.get("musicPlaying") != null) {
			        userInfo.setMusicPlaying(properties.get("musicPlaying").equals("true"));
			    }
            }
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
		
		// Joining a chat implicitly un-ignores a post
		if (participant && kind == ChatRoomKind.POST) {
			MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
			try {
				glue.setPostIgnored(Guid.parseTrustedJabberId(username), 
								    Guid.parseTrustedJabberId(roomName), false);
			} catch (NotFoundException e) {
				Log.error(e);								
			} catch (ParseException e) {
				Log.error(e);
			}					
		}		

		if (needNotify) {
			if (resourceWasPresent)
				handler.getPresenceMonitor().onRoomUserUnavailable(kind, roomName, username);
			handler.getPresenceMonitor().onRoomUserAvailable(kind, roomName, username, userInfo.getStatus());
			RoomUserStatus oldStatus;
			if (resourceWasParticipant)
				oldStatus = RoomUserStatus.PARTICIPANT;
			else if (resourceWasPresent)
				oldStatus = RoomUserStatus.VISITOR;
			else
				oldStatus = RoomUserStatus.NONMEMBER;
			
			Presence presence = makePresenceAvailable(userInfo, oldStatus);
			sendPacketToAll(presence);
		}
		
		// Send the list of current membmers and a complete history of past messages
		if (!resourceWasPresent)
			sendRoomDetails(needNotify, jid);
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
			handler.getPresenceMonitor().onRoomUserUnavailable(kind, roomName, username);
			if (userInfo.getStatus() != RoomUserStatus.NONMEMBER)
				handler.getPresenceMonitor().onRoomUserAvailable(kind, roomName, username, userInfo.getStatus());
			
			Presence presence;
			if (userInfo.getStatus() == RoomUserStatus.NONMEMBER)
				presence = makePresenceUnavailable(userInfo);
			else
				presence = makePresenceAvailable(userInfo, RoomUserStatus.PARTICIPANT);
			sendPacketToAll(presence);
		}
	}
	
	private Message makeMessage(MessageInfo messageInfo, boolean isDelayed) {
		UserInfo userInfo = messageInfo.getUser(); 
		
		Message outgoing = new Message();
		outgoing.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		outgoing.setBody(messageInfo.getText());
		
		Element messageElement = outgoing.getElement();
		Element info = messageElement.addElement("messageInfo", "http://dumbhippo.com/protocol/rooms");
		info.addAttribute("name", userInfo.getName());
		info.addAttribute("smallPhotoUrl", userInfo.getSmallPhotoUrl());
		info.addAttribute("timestamp", Long.toString(messageInfo.getTimestamp().getTime()));
		info.addAttribute("serial", Integer.toString(messageInfo.getSerial()));

		if (isDelayed) {
			Element delay = messageElement.addElement("x", "jabber:x:delay");
			delay.addAttribute("from", outgoing.getFrom().toString());
			// This isn't in the Jabber format and we're too lazy to fix it 
			// delay.addAttribute("stamp", info.attributeValue("timestamp"));
		}
		
		addRoomInfo(outgoing, false, null);
		
		return outgoing;
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
		UserInfo userInfo = lookupUserInfo(username);
		
		Date timestamp = new Date(); // Current time
		int serial = nextSerial++;
		
		MessageInfo messageInfo = new MessageInfo(userInfo, packet.getBody(), timestamp, serial);
		messages.add(messageInfo);
		
		Message outgoing = makeMessage(messageInfo, false);
		sendPacketToAll(outgoing);
		
		// Send over to the server via JMS
		XmppEventChatMessage event = new XmppEventChatMessage(roomName, kind, messageInfo.getUser().getUsername(), messageInfo.getText(), messageInfo.getTimestamp(), messageInfo.getSerial());
        ObjectMessage message = queue.createObjectMessage(event);
        queue.send(message);
	}
	
	private void processIQPacket(IQ packet) {
		JID fromJid = packet.getFrom();
		
		IQ reply = IQ.createResultIQ(packet);
	
		Element child = packet.getChildElement();
		
		if (child == null) {
			reply.setError(Condition.bad_request);
		} else if (packet.getType() == IQ.Type.get &&
			child.getNamespaceURI().equals("http://dumbhippo.com/protocol/rooms") &&
		    child.getName().equals("details")) {
			
			// This is somewhat of an abuse of the IQ system; the "details"
			// IQ is a request for the full membership list and history of
			// the room. Instead of packing that information as a child
			// elements of the result IQ, which would require extending
			// the XML schema and adding more parsing code on the client
			// side, we simply send all that information as <presence/> and
			// <message/> elements, *then* we send the result of the IQ
			// to indicate that we are finished.
			sendRoomDetails(false, fromJid);
		
			addRoomInfo(reply, true, fromJid.getNode());
		} else {
			reply.setError(Condition.feature_not_implemented);
		}
		
		XMPPServer.getInstance().getPacketRouter().route(reply);
	}

	/**
	 * @param iq an Element containing all the information about a new track
	 * @param propeties a map containing all the information about a new track
	 * @param username username of a person whose music has changed
	 */
	public void processMusicChange(Element iq, Map<String,String> properties, String username) {
		// update UserInfo for username
		UserInfo userInfo = presentUsers.get(username);
		if (userInfo == null)
			throw new RuntimeException("User " + username + " isn't in presentUsers");
        // if name and artist are not set in the element, it means that we got a message
        // about the music having stopped, preserve the information about the last played 
        // arrangement and set the nowPlaying flag to false  
		if ((properties.get("name") == null) && (properties.get("artist") == null)) {
			userInfo.setMusicPlaying(false);
		} else {
		    userInfo.setArrangementName(properties.get("name"));
		    userInfo.setArtist(properties.get("artist"));
			userInfo.setMusicPlaying(true);		    
		}
		
		// To communicate the change in music, we send a new Presence message
		// We send it only to users currently in the chatroom, not to everybody,
		// to cut down on traffic.
		Presence presence = makePresenceAvailable(userInfo, null);
		sendPacketToPresent(presence);
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
		} else if (packet instanceof IQ) {
			processIQPacket((IQ)packet);
		} else
			throw new NotImplementedException();
	}
	
	/**
	 * Check if user can join a particular room and send messages
	 * to it. This is the same as being able to view the 
	 * post that the room is about or being in the group chatted about
	 * or whatever is appropriate. 
	 * 
	 * @param username
	 * @return true if the user can join this room
	 */
	public boolean checkUserCanJoin(String username) {
		MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
		return glue.canJoinChat(roomName, kind, username);
	}

	/**
	 * Returns the count of present users.
	 * 
	 * @return the count of present users
	 */
	public int getPresenceCount() {
		return presentUsers.size(); 
	}
	
	/**
	 * Check if user is present in this room.
	 * 
	 * @param username
	 * @return true if user is present in this room
	 */
	public boolean checkUserPresent(String username) {
	    return presentUsers.containsKey(username);	
	}

	public void reloadCaches() {
		updateRecipientsCache();
	}
}
