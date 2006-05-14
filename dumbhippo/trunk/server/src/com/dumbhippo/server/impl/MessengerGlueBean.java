package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.Hotness;
import com.dumbhippo.live.LivePost;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveXmppServer;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.EmbeddedMessage;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.MySpaceBlogComment;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatRoomKind;
import com.dumbhippo.server.ChatRoomMessage;
import com.dumbhippo.server.ChatRoomUser;
import com.dumbhippo.server.EntityView;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.JabberUserNotFoundException;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.MySpaceTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.server.UserViewpoint;

@Stateless
public class MessengerGlueBean implements MessengerGlueRemote {
	
	static private final Logger logger = GlobalSetup.getLogger(MessengerGlueBean.class);
	
	@EJB
	private IdentitySpider identitySpider;
		
	@EJB
	private AccountSystem accountSystem;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private MySpaceTracker mySpaceTracker;
	
	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB
	private GroupSystem groupSystem;
	
	private Account accountFromUsername(String username) throws JabberUserNotFoundException {
		Guid guid;
		try {
			guid = Guid.parseJabberId(username);
		} catch (ParseException e) {
			throw new JabberUserNotFoundException("corrupt username");
		}
		Account account = accountSystem.lookupAccountByPersonId(guid.toString());
		if (account == null)
			throw new JabberUserNotFoundException("username does not exist");
		
		assert account.getOwner().getId().equals(username);
		
		return account;
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
		
		return account.checkClientCookie(token, digest);
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
		
		PersonView view = identitySpider.getSystemView(account.getOwner(), PersonViewExtra.PRIMARY_EMAIL);
		
		JabberUser user = new JabberUser(username, account.getOwner().getNickname(), view.getEmail().getEmail());
	
		return user;
	}

	public String serverStartup(long timestamp) {
		logger.info("Jabber server startup at {}", new Date(timestamp));
		
		return LiveState.getInstance().createXmppServer().getServerIdentifier();
	}
	
	public void serverPing(String serverIdentifier) throws NoSuchServerException {
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		server.ping();
	}
	
	public void onUserAvailable(String serverIdentifier, String username) throws NoSuchServerException {
		logger.debug("Jabber user {} now available", username);

		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);

		try {
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
			
			server.userAvailable(account.getOwner().getGuid());
			
			if (!account.getWasSentShareLinkTutorial()) {
				logger.debug("We have a new user!!!!! WOOOOOOOOOOOOHOOOOOOOOOOOOOOO send them tutorial!");
	
				InvitationToken invite = invitationSystem.getCreatingInvitation(account);
				
				// see what feature the user was sold on originally, and share the right thing 
				// with them accordingly
				
				if (invite != null && invite.getPromotionCode() == PromotionCode.MUSIC_INVITE_PAGE_200602)
					postingBoard.doNowPlayingTutorialPost(account.getOwner());
				else
					postingBoard.doShareLinkTutorialPost(account.getOwner());
	
				account.setWasSentShareLinkTutorial(true);
			}
		} catch (RuntimeException e) {
			logger.error("Failed to do share link tutorial");
			throw e;
		}
	}

	public void onUserUnavailable(String serverIdentifier, String username) throws NoSuchServerException {
		logger.debug("Jabber user {} now unavailable", username);
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		try {
			server.userUnavailable(Guid.parseJabberId(username));
		} catch (ParseException e) {
			logger.warn("Corrupt username passed to onUserUnavailable", e);
		}
	}

	public void onRoomUserAvailable(String serverIdentifier, ChatRoomKind kind, String roomname, String username, boolean participant) throws NoSuchServerException  {
		logger.debug("Jabber user {} has joined chatroom {}", username, roomname);
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		if (kind == ChatRoomKind.POST) {
			try {
				server.postRoomUserAvailable(Guid.parseJabberId(roomname), Guid.parseJabberId(username), participant);
			} catch (ParseException e) {
				logger.warn("Corrupt roomname or username passed to onUserUnavailable", e);
			}
		}
	}

	public void onRoomUserUnavailable(String serverIdentifier, ChatRoomKind kind, String roomname, String username) throws NoSuchServerException {
		logger.debug("Jabber user {} has left chatroom {}", username, roomname);
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		
		if (kind == ChatRoomKind.POST) {
			try {
				server.postRoomUserUnavailable(Guid.parseJabberId(roomname), Guid.parseJabberId(username));
			} catch (ParseException e) {
				logger.warn("Corrupt roomname or username passed to onUserUnavailable", e);
			}
		}
	}
	
	public void onResourceConnected(String serverIdentifier, String username) throws NoSuchServerException {
		LiveXmppServer server = LiveState.getInstance().getXmppServer(serverIdentifier);
		if (server == null)
			throw new NoSuchServerException(null);
		try {
			server.resourceConnected(Guid.parseJabberId(username));
		} catch (ParseException e) {
			if (!username.equals("admin"))
				logger.warn("Corrupt username passed to onResourceConnected", e);
		}		
	}	
	
	public String getMySpaceName(String username) {
		User user = userFromTrustedUsername(username);
		return user.getAccount().getMySpaceName();
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
			Account acct = user.getAccount();
			ret.add(new MySpaceContactInfo(acct.getMySpaceName(), acct.getMySpaceFriendId()));
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
	
	private User getUserFromUsername(String username) {
		try {
			return identitySpider.lookupGuid(User.class, Guid.parseTrustedJabberId(username));
		} catch (NotFoundException e) {
			throw new RuntimeException("User does not exist: " + username, e);
		}
	}
	
	private Post getPostFromRoomName(UserViewpoint viewpoint, String roomName) throws NotFoundException {
		return postingBoard.loadRawPost(viewpoint, Guid.parseTrustedJabberId(roomName));
	}
	
	private Group getGroupFromRoomName(UserViewpoint viewpoint, String roomName) throws NotFoundException {
		return groupSystem.lookupGroupById(viewpoint, Guid.parseTrustedJabberId(roomName));
	}
	
	private ChatRoomMessage newChatRoomMessage(EmbeddedMessage message) {
		String username = message.getFromUser().getGuid().toJabberId(null);
		return new ChatRoomMessage(username, message.getMessageText(),
				message.getTimestamp(), message.getMessageSerial()); 		
	}
	
	private ChatRoomUser newChatRoomUser(User user) {
		return new ChatRoomUser(user.getGuid().toJabberId(null),
				user.getVersion(), user.getNickname(), user.getPhotoUrl60());
	}
	
	private ChatRoomInfo getChatRoomInfo(String roomName, Group group) {
		List<ChatRoomUser> allowedUsers = new ArrayList<ChatRoomUser>();
		
		Set<User> members = groupSystem.getUserMembers(SystemViewpoint.getInstance(), group);
		for (User user : members) {
			allowedUsers.add(newChatRoomUser(user));
		}
		
		List<GroupMessage> messages = groupSystem.getGroupMessages(group);

		List<ChatRoomMessage> history = new ArrayList<ChatRoomMessage>();
		
		for (GroupMessage m : messages) {
			history.add(newChatRoomMessage(m));
		}
		
		return new ChatRoomInfo(ChatRoomKind.GROUP, roomName, group.getName(), allowedUsers, history);
	}

	private ChatRoomInfo getChatRoomInfo(String roomName, Post post) {
		List<ChatRoomUser> allowedUsers = new ArrayList<ChatRoomUser>();
		
		User poster = post.getPoster();
		allowedUsers.add(newChatRoomUser(poster));
		
		// FIXME: This isn't really right; it doesn't handle public posts and
		// posts where people join a group that it was sent to after the post was
		// sent. Public posts will need to be handled with a separate flag
		// in ChatRoomInfo.
		for (Resource recipient : post.getExpandedRecipients()) {
			User user = identitySpider.getUser(recipient);
			if (user != null && !user.equals(poster))
				allowedUsers.add(newChatRoomUser(user));
		}
		
		List<PostMessage> messages = postingBoard.getPostMessages(post);

		List<ChatRoomMessage> history = new ArrayList<ChatRoomMessage>();
	
		// if post description is not empty, add it to the history of chat room messages, designate this type
		// of message that contains post description with serial = -1
		if (post.getText().trim().length() != 0) {
            ChatRoomMessage message = new ChatRoomMessage(poster.getGuid().toJabberId(null), post.getText(), post.getPostDate(), -1);	 
            history.add(message);
		}
		
		for (PostMessage postMessage : messages) {
			history.add(newChatRoomMessage(postMessage));
		}
		
		return new ChatRoomInfo(ChatRoomKind.POST, roomName, post.getTitle(), allowedUsers, history);
	}
	
	public ChatRoomInfo getChatRoomInfo(String roomName, String initialUsername) {
		User initialUser = getUserFromUsername(initialUsername);
		UserViewpoint viewpoint = new UserViewpoint(initialUser);
		Post post = null;
		Group group = null;
		try {
			post = getPostFromRoomName(viewpoint, roomName);
		} catch (NotFoundException e) {
			// FIXME in principle this should happen if the initialUser can't see the post, 
			// but right now there's no access controls in loadRawPost so it only happens
			// if something is broken
			post = null;
		}
		try {
			group = getGroupFromRoomName(viewpoint, roomName);
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
		
		prefs.put("musicSharingEnabled", Boolean.toString(!account.isDisabled() && account.isMusicSharingEnabled()));

		// not strictly a "pref" but this is a convenient place to send this to the client
		prefs.put("musicSharingPrimed", Boolean.toString(account.isMusicSharingPrimed()));
		
		return prefs;
	}

	public Hotness getUserHotness(String username) {
		User user = userFromTrustedUsername(username);
		LiveState state = LiveState.getInstance();
		return state.getLiveUser(user.getGuid()).getHotness();
	}
	
	static final String RECENT_POSTS_ELEMENT_NAME = "recentPosts";
	static final String RECENT_POSTS_NAMESPACE = "http://dumbhippo.com/protocol/post";
	
	public String getRecentPostsXML(String username, String id) {
		User user = getUserFromUsername(username);
		UserViewpoint viewpoint = new UserViewpoint(user);
		LiveState liveState = LiveState.getInstance();
		List<PostView> views;
		
		if (id != null) {
			PostView view;
			Guid guid;
			
			try {
				guid = new Guid(id);
			} catch (ParseException e) {
				return null;
			}
			
			try {
				view = postingBoard.loadPost(viewpoint, guid);
			} catch (NotFoundException e) {
				return null;
			}	
			
			views = Collections.singletonList(view);
		} else {
			views = postingBoard.getReceivedPosts(viewpoint, user, 0, 4);
		}

		XmlBuilder builder = new XmlBuilder();
		builder.openElement(RECENT_POSTS_ELEMENT_NAME, "xmlns", RECENT_POSTS_NAMESPACE);
		
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
				viewerEntities.add(identitySpider.getPersonView(viewpoint, viewer));
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
}
