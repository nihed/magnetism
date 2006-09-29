package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.ChatRoomEvent;
import com.dumbhippo.live.Hotness;
import com.dumbhippo.live.LiveClientData;
import com.dumbhippo.live.LivePost;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.UserChangedEvent;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.EmbeddedMessage;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.BlockView;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatRoomKind;
import com.dumbhippo.server.ChatRoomMessage;
import com.dumbhippo.server.ChatRoomUser;
import com.dumbhippo.server.EntityView;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.GroupView;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.MusicSystemInternal;
import com.dumbhippo.server.MySpaceTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.ServerStatus;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.UserViewpoint;

@Stateless
public class MessengerGlueBean implements MessengerGlue {
	
	static private final Logger logger = GlobalSetup.getLogger(MessengerGlueBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;
		
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private MySpaceTracker mySpaceTracker;
	
	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private MusicSystemInternal musicSystemInternal;	
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private ServerStatus serverStatus;
	
	@EJB
	private TransactionRunner transactionRunner;
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@EJB
	@IgnoreDependency
	private Stacker stacker;
	
	static final private long EXECUTION_WARN_MILLISECONDS = 5000;
	
	static private long tooBusyCount;
	
	static final private int MAX_ACTIVE_REQUESTS = 7; 
	
	static private int activeRequestCount;
	
	static private int maxActiveRequestCount;
	
	static private synchronized void changeActiveRequestCount(int delta) {
		activeRequestCount += delta;
		if (activeRequestCount > maxActiveRequestCount)
			maxActiveRequestCount = activeRequestCount;
	}
	
	static public synchronized int getActiveRequestCount() {
		return activeRequestCount;
	}

	static public synchronized int getMaxActiveRequestCount() {
		return maxActiveRequestCount;
	}
	
	static private synchronized void incrementTooBusyCount() {
		tooBusyCount += 1;
	}
	
	static public synchronized long getTooBusyCount() {
		return tooBusyCount;
	}
	
	@AroundInvoke
	public Object timeAndCatchRuntimeExceptions(InvocationContext ctx) throws Exception {
		try {
			changeActiveRequestCount(1);
			long start = System.currentTimeMillis();
			Object result = ctx.proceed();
			long end = System.currentTimeMillis();
			if (end - start > EXECUTION_WARN_MILLISECONDS) {
				logger.warn("Execution of MessengerGlueBean.{} took {} milliseconds", 
				  	        ctx.getMethod().getName(), end - start);
			}
			return result;
		} catch (Exception e) {
			logger.error("Unexpected exception: " + e.getMessage(), e);
			// create a new RuntimeException that won't have any types the XMPP server 
			// is unfamiliar with
			throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
		} finally {
			changeActiveRequestCount(-1);
		}
	}
	
	private Account accountFromUsername(String username) throws JabberUserNotFoundException {
		Guid guid;
		try {
			guid = Guid.parseJabberId(username);
			Account account = accountSystem.lookupAccountByOwnerId(guid);
			
			assert account.getOwner().getId().equals(username);
			
			return account;
		} catch (ParseException e) {
			throw new JabberUserNotFoundException("corrupt username");
		} catch (NotFoundException e) {
			throw new JabberUserNotFoundException("username does not exist");
		}
	}
	
	private User userFromTrustedUsername(String username) {
		try {
			return identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException("trusted username doesn't appear to exist: " + username);
		}
	}
	
	public boolean authenticateJabberUser(String username, String token, String digest) {
		Account account;
		
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			return false;
		}
		
		assert account != null;
		
		return !account.isAdminDisabled() && account.checkClientCookie(token, digest);
	}
	

	public long getJabberUserCount() {
		return accountSystem.getNumberOfActiveAccounts();
	}


	public void setName(String username, String name)
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}


	public void setEmail(String username, String email) 
		throws JabberUserNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public JabberUser loadUser(String username) throws JabberUserNotFoundException {
		
		Account account = accountFromUsername(username);
		
		PersonView view = personViewer.getSystemView(account.getOwner(), PersonViewExtra.PRIMARY_EMAIL);
		
		String email = null;
		if (view.getEmail() != null)
			email = view.getEmail().getEmail();
		
		JabberUser user = new JabberUser(username, account.getOwner().getNickname(), email);
	
		return user;
	}
	
	private void doShareLinkTutorial(Account account) {
		logger.debug("We have a new user!!!!! WOOOOOOOOOOOOHOOOOOOOOOOOOOOO send them tutorial!");

		InvitationToken invite = invitationSystem.getCreatingInvitation(account);
		
		// see what feature the user was sold on originally, and share the right thing 
		// with them accordingly
		
		User owner = account.getOwner();
		if (invite != null && invite.getPromotionCode() == PromotionCode.MUSIC_INVITE_PAGE_200602)
			postingBoard.doNowPlayingTutorialPost(owner);
		else {
			UserViewpoint viewpoint = new UserViewpoint(owner);
			Set<Group> invitedToGroups = groupSystem.findRawGroups(viewpoint, owner, MembershipStatus.INVITED);
			Set<Group> invitedToFollowGroups = groupSystem.findRawGroups(viewpoint, owner, MembershipStatus.INVITED_TO_FOLLOW);
			invitedToGroups.addAll(invitedToFollowGroups);
			if (invitedToGroups.size() == 0) {
				postingBoard.doShareLinkTutorialPost(account.getOwner());
			} else {
				for (Group group : invitedToGroups) {
					postingBoard.doGroupInvitationPost(owner, group);
				}
			}
		}

		account.setWasSentShareLinkTutorial(true);
	}

	public void onResourceConnected(String username, boolean wasAlreadyConnected, Date timestamp) {
		// account could be missing due to debug users or our own
		// send-notifications
		// user, i.e. any user on the jabber server that we don't know about
		Account account;
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			if (!username.equals("admin"))
				logger.warn("username signed on that we don't know: {}", username);
			return;
		}
		
		LiveState.getInstance().resendAllNotifications(account.getOwner().getGuid());

		account.setLastLoginDate(timestamp);
		
		// We can't reliably tell if the user is currently logged in by checking
		// for loginDate > logoutDate, since that could happen if the server
		// crashed while the user was logged in as well, so instead we check
		// wasAlreadyConnected. wasAlreadyConnected isn't 100% reliable if two 
		// resources are connecting simultaneously, but the worst that will happen 
		// is that the user gets backlog replayed to both accounts, which might be 
		// considered a feature.
		if (!wasAlreadyConnected) {
			postingBoard.sendBacklog(account.getOwner(), account.getLastLogoutDate());
		}
			
		if (!account.getWasSentShareLinkTutorial()) {
			doShareLinkTutorial(account);
		}
	}	
	

	public void onUserLogout(String username, Date timestamp) {
		Account account;
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			if (!username.equals("admin"))
				logger.warn("username logged out that we don't know: {}", username);
			return;
		}
		
		account.setLastLogoutDate(timestamp);
	}
	
	public String getMySpaceName(String username) {
		User user = userFromTrustedUsername(username);
		try {
			return externalAccounts.getMySpaceName(SystemViewpoint.getInstance(), user);
		} catch (NotFoundException e) {
			return null;
		}
	}
	
	public void addMySpaceBlogComment(String username, long commentId, long posterId) {
		mySpaceTracker.addMySpaceBlogComment(userFromTrustedUsername(username), commentId, posterId);
	}	
	
	public List<MySpaceBlogCommentInfo> getMySpaceBlogComments(String username) {
		List<MySpaceBlogCommentInfo> ret = new ArrayList<MySpaceBlogCommentInfo>();
		for (MySpaceBlogComment cmt : mySpaceTracker.getRecentComments(userFromTrustedUsername(username))) {
			ret.add(new MySpaceBlogCommentInfo(cmt.getCommentId(), cmt.getPosterId()));
		}
		return ret;
	}
	
	private List<MySpaceContactInfo> userSetToContactList(Set<User> users) {
		List<MySpaceContactInfo> ret = new ArrayList<MySpaceContactInfo>();
		for (User user : users) {
			String myspaceName = null;
			String myspaceFriendId = null;
			try {
				ExternalAccount external = externalAccounts.lookupExternalAccount(SystemViewpoint.getInstance(), user, ExternalAccountType.MYSPACE);
				if (external.getSentiment() == Sentiment.LOVE) {
					myspaceName = external.getHandle();
					myspaceFriendId = external.getExtra();
				}
			} catch (NotFoundException e) {
			}
			ret.add(new MySpaceContactInfo(myspaceName, myspaceFriendId));
		}
		return ret;
	}
	
	public List<MySpaceContactInfo> getContactMySpaceNames(String username) {
		User requestingUser = userFromTrustedUsername(username);
		return userSetToContactList(identitySpider.getMySpaceContacts(new UserViewpoint(requestingUser)));
	}
	
	public void notifyNewMySpaceContactComment(String username, String mySpaceContactName) {
		User requestingUser = userFromTrustedUsername(username);
		mySpaceTracker.notifyNewContactComment(new UserViewpoint(requestingUser), mySpaceContactName);
	}
	
	private User getUserFromGuid(Guid guid) {
		try {
			return identitySpider.lookupGuid(User.class, guid);
		} catch (NotFoundException e) {
			throw new RuntimeException("User does not exist: " + guid, e);
		}
	}
	
	private User getUserFromUsername(String username) {
		return getUserFromGuid(Guid.parseTrustedJabberId(username));
	}
	
	private Post getPostFromRoomName(String roomName) throws NotFoundException {
		return postingBoard.loadRawPost(SystemViewpoint.getInstance(), Guid.parseTrustedJabberId(roomName));
	}
	
	private Group getGroupFromRoomName(String roomName) throws NotFoundException {
		return groupSystem.lookupGroupById(SystemViewpoint.getInstance(), Guid.parseTrustedJabberId(roomName));
	}
	
	private ChatRoomMessage newChatRoomMessage(EmbeddedMessage message) {
		String username = message.getFromUser().getGuid().toJabberId(null);
		return new ChatRoomMessage(username, message.getMessageText(),
				message.getTimestamp(), message.getId()); 		
	}
	
	private ChatRoomUser newChatRoomUser(User user) {
		return new ChatRoomUser(user.getGuid().toJabberId(null),
				                user.getNickname(), user.getPhotoUrl60());
	}
	
	private Set<ChatRoomUser> getChatRoomRecipients(Group group) {
		Set<ChatRoomUser> allowedUsers = new HashSet<ChatRoomUser>();		
		Set<User> members = groupSystem.getUserMembers(SystemViewpoint.getInstance(), group, MembershipStatus.ACTIVE);
		for (User user : members) {
			allowedUsers.add(newChatRoomUser(user));
		}
		return allowedUsers;
	}
		
	
	private List<ChatRoomMessage> getChatRoomMessages(Group group, long lastSeenSerial) {
		List<GroupMessage> messages = groupSystem.getGroupMessages(group, lastSeenSerial);

		List<ChatRoomMessage> history = new ArrayList<ChatRoomMessage>();
		
		for (GroupMessage m : messages)
			history.add(newChatRoomMessage(m));

		return history;
	}

	private ChatRoomInfo getChatRoomInfo(String roomName, Group group) {
		List<ChatRoomMessage> history = getChatRoomMessages(group, -2);
		return new ChatRoomInfo(ChatRoomKind.GROUP, roomName, group.getName(), history, false);
	}

	private Set<ChatRoomUser> getChatRoomRecipients(Post post) {
		Set<ChatRoomUser> recipients = new HashSet<ChatRoomUser>();
		User poster = post.getPoster();
		if (poster != null)
			recipients.add(newChatRoomUser(poster));	
		
		// FIXME: This doesn't handle
		// posts where people join a group that it was sent to after the post was
		// sent. 
		for (Resource recipient : post.getExpandedRecipients()) {
			User user = identitySpider.getUser(recipient);
			if (user != null) {
				recipients.add(newChatRoomUser(user));
			}
		}
		return recipients;
	}

	private List<ChatRoomMessage> getChatRoomMessages(Post post, long lastSeenSerial) {
		List<PostMessage> messages = postingBoard.getPostMessages(post, lastSeenSerial);

		List<ChatRoomMessage> history = new ArrayList<ChatRoomMessage>();

		if (lastSeenSerial < -1) {
			// if post description is not empty, add it to the history of chat room messages.
			// We mark this message that contains post description with the serial = -1
			// FIXME: Should handle the case of a FeedPost where the effective poster is a GroupFeed
			User poster = post.getPoster();
			if (poster != null && post.getText().trim().length() != 0) {
	            ChatRoomMessage message = new ChatRoomMessage(poster.getGuid().toJabberId(null), post.getText(), post.getPostDate(), -1);	 
	            history.add(message);
			}
		}
		
		for (PostMessage postMessage : messages) {
			history.add(newChatRoomMessage(postMessage));
		}
		
		return history;
	}
	
	private ChatRoomInfo getChatRoomInfo(String roomName, Post post) {
		boolean worldAccessible = true;
		if (post.getVisibility() == PostVisibility.RECIPIENTS_ONLY)
			worldAccessible = false;
		
		List<ChatRoomMessage> history = getChatRoomMessages(post, -2);
		return new ChatRoomInfo(ChatRoomKind.POST, roomName, post.getTitle(), history, worldAccessible);
	}
	
	public ChatRoomUser getChatRoomUser(String roomName, ChatRoomKind kind, String username) {
		User user;
		// Note: we could add access controls here as well, requiring that the username
		// is allowed to join the chat room.  But for now that's checked in the chat room
		// code.
		try {
			user = identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		return newChatRoomUser(user);
	}
	
	public Set<ChatRoomUser> getChatRoomRecipients(String roomName, ChatRoomKind kind) {
		try {
			if (kind == ChatRoomKind.POST)
				return getChatRoomRecipients(getPostFromRoomName(roomName));
			else if (kind == ChatRoomKind.GROUP)
				return getChatRoomRecipients(getGroupFromRoomName(roomName));
			else
				throw new RuntimeException("Unknown chat room type " + kind);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ChatRoomInfo getChatRoomInfo(String roomName) {
		Post post = null;
		Group group = null;
		try {
			post = getPostFromRoomName(roomName);
		} catch (NotFoundException e) {
			// FIXME in principle this should happen if the initialUser can't see the post, 
			// but right now there's no access controls in loadRawPost so it only happens
			// if something is broken
			post = null;
		}
		try {
			group = getGroupFromRoomName(roomName);
		} catch (NotFoundException e) {
			group = null;
		}
		
		if (post != null && group != null) {
			// this should theoretically be very, very unlikely so we won't bother to fix it unless 
			// it happens on production, and even then we could just munge the database instead of 
			// bothering...
			logger.error("GUID collision... we'll have to put a group vs. post marker in the room ID string");
			group = null; // just take a guess and use the post
		}
		
		if (post != null)
			return getChatRoomInfo(roomName, post);
		else if (group != null)
			return getChatRoomInfo(roomName, group);
		else {
			logger.debug("Room name {} doesn't correspond to a post or group, or user not allowed to see it", roomName);
			return null;
		}
	}
	
	public List<ChatRoomMessage> getChatRoomMessages(String roomName, ChatRoomKind kind, long lastSeenSerial) {
		switch (kind) {
		case POST:
			Post post;
			try {
				post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), Guid.parseTrustedJabberId(roomName));
			} catch (NotFoundException e) {
				throw new RuntimeException("post chat not found", e);
			}
			return getChatRoomMessages(post, lastSeenSerial);
		case GROUP:
			Group group;
			try {
				group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), Guid.parseTrustedJabberId(roomName));
			} catch (NotFoundException e) {
				throw new RuntimeException("group chat not found", e);
			}
			return getChatRoomMessages(group, lastSeenSerial);
		}
		throw new IllegalArgumentException("Bad chat room type");
	}

	public void addChatRoomMessage(String roomName, ChatRoomKind kind, String userName, String text, Date timestamp) {
		Guid chatRoomId = Guid.parseTrustedJabberId(roomName);
		User fromUser = getUserFromUsername(userName);
		UserViewpoint viewpoint = new UserViewpoint(fromUser);
		switch (kind) {
		case POST:
			Post post;
			try {
				post = postingBoard.loadRawPost(viewpoint, Guid.parseTrustedJabberId(roomName));
			} catch (NotFoundException e) {
				throw new RuntimeException("post chat not found", e);
			}
			postingBoard.addPostMessage(post, fromUser, text, timestamp);
			break;
		case GROUP:
			Group group;
			try {
				group = groupSystem.lookupGroupById(viewpoint, Guid.parseTrustedJabberId(roomName));
			} catch (NotFoundException e) {
				throw new RuntimeException("group chat not found", e);
			}
			groupSystem.addGroupMessage(group, fromUser, text, timestamp);
			break;
		}
		
		LiveState.getInstance().queueUpdate(new ChatRoomEvent(chatRoomId, ChatRoomEvent.Detail.MESSAGES_CHANGED));
	}

	public boolean canJoinChat(String roomName, ChatRoomKind kind, String username) {
		try {
			User user = getUserFromUsername(username);
			if (kind == ChatRoomKind.POST) {
				Post post = getPostFromRoomName(roomName);
				UserViewpoint viewpoint = new UserViewpoint(user);
				return postingBoard.canViewPost(viewpoint, post);
			} else if (kind == ChatRoomKind.GROUP) {
				Group group = getGroupFromRoomName(roomName);
				return groupSystem.isMember(group, user);
			} else
				throw new RuntimeException("Unknown chat room type " + kind);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}	
	
	public Map<String, String> getCurrentMusicInfo(String username) {
		Map<String,String> musicInfo = new HashMap<String,String>();
		
		// would through an exception if the user does not exist
		User user = getUserFromUsername(username);
		try {
			// because we are asking only for one recent track, we do not need to
			// pass a viewpoint
		    TrackView trackView = musicSystem.getCurrentTrackView(null, user);
		    musicInfo.put("name", trackView.getName());
		    musicInfo.put("artist", trackView.getArtist());
		    musicInfo.put("musicPlaying", Boolean.toString(trackView.isNowPlaying()));
		} catch (NotFoundException e) {
			// user does not have a music history
			return null;
		}
		
		return musicInfo;
	}

	public Map<String,String> getPrefs(String username) {
		Map<String,String> prefs = new HashMap<String,String>();
		
		Account account;
		try {
			account = accountFromUsername(username);
		} catch (JabberUserNotFoundException e) {
			logger.warn("Returning empty prefs for user we've never heard of");
			return prefs;
		}
		
		return accountSystem.getPrefs(account);
	}

	public Hotness getUserHotness(String username) {
		User user = userFromTrustedUsername(username);
		LiveState state = LiveState.getInstance();
		LiveClientData data = state.getLiveClientData(user.getGuid());
		return data.getHotness();
	}
	
	static final String RECENT_POSTS_ELEMENT_NAME = "recentPosts";
	static final String RECENT_POSTS_NAMESPACE = "http://dumbhippo.com/protocol/post";
	
	public String getPostsXml(Guid userId, Guid id, String elementName) {
		User user = getUserFromGuid(userId);
		UserViewpoint viewpoint = new UserViewpoint(user);
		LiveState liveState = LiveState.getInstance();
		List<PostView> views;
		
		if (id != null) {
			PostView view;
			try {
				view = postingBoard.loadPost(viewpoint, id);
			} catch (NotFoundException e) {
				return null;
			}	
			
			views = Collections.singletonList(view);
		} else {
			views = postingBoard.getReceivedPosts(viewpoint, user, 0, 4);
		}

		XmlBuilder builder = new XmlBuilder();
		if (elementName == null)
			elementName = RECENT_POSTS_ELEMENT_NAME;
		builder.openElement(elementName, "xmlns", RECENT_POSTS_NAMESPACE);
		
		Set<EntityView> viewerEntities = new HashSet<EntityView>();
		
		for (PostView postView : views) {
			LivePost lpost = liveState.getLivePost(postView.getPost().getGuid());
			for (Guid guid : lpost.getViewers()) {
				Person viewer;
				try {
					viewer = identitySpider.lookupGuid(Person.class, guid);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
				viewerEntities.add(personViewer.getPersonView(viewpoint, viewer));
			}
			
			
			for (EntityView ev : postingBoard.getReferencedEntities(viewpoint, postView.getPost())) {
				viewerEntities.add(ev);
			}			
		}
		
		for (EntityView ev : viewerEntities) {
			builder.append(ev.toXml());
		}
		
		for (PostView postView : views) {
			builder.append(postView.toXml());
			LivePost lpost = liveState.getLivePost(postView.getPost().getGuid()); 
			builder.append(lpost.toXml());
		}
		
		builder.closeElement();
		return builder.toString();
	}

	public void setPostIgnored(Guid userId, Guid postId, boolean ignore) throws NotFoundException, ParseException {
		User user = getUserFromGuid(userId);
		Post post = postingBoard.loadRawPost(new UserViewpoint(user), postId);
		postingBoard.setPostIgnored(user, post, ignore);
	}

	public String getGroupXml(Guid userId, Guid groupId) throws NotFoundException {
		User user = getUserFromGuid(userId);
		UserViewpoint viewpoint = new UserViewpoint(user);		
		GroupView groupView = groupSystem.loadGroup(viewpoint, groupId);
		return groupView.toXml();
	}

	public void addGroupMember(Guid userId, Guid groupId, Guid inviteeId) throws NotFoundException {
		User user = getUserFromGuid(userId);
		UserViewpoint viewpoint = new UserViewpoint(user);
		GroupView groupView = groupSystem.loadGroup(viewpoint, groupId);
		User invitee = getUserFromGuid(inviteeId);		
		groupSystem.addMember(user, groupView.getGroup(), invitee);
	}

	public String getBlocksXml(String username, long lastTimestamp, int start, int count) {
		User user = getUserFromUsername(username);
		UserViewpoint viewpoint = new UserViewpoint(user);
		List<BlockView> list = stacker.getStack(viewpoint, user, lastTimestamp, start, count);
		XmlBuilder xml = new XmlBuilder();
		CommonXmlWriter.writeBlocks(xml, viewpoint, user, list, CommonXmlWriter.NAMESPACE_BLOCKS);
		return xml.toString();
	}
	
	public boolean isServerTooBusy() {
		if (activeRequestCount >= MAX_ACTIVE_REQUESTS || serverStatus.throttleXmppConnections()) {
			incrementTooBusyCount();
			return true;
		} else {
			return false;
		}
	}

	private void queueMusicChange(final Guid userId) {
		// LiveState.queueUpdate() expects to be called in a transaction, and we
		// don't have one since the music system code needs to be called not
		// in a transaction.
		transactionRunner.runTaskInNewTransaction(new Runnable() {
			public void run() {
				LiveState.getInstance().queueUpdate(new UserChangedEvent(userId, UserChangedEvent.Detail.MUSIC)); 
			}
		});
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void handleMusicChanged(Guid userId, Map<String, String> properties) {
		User user = getUserFromGuid(userId);
		musicSystemInternal.setCurrentTrack(user, properties);
		queueMusicChange(userId);
	}

	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void handleMusicPriming(Guid userId, List<Map<String, String>> tracks) {
		User user = getUserFromGuid(userId);
		if (identitySpider.getMusicSharingPrimed(user)) {
			// at log .info, since it isn't really a problem, but if it happened a lot we'd 
			// want to investigate why
			logger.info("Ignoring priming data for music sharing, already primed");
			return;
		}
		// the tracks are in order from most to least highly-ranked, we want to 
		// timestamp the most highly-ranked one as most recent, so do this backward
		tracks = new ArrayList<Map<String,String>>(tracks);
		Collections.reverse(tracks);
		for (Map<String,String> properties : tracks) {
			musicSystemInternal.addHistoricalTrack(user, properties);
		}
		// don't do this again
		identitySpider.setMusicSharingPrimed(user, true);
		logger.debug("Primed user with {} tracks", tracks.size());	
		queueMusicChange(userId);
	}
}
