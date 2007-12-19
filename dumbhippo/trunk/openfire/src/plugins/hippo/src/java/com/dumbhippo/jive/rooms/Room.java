package com.dumbhippo.jive.rooms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.dumbhippo.Site;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.ReadOnlySession;
import com.dumbhippo.dm.fetch.BoundFetch;
import com.dumbhippo.dm.fetch.FetchNode;
import com.dumbhippo.dm.parser.FetchParser;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.MessageSender;
import com.dumbhippo.jive.XmppClient;
import com.dumbhippo.jive.XmppClientManager;
import com.dumbhippo.jive.XmppFetchVisitor;
import com.dumbhippo.live.PresenceListener;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatRoomKind;
import com.dumbhippo.server.ChatRoomUser;
import com.dumbhippo.server.ChatSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

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
		private Guid userId;
		private String username;
		private String name;
		private String smallPhotoUrl;		
		private int participantCount;
		private int presentCount;
		private RoomUserStatus globalStatus;
		
		public UserInfo(String username, String name, String smallPhotoUrl) {
			this.userId = Guid.parseTrustedJabberId(username);
			this.username = username;
			this.name = name;
			this.smallPhotoUrl = smallPhotoUrl;			
			this.participantCount = 0;
			this.presentCount = 0;
			this.globalStatus = RoomUserStatus.NONMEMBER;
		}
		
		public Guid getUserId() {
			return userId;
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
	
	private static class ResourceInfo {
		private JID jid;
		private UserInfo user;
		private boolean participant;
		private int protocolVersion;
		
		ResourceInfo(JID jid, UserInfo user) {
			this.jid = jid;
			this.user = user;
		}

		public JID getJid() {
			return jid;
		}
		
		public boolean isParticipant() {
			return participant;
		}

		public void setParticipant(boolean participant) {
			this.participant = participant;
		}

		public int getProtocolVersion() {
			return protocolVersion;
		}

		public void setProtocolVersion(int protocolVersion) {
			this.protocolVersion = protocolVersion;
		}

		public UserInfo getUser() {
			return user;
		}
		
		public void initializeReadOnlySession() {
			XmppClient client = XmppClientManager.getInstance().getClient(jid);
			DataService.getModel().initializeReadOnlySession(client);
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
	
	private Map<String, UserInfo> userInfoCache = new HashMap<String, UserInfo>();
	private Map<JID, ResourceInfo> presentResources = new HashMap<JID, ResourceInfo>();
	private Map<String, UserInfo> presentUsers = new HashMap<String, UserInfo>();
	private List<MessageInfo> messages = new ArrayList<MessageInfo>();
	private long maxMessageSerial = -1;
	
	private Room(ChatRoomInfo info) {
		roomGuid = info.getChatId();
		roomName = roomGuid.toJabberId(null);	
		kind = info.getKind();
		title = info.getTitle();
		
		addMessages(info.getHistory(), false);
		
		PresenceService.getInstance().addListener(getPresenceLocation(), this);
	}
	
	private void addMessages(List<? extends ChatMessage> toAdd, boolean notify) {
		final int startIndex = messages.size();
		
		for (ChatMessage message : toAdd) {
			UserInfo userInfo = lookupUserInfo(message.getFromUser().getGuid().toJabberId(null));
			MessageInfo messageInfo = new MessageInfo(userInfo, message.getMessageText(), message.getSentiment(), message.getTimestamp(), message.getId());
			messages.add(messageInfo);
			if (messageInfo.getSerial() > maxMessageSerial)
				maxMessageSerial = messageInfo.getSerial();
		}
		
		final int endIndex = messages.size();
		
		if (notify) {
			Log.debug("Have new messages " + startIndex + " - " + (endIndex -1) + "; notifying");

			for (final ResourceInfo destination : presentResources.values()) {
				// We do these messages asynchronously on commit only because we already
				// have a transaction here and getting rid of it is hard. Since we are always
				// only appending to messages, using indexes here is safe. The message serials 
				// prevent problems if we end up sending messages in the wrong order.
				//
				TxUtils.runInTransactionOnCommit(new Runnable() {
					public void run() {
						destination.initializeReadOnlySession();
						
						for (int i = startIndex; i < endIndex; i++)
							sendMessage(messages.get(i), false, destination);
					}
				});
			}
		}
	}
	
	public void shutdown() {
		PresenceService.getInstance().removeListener(getPresenceLocation(), this);
	}
	
	private UserInfo lookupUserInfo(String username, boolean refresh) {
		UserInfo info;
		if (refresh || !userInfoCache.containsKey(username)) {
			ChatSystem chatSystem = EJBUtil.defaultLookup(ChatSystem.class);
			ChatRoomUser user = chatSystem.getChatRoomUser(roomGuid, kind, username);		
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
	 * @param destination the recipient to send the packet to
	 */
	private void sendPacketToResource(Packet packet, ResourceInfo destination) {
		MessageSender.getInstance().sendPacketToResource(destination.getJid(), packet);
	}

	public static Room loadFromServer(String roomName) {
		Log.debug("Querying server for information on chat room " + roomName);
		ChatSystem chatSystem = EJBUtil.defaultLookup(ChatSystem.class);
		
		ChatRoomInfo info;
		try {
			info = chatSystem.getChatRoomInfo(Guid.parseJabberId(roomName), true);
		} catch (NotFoundException e) {
			Log.debug("  no such room");
			return null;
		} catch (ParseException e) {
			Log.debug("  no such room");
			return null;
		}

		if (info == null) {
			Log.debug("  no such room");
			return null;
		}

		Log.debug("  got response from server, title is " + info.getTitle());
		
		return new Room(info);
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
	}
	
	private void sendPresenceAvailable(UserInfo userInfo, RoomUserStatus oldStatus, ResourceInfo destination) {
		Presence presence = new Presence((Presence.Type)null);
		presence.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		
		Log.debug("Sending presence available from " + presence.getFrom());
		
		// Sending the (human-readable) name of the user here is a significant optimization, 
		// since otherwise the client would have to query it from the user before
		// displaying it
		Element child = presence.addChildElement("x", "http://jabber.org/protocol/muc#user");
		Element info = child.addElement("userInfo", "http://dumbhippo.com/protocol/rooms");
		info.addAttribute("role", roleString(userInfo.getGlobalStatus()));
		if (oldStatus != null) {
			info.addAttribute("oldRole", roleString(oldStatus));
		}

		if (destination.getProtocolVersion() == 0) {
			info.addAttribute("name", userInfo.getName());
			info.addAttribute("smallPhotoUrl", userInfo.getSmallPhotoUrl());
		} else {
			info.addAttribute("user", addUserResource(info, userInfo));
		}

		addRoomInfo(presence, false, null);
		
		sendPacketToResource(presence, destination);
	}
	
	private void sendPresenceUnavailable(UserInfo userInfo, ResourceInfo destination) {
		Presence presence = new Presence(Presence.Type.unavailable);
		presence.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		
		Element child = presence.addChildElement("x", "http://jabber.org/protocol/muc#user");
		Element info = child.addElement("userInfo", "http://dumbhippo.com/protocol/rooms");
		if (destination.getProtocolVersion() > 0) {
			info.addAttribute("user", addUserResource(info, userInfo));
		}

		addRoomInfo(presence, false, null);
		
		sendPacketToResource(presence, destination);
	}
	
	private void sendRoomDetails(final JID to) {
		TxUtils.runInTransaction(new Runnable() {
			public void run() {
				ResourceInfo destination = presentResources.get(to);
				destination.initializeReadOnlySession();

				// Send the list of current members
				for (UserInfo memberInfo : presentUsers.values()) {
					sendPresenceAvailable(memberInfo, null, destination);
				}
				
				// And a a history of recent messages
				int count = messages.size();
				if (count > MAX_HISTORY_COUNT)
					count = MAX_HISTORY_COUNT;
				
				for (int i = messages.size() - count; i < messages.size(); i++) {
					MessageInfo messageInfo = messages.get(i);
					sendMessage(messageInfo, true, destination);
				}
			}
		});
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
		// user wants to join as a 'visitor' or a 'participant' and whether
		// it wants the old data hand-roled protocol or the new data model protocol.
		boolean participant = true;
		int protocolVersion = 0;
		Element xElement = packet.getChildElement("x", "http://jabber.org/protocol/muc");
		if (xElement != null) {
	        for (Iterator i = xElement.elementIterator("userInfo"); i.hasNext(); ) {
	            Element element = (Element)i.next();
	            if (element.getNamespaceURI().equals("http://dumbhippo.com/protocol/rooms")) {
	            	Attribute attr = element.attribute("role");
	            	if (attr != null)
	            		participant = attr.getText().equals("participant");
	            	
	            	attr = element.attribute("protocol");
	            	if (attr != null) {
	            		try {
	            			protocolVersion = Integer.parseInt(attr.getText());
	            		} catch (NumberFormatException e) {
	            			Log.warn("Invalid protocol attribute: " + attr.getText());
	            		}
	            	}
	            }
	        }			
		}
		
		boolean resourceWasPresent;
		boolean resourceWasParticipant;
		
		ResourceInfo resourceInfo = presentResources.get(jid);
		if (resourceInfo != null) {
			resourceWasPresent = true;
			resourceWasParticipant = resourceInfo.isParticipant();
		} else {
			resourceWasPresent = false;
			resourceWasParticipant = false;
			
			resourceInfo = new ResourceInfo(jid, userInfo);
			presentResources.put(jid, resourceInfo);
		}
		
		resourceInfo.setParticipant(participant);
		resourceInfo.setProtocolVersion(protocolVersion);

		boolean statusChanged = false;

		if (!resourceWasPresent) {
			userInfo.setPresentCount(userInfo.getPresentCount() + 1);
		
			// User is joining the channel for the first time
			if (userInfo.getPresentCount() == 1) {
				statusChanged = true;
			}
		}

		if (participant && !resourceWasParticipant) {
			userInfo.setParticipantCount(userInfo.getParticipantCount() + 1);
			if (userInfo.getParticipantCount() == 1) {
				statusChanged = true;
			}			
		} else if (!participant && resourceWasParticipant) {
			userInfo.setParticipantCount(userInfo.getParticipantCount() - 1);
			if (userInfo.getParticipantCount() == 0) {
				statusChanged = true;
			}
		}
		
		// Joining a chat implicitly un-ignores a post
		if (participant && kind == ChatRoomKind.POST) {
			MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
			try {
				glue.setPostIgnored(Guid.parseTrustedJabberId(username), 
								    roomGuid, false);
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
		if (!resourceWasPresent) {
			sendRoomDetails(jid);
		}
	}
	
	private void processPresenceUnavailable(Presence packet) {
		JID jid = packet.getFrom();
		
		Log.debug("Got unavailable presence from : " + jid);

		ResourceInfo resourceInfo = presentResources.get(jid);
		if (resourceInfo == null)
			return; // Not present, nothing to do
		
		presentResources.remove(jid);
			
		boolean statusChanged = false;
		
		UserInfo userInfo = resourceInfo.getUser();

		presentResources.remove(jid);
		userInfo.setPresentCount(userInfo.getPresentCount() - 1);
		if (userInfo.getPresentCount() == 0)
			statusChanged = true;
		
		if (resourceInfo.isParticipant()) {
			userInfo.setParticipantCount(userInfo.getParticipantCount() - 1);
			if (userInfo.getParticipantCount() == 0)
				statusChanged = true;
		}

		if (statusChanged)
			setLocalPresence(userInfo);
	}
	
	private static final String USER_FETCH_STRING = "+";
	private static final FetchNode USER_FETCH;
	static {
		try {
			USER_FETCH = FetchParser.parse(USER_FETCH_STRING);
		} catch (com.dumbhippo.dm.parser.ParseException e) {
			throw new RuntimeException("Can't parse user fetch string", e);
		}
	}
	
	private String addUserResource(Element rootElement, UserInfo userInfo) {
		Element resources = rootElement.addElement("resources");
		XmppFetchVisitor visitor = new XmppFetchVisitor(resources, DataService.getModel());
		
		ReadOnlySession session = DataService.currentSessionRO();
		UserDMO userDMO;
		try {
			userDMO = session.find(UserDMO.class, userInfo.getUserId());
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't get UserDMO object", e);
		}
		BoundFetch<Guid,DMObject<Guid>> fetch = USER_FETCH.bind(userDMO.getClassHolder());
		session.visitFetch(userDMO, fetch, visitor, true);
		
		if (resources.elements().size() == 0)
			rootElement.remove(resources);
		
		return userDMO.getResourceId();
	}
	
	private void sendMessage(MessageInfo messageInfo, boolean isDelayed, ResourceInfo destination) {
		UserInfo userInfo = messageInfo.getUser(); 
		
		Message outgoing = new Message();
		outgoing.setFrom(new JID(roomName, getServiceDomain(), userInfo.getUsername()));
		outgoing.setBody(messageInfo.getText());
		
		Element messageElement = outgoing.getElement();
		Element info = messageElement.addElement("messageInfo", "http://dumbhippo.com/protocol/rooms");
		info.addAttribute("timestamp", Long.toString(messageInfo.getTimestamp().getTime()));
		if (messageInfo.getSentiment() != Sentiment.INDIFFERENT)
			info.addAttribute("sentiment", messageInfo.getSentiment().name());
		info.addAttribute("serial", Long.toString(messageInfo.getSerial()));

		if (destination.getProtocolVersion() == 0) {
			info.addAttribute("name", userInfo.getName());
			info.addAttribute("smallPhotoUrl", userInfo.getSmallPhotoUrl());
		} else {
			info.addAttribute("user", addUserResource(info, userInfo));
		}

		if (isDelayed) {
			Element delay = messageElement.addElement("x", "jabber:x:delay");
			delay.addAttribute("from", outgoing.getFrom().toString());
			// This isn't in the Jabber format and we're too lazy to fix it 
			// delay.addAttribute("stamp", info.attributeValue("timestamp"));
		}

		addRoomInfo(outgoing, false, null);
		
		sendPacketToResource(outgoing, destination);
	}
	
	private Sentiment sentimentFromString(String sentimentString) {
		if (sentimentString != null) {
			if (sentimentString.equals("LOVE"))
				return Sentiment.LOVE;
			else if (sentimentString.equals("HATE"))
				return Sentiment.HATE;
		}
		
		return Sentiment.INDIFFERENT;
	}
	
	private void processMessagePacket(final Message packet) {
		JID fromJid = packet.getFrom();
		JID toJid = packet.getTo();
		
		Log.debug("Got message from: " + fromJid);

		// Only handle messages that are sent directly to the room, for broadcast
		// to all present members; we don't support private messages
		if (toJid.getResource() != null)
			return;
		
		String username = fromJid.getNode();
		final Guid userId = Guid.parseTrustedJabberId(username);
		
		
		Element messageInfo = packet.getChildElement("messageInfo", "http://dumbhippo.com/protocol/rooms");
		
		final Sentiment sentiment = sentimentFromString(messageInfo != null ? messageInfo.attributeValue("sentiment") : null); 

		TxUtils.runInTransaction(new TxRunnable() {
			public void run() throws RetryException {
				// We call a MessengerGlue method to stick the message into the database. A message
				// will then be sent to all nodes, including us, saying that there are new messages.
				// We'll (in onMessagesChanged) query for new messages and send them to the connected clients.
				ChatSystem chatSystem = EJBUtil.defaultLookup(ChatSystem.class);
				IdentitySpider spider = EJBUtil.defaultLookup(IdentitySpider.class);
				UserViewpoint viewpoint = new UserViewpoint(spider.lookupUser(userId), Site.XMPP);
				
				chatSystem.addChatRoomMessage(roomGuid, kind, viewpoint, packet.getBody(), sentiment, new Date());
			}
		});
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
		ChatSystem chatSystem = EJBUtil.defaultLookup(ChatSystem.class);
		Guid userId;
		try {
			userId = Guid.parseJabberId(username);
		} catch (ParseException e) {
			return false;
		}
		return chatSystem.canJoinChat(roomGuid, kind, new UserViewpoint(userId, Site.XMPP));
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
		ChatSystem chatSystem = EJBUtil.defaultLookup(ChatSystem.class);
		List<? extends ChatMessage> newMessages = chatSystem.getChatRoomMessages(roomGuid, kind, maxMessageSerial);
		
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
		final UserInfo userInfo = lookupUserInfo(guid.toJabberId(null));
		
		final RoomUserStatus oldStatus = userInfo.getGlobalStatus();
		
		PresenceService presenceService = PresenceService.getInstance();
		int newPresence = presenceService.getPresence(getPresenceLocation(), guid);

		final RoomUserStatus newStatus;
		switch (newPresence) {
		case 1:
			newStatus = RoomUserStatus.VISITOR;
			break;
		case 2:
			newStatus = RoomUserStatus.PARTICIPANT;
			break;
		default:
			newStatus = RoomUserStatus.NONMEMBER;
			break;
		}
		
		if (newStatus == oldStatus)
			return;
		
		if (newStatus == RoomUserStatus.NONMEMBER)
			presentUsers.remove(userInfo.getUsername());
		else if (oldStatus == RoomUserStatus.NONMEMBER)
			presentUsers.put(userInfo.getUsername(), userInfo);

		userInfo.setGlobalStatus(newStatus);
		
		for (final ResourceInfo destination : presentResources.values()) {
			TxUtils.runInTransaction(new Runnable() {
				public void run() {
					destination.initializeReadOnlySession();
					if (newStatus == RoomUserStatus.NONMEMBER) {
						sendPresenceUnavailable(userInfo, destination);
					} else {
						sendPresenceAvailable(userInfo, oldStatus, destination);
					}
				}
			});
		}
	}
}
