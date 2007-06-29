package com.dumbhippo.jive.rooms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.XMPPServer;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;
import org.xmpp.packet.PacketError.Condition;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.MessageSender;
import com.dumbhippo.jive.XmlParser;
import com.dumbhippo.live.PresenceListener;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatRoomKind;
import com.dumbhippo.server.ChatRoomMessage;
import com.dumbhippo.server.ChatRoomUser;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.util.EJBUtil;

public class Room implements PresenceListener {
	static final int MAX_HISTORY_COUNT = 100;
	
	/* NOTES: Concurrency and Thread Safety
	 *
	 * Packets from a client are delivered in the thread that reads from that
	 * client's socket. In addition, our PresenceListener is called from an internal
	 * thread of PresenceServer and reloadCaches() can be called the HippoPlugin JMS
	 * consumer thread. Rather than trying sort out which of all these accesses affect
	 * which of our data structures, we take the simple approach of synchronizing all
	 * public methods (except for ones that clearly don't access mutable data structures).
	 * 
	 * The one concern about this is it does mean holding a global lock while calling
	 * MessengerGlue methods such as cgetChatRoomUser(), which may do database accesses,
	 * but doing better would take a lot of careful work.
	 * 
	 * The one thing we handle specially is outgoing messages; if we delivered them
	 * directly from within our lock, we could block for arbitrary amounts of time 
	 * with the lock held, on the other hand, delivering them unlocked would lose ordering.
	 * Delivering them via MessageSender provides the necessary asynchronicity and
	 * ordering guarantees. (The necessary ordering guarantee is that two packets queued 
	 * from the same chatroom to the same destination are delivered in that order.) 
	 **/ 
	
	private static class UserInfo {
		private String username;
		private String name;
		private String smallPhotoUrl;		
		private String arrangementName;
		private String artist;
		private boolean musicPlaying;
		private int participantCount;
		private int presentCount;
		private RoomUserStatus globalStatus;
		
		public UserInfo(String username, String name, String smallPhotoUrl) {
			this.username = username;
			this.name = name;
			this.smallPhotoUrl = smallPhotoUrl;			
			this.arrangementName = "";
			this.artist = "";
			this.musicPlaying = false;
			this.participantCount = 0;
			this.presentCount = 0;
			this.globalStatus = RoomUserStatus.NONMEMBER;
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
		
		public RoomUserStatus getLocalStatus() {
			if (presentCount == 0)
				return RoomUserStatus.NONMEMBER;
			else if (participantCount == 0)
				return RoomUserStatus.VISITOR;
			else
				return RoomUserStatus.PARTICIPANT;
		}
		
		public RoomUserStatus getGlobalStatus() {
			return globalStatus;
		}
		
		public void setGlobalStatus(RoomUserStatus status) {
			this.globalStatus = status;
		}

		public void setSmallPhotoUrl(String smallPhotoUrl) {
			this.smallPhotoUrl = smallPhotoUrl;
		}
	}
	
	private static class MessageInfo {
		private UserInfo user;
		private String text;
		private Date timestamp;
		private Sentiment sentiment;
		private long serial;
		
		public MessageInfo(UserInfo user, String text, Sentiment sentiment, Date timestamp, long serial) {
			this.user = user;
			this.text = text;
			this.timestamp = timestamp;
			this.sentiment = sentiment;
			this.serial = serial;
		}
		
		public UserInfo getUser() {
			return user;
		}
		
		public long getSerial() {
			return serial;
		}
		
		public String getText() {
			return text;
		}
		
		public Sentiment getSentiment() {
			return sentiment;
		}
		
		public Date getTimestamp() {
			return timestamp;
		}
	}
	
	private String title;
	private ChatRoomKind kind;
	private String roomName;
	private Guid roomGuid;
	
	private Map<String, UserInfo> userInfoCache;
	private Map<JID, UserInfo> participantResources;
	private Map<JID, UserInfo> presentResources;
	private Map<String, UserInfo> presentUsers;
	private List<MessageInfo> messages;
	private long maxMessageSerial = -1;
	
	private Room(ChatRoomInfo info) {
		userInfoCache = new HashMap<String, UserInfo>();
		participantResources = new HashMap<JID, UserInfo>();
		presentResources = new HashMap<JID, UserInfo>();
		presentUsers = new HashMap<String, UserInfo>();
		messages = new ArrayList<MessageInfo>();
		
		roomName = info.getChatId();
		roomGuid = Guid.parseTrustedJabberId(roomName);
		kind = info.getKind();
		title = info.getTitle();
		
		addMessages(info.getHistory(), false);
		
		PresenceService.getInstance().addListener(getPresenceLocation(), this);
	}
	
	private void addMessages(List<ChatRoomMessage> toAdd, boolean notify) {
		for (ChatRoomMessage message : toAdd) {
			UserInfo userInfo = lookupUserInfo(message.getFromUsername());
			MessageInfo messageInfo = new MessageInfo(userInfo, message.getText(), message.getSentiment(), message.getTimestamp(), message.getSerial());
			messages.add(messageInfo);
			if (messageInfo.getSerial() > maxMessageSerial)
				maxMessageSerial = messageInfo.getSerial();
			
			if (notify) {
				Message outgoing = makeMessage(messageInfo, false);
				sendPacketToAll(outgoing);
			}
		}
	}
	
	public void shutdown() {
		PresenceService.getInstance().removeListener(getPresenceLocation(), this);
	}
	
	private UserInfo lookupUserInfo(String username, boolean refresh) {
		UserInfo info;
		if (refresh || !userInfoCache.containsKey(username)) {
			MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
			ChatRoomUser user = glue.getChatRoomUser(roomName, kind, username);		
			if (!userInfoCache.containsKey(username)) {
				info = new UserInfo(user.getUsername(), user.getName(), user.getSmallPhotoUrl());
				userInfoCache.put(username, info);				
			} else {
				info = userInfoCache.get(username);		
			}
			// TODO - presently we only handle user photo changes in chat rooms
			// we probably also want to support nickname changes, although this
			// would likely require more extensive UI changes in order to not
			// be very confusing
			if (refresh) {
				info.setSmallPhotoUrl(user.getSmallPhotoUrl());
			}
			return info;
		} else {
			info = userInfoCache.get(username);
		}
		return info; 
	}
	
	private UserInfo lookupUserInfo(String username) {
		return lookupUserInfo(username, false);
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
		MessageSender.getInstance().sendPacketToResource(to, packet);
	}

	/**
	 * Send a packet to everybody is in the chatroom.
	 * 
	 * For group chat, right now this would send to everyone in the group.
	 * 
	 * @param packet the packet to send
	 */ 
	private void sendPacketToAll(Packet packet) {
		Set<Guid> targets = new HashSet<Guid>();
		for (UserInfo userInfo : presentUsers.values()) {
			targets.add(Guid.parseTrustedJabberId(userInfo.getUsername()));
		}
		
		MessageSender.getInstance().sendPacketToUsers(targets, packet);
	}
	
	/**
	 * Send a packet to everybody currently in the chatroom. 
	 * 
	 * @param packet the packet to send
	 */
	private void sendPacketToPresent(Packet packet) {
		MessageSender.getInstance().sendPacketToResources(presentResources.keySet(), packet);
	}
	
	public static Room loadFromServer(String roomName) {
		Log.debug("Querying server for information on chat room " + roomName);
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		
		ChatRoomInfo info = glue.getChatRoomInfo(roomName);
		if (info == null) {
			Log.debug("  no such room");
			return null;
		}

		Log.debug("  got response from server, title is " + info.getTitle());
		
		return new Room(info);
	}
	
	private static Map<String, String> getCurrentMusicFromServer(String username) {
		Log.debug("Querying server for current music for " + username);
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		
		Map<String, String> musicInfo = glue.getCurrentMusicInfo(username);
		
		if (musicInfo == null) {
			Log.debug("  user has no music info");
			return null;
		}

		Log.debug("  got response from server with the music info");
		
		return musicInfo;
		
	}
	
	private static String roleString(RoomUserStatus status) {
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
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		String objectXml = null;
		if (includeDetails) {
			String elementName = "objects";
			
			if (kind == ChatRoomKind.GROUP) {
				try {			
					objectXml = glue.getGroupXml(Guid.parseTrustedJabberId(viewpointGuid), 
							                     Guid.parseTrustedJabberId(roomName));
				} catch (NotFoundException e) {
					Log.error("failed to find group", e);				
				}				
			} else if (kind == ChatRoomKind.POST) {
			       objectXml = glue.getPostsXml(Guid.parseTrustedJabberId(viewpointGuid), 
			    		                        Guid.parseTrustedJabberId(roomName), 
			    		                        elementName);
			}
			if (objectXml != null) {
				Element childElement = XmlParser.elementFromXml(objectXml);
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
		info.addAttribute("role", roleString(userInfo.getGlobalStatus()));
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
	
	private void sendRoomDetails(JID to) {
		// Send the list of current members
		for (UserInfo memberInfo : presentUsers.values()) {
			Presence presence = makePresenceAvailable(memberInfo, null);
			sendPacketToResource(presence, to);
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
		
		boolean statusChanged = false;

		if (!resourceWasPresent) {
			presentResources.put(jid, userInfo);
			userInfo.setPresentCount(userInfo.getPresentCount() + 1);
		
			// User is joining the channel for the first time
			if (userInfo.getPresentCount() == 1) {
				statusChanged = true;
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
			participantResources.put(jid, userInfo);
			if (userInfo.getParticipantCount() == 1) {
				statusChanged = true;
			}			
		} else if (!participant && resourceWasParticipant) {
			userInfo.setParticipantCount(userInfo.getParticipantCount() - 1);
			participantResources.remove(jid);
			if (userInfo.getParticipantCount() == 0) {
				statusChanged = true;
			}
		}
		
		// Joining a chat implicitly un-ignores a post
		if (participant && kind == ChatRoomKind.POST) {
			MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
			try {
				glue.setPostIgnored(Guid.parseTrustedJabberId(username), 
								    Guid.parseTrustedJabberId(roomName), false);
			} catch (NotFoundException e) {
				Log.error(e);								
			} catch (ParseException e) {
				Log.error(e);
			}					
		}		

		if (statusChanged)
			setLocalPresence(userInfo);
		
		// Send the list of current membmers and a complete history of past messages
		// if the above caused a notification to be sent out then the user may get
		// notified of themself twice - that's harmless
		if (!resourceWasPresent)
			sendRoomDetails(jid);
	}
	
	private void processPresenceUnavailable(Presence packet) {
		JID jid = packet.getFrom();
		
		Log.debug("Got unavailable presence from : " + jid);

		UserInfo userInfo = presentResources.get(jid);
		if (userInfo == null)
			return; // Not present, nothing to do
			
		boolean statusChanged = false;

		presentResources.remove(jid);
		userInfo.setPresentCount(userInfo.getPresentCount() - 1);
		if (userInfo.getPresentCount() == 0)
			statusChanged = true;
		
		if (participantResources.get(jid) != null) {
			participantResources.remove(jid);
			userInfo.setParticipantCount(userInfo.getParticipantCount() - 1);
			if (userInfo.getParticipantCount() == 0)
				statusChanged = true;
		}

		if (statusChanged)
			setLocalPresence(userInfo);
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
		if (messageInfo.getSentiment() != Sentiment.INDIFFERENT)
			info.addAttribute("sentiment", messageInfo.getSentiment().name());
		info.addAttribute("serial", Long.toString(messageInfo.getSerial()));

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
		Sentiment sentiment = Sentiment.INDIFFERENT;
		JID fromJid = packet.getFrom();
		JID toJid = packet.getTo();
		
		Log.debug("Got message from: " + fromJid);

		// Only handle messages that are sent directly to the room, for broadcast
		// to all present members; we don't support private messages
		if (toJid.getResource() != null)
			return;
		
		String username = fromJid.getNode();
		
		// We call a MessengerGlue method to stick the message into the database. A message
		// will then be sent to all nodes, including us, saying that there are new messages.
		// We'll (in onMessagesChanged) query for new messages and send them to the connected clients.
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		
		Element messageInfo = packet.getChildElement("messageInfo", "http://dumbhippo.com/protocol/rooms");
		if (messageInfo != null) {
			String sentimentString = messageInfo.attributeValue("sentiment");
			if (sentimentString != null) {
				if (sentimentString.equals("LOVE"))
					sentiment = Sentiment.LOVE;
				else if (sentimentString.equals("HATE"))
					sentiment = Sentiment.HATE;
			}
		}
		
		glue.addChatRoomMessage(roomName, kind, username, packet.getBody(), sentiment, new Date());
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
			sendRoomDetails(fromJid);
		
			addRoomInfo(reply, true, fromJid.getNode());
		} else {
			reply.setError(Condition.feature_not_implemented);
		}
		
		XMPPServer.getInstance().getPacketRouter().route(reply);
	}

	public synchronized void processUserChange(String username, boolean music) {
		// update UserInfo for username
		UserInfo userInfo = lookupUserInfo(username, true);
		if (music) {
			Map<String,String> properties = getCurrentMusicFromServer(username);
			// if name and artist are not set in the element, it means that we got a message
			// about the music having stopped, preserve the information about the last played 
			// arrangement and set the nowPlaying flag to false  
			if (properties == null || ((properties.get("name") == null) && (properties.get("artist") == null))) {
				userInfo.setMusicPlaying(false);
			} else {
				userInfo.setArrangementName(properties.get("name"));
				userInfo.setArtist(properties.get("artist"));
				userInfo.setMusicPlaying(true);		    
			}
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
	public synchronized void processPacket(Packet packet) {
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
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		return glue.canJoinChat(roomName, kind, username);
	}

	/**
	 * Returns the count of present users.
	 * 
	 * @return the count of present users
	 */
	public synchronized int getPresenceCount() {
		return presentUsers.size(); 
	}
	
	/**
	 * Check if user is present in this room.
	 * 
	 * @param username
	 * @return true if user is present in this room
	 */
	public synchronized boolean checkUserPresent(String username) {
	    return presentUsers.containsKey(username);	
	}

	/**
	 * Called when the messages for the chat room have changed 
	 */
	public synchronized void onMessagesChanged() {
		MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
		List<ChatRoomMessage> newMessages = glue.getChatRoomMessages(roomName, kind, maxMessageSerial);
		
		addMessages(newMessages, true);
	}
	
	private String getPresenceLocation() {
		return "/rooms/" + roomGuid;
	}

	private void setLocalPresence(UserInfo userInfo) {
		int presence = 0;
		
		switch (userInfo.getLocalStatus()) {
		case NONMEMBER:
			presence = 0;
			break;
		case VISITOR:
			presence = 1;
			break;
		case PARTICIPANT:
			presence = 2;
			break;
		}
		
		PresenceService presenceService = PresenceService.getInstance();
		presenceService.setLocalPresence(getPresenceLocation(), Guid.parseTrustedJabberId(userInfo.getUsername()), presence);
	}
	
	// Callback from PresenceService
	public synchronized void presenceChanged(Guid guid) {
		UserInfo userInfo = lookupUserInfo(guid.toJabberId(null));
		
		RoomUserStatus oldStatus = userInfo.getGlobalStatus();
		
		PresenceService presenceService = PresenceService.getInstance();
		int newPresence = presenceService.getPresence(getPresenceLocation(), guid);

		RoomUserStatus newStatus = RoomUserStatus.NONMEMBER;
		switch (newPresence) {
		case 1:
			newStatus = RoomUserStatus.VISITOR;
			break;
		case 2:
			newStatus = RoomUserStatus.PARTICIPANT;
			break;
		}
		
		if (newStatus == oldStatus)
			return;
		
		if (newStatus == RoomUserStatus.NONMEMBER)
			presentUsers.remove(userInfo.getUsername());
		else if (oldStatus == RoomUserStatus.NONMEMBER)
			presentUsers.put(userInfo.getUsername(), userInfo);

		userInfo.setGlobalStatus(newStatus);
		
		Presence presence;
		if (newStatus == RoomUserStatus.NONMEMBER)
			presence = makePresenceUnavailable(userInfo);
		else
			presence = makePresenceAvailable(userInfo, oldStatus);

		sendPacketToAll(presence);
	}
}
