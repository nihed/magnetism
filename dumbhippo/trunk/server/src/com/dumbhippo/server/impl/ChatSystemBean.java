package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.ChatRoomEvent;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockMessage;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.TrackHistory;
import com.dumbhippo.persistence.TrackMessage;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ChatRoomInfo;
import com.dumbhippo.server.ChatRoomKind;
import com.dumbhippo.server.ChatRoomUser;
import com.dumbhippo.server.ChatSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Stateless
public class ChatSystemBean implements ChatSystem {
	static private final Logger logger = GlobalSetup.getLogger(ChatSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private Notifier notifier;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	private PostingBoard postingBoard;
	
	// We order and select on pm.id, though in rare cases the order by pm.id and by pm.timestamp
	// might be different if two messages arrive almost at once. In this case, the timestamps will
	// likely be within a second of each other and its much cheaper this way.
	private static final String BLOCK_MESSAGE_QUERY = "SELECT pm from BlockMessage pm WHERE pm.block = :block";
	private static final String BLOCK_MESSAGE_SELECT = " AND pm.id >= :lastSeenSerial ";
	private static final String BLOCK_MESSAGE_ORDER = " ORDER BY pm.id";
	
	public List<BlockMessage> getBlockMessages(Block block, long lastSeenSerial) {
		List<?> messages = em.createQuery(BLOCK_MESSAGE_QUERY + BLOCK_MESSAGE_SELECT + BLOCK_MESSAGE_ORDER)
		.setParameter("block", block)
		.setParameter("lastSeenSerial", lastSeenSerial)
		.getResultList();
		
		return TypeUtils.castList(BlockMessage.class, messages);
	}
	
	public List<BlockMessage> getNewestBlockMessages(Block block, int maxResults) {
		List<?> messages = em.createQuery("SELECT pm from BlockMessage pm WHERE pm.block = :block ORDER BY pm.timestamp DESC")
		.setParameter("block", block)
		.setMaxResults(maxResults)
		.getResultList();
		
		return TypeUtils.castList(BlockMessage.class, messages);		
	}
	
	public int getBlockMessageCount(Block block) {
		Query q = em.createQuery("SELECT COUNT(pm) FROM BlockMessage pm WHERE pm.block = :block")
			.setParameter("block", block);
		
		return ((Number)q.getSingleResult()).intValue();
	}
	
//	private void addBlockMessage(Block block, User fromUser, String text, Sentiment sentiment, Date timestamp) {
//		BlockMessage blockMessage = new BlockMessage(block, fromUser, text, sentiment, timestamp);
//		em.persist(blockMessage);
//		
//		notifier.onBlockMessageCreated(blockMessage);
//	}
	
	// We order and select on pm.id, though in rare cases the order by pm.id and by pm.timestamp
	// might be different if two messages arrive almost at once. In this case, the timestamps will
	// likely be within a second of each other and its much cheaper this way.
	private static final String POST_MESSAGE_QUERY = "SELECT pm from PostMessage pm WHERE pm.post = :post";
	private static final String POST_MESSAGE_SELECT = " AND pm.id >= :lastSeenSerial ";
	private static final String POST_MESSAGE_ORDER = " ORDER BY pm.id";
	
	public List<PostMessage> getPostMessages(Post post, long lastSeenSerial) {
		List<?> messages = em.createQuery(POST_MESSAGE_QUERY + POST_MESSAGE_SELECT + POST_MESSAGE_ORDER)
		.setParameter("post", post)
		.setParameter("lastSeenSerial", lastSeenSerial)
		.getResultList();
		
		return TypeUtils.castList(PostMessage.class, messages);
	}
	
	public List<PostMessage> getNewestPostMessages(Post post, int maxResults) {
		List<?> messages = em.createQuery("SELECT pm from PostMessage pm WHERE pm.post = :post ORDER BY pm.timestamp DESC")
		.setParameter("post", post)
		.setMaxResults(maxResults)
		.getResultList();
		
		return TypeUtils.castList(PostMessage.class, messages);		
	}
	
	public int getPostMessageCount(Post post) {
		Query q = em.createQuery("SELECT COUNT(pm) FROM PostMessage pm WHERE pm.post = :post")
			.setParameter("post", post);
		
		return ((Number)q.getSingleResult()).intValue();
	}
	
	public List<ChatMessageView> viewPostMessages(List<PostMessage> messages, Viewpoint viewpoint) {
		List<ChatMessageView> viewedMsgs = new ArrayList<ChatMessageView>();
		for (PostMessage m : messages) {
			viewedMsgs.add(new ChatMessageView(m, personViewer.getPersonView(viewpoint, m.getFromUser())));
		}
		return viewedMsgs;
	}	
	
	private void addPostMessage(Post post, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		PostMessage postMessage = new PostMessage(post, fromUser, text, sentiment, timestamp);
		em.persist(postMessage);
		
		notifier.onPostMessageCreated(postMessage);
	}

	// We order and select on pm.id, though in rare cases the order by pm.id and by pm.timestamp
	// might be different if two messages arrive almost at once. In this case, the timestamps will
	// likely be within a second of each other and its much cheaper this way.
	private static final String GROUP_MESSAGE_QUERY = "SELECT gm FROM GroupMessage gm WHERE gm.group = :group ";
	private static final String GROUP_MESSAGE_SELECT = " AND gm.id >= :lastSeenSerial ";
	private static final String GROUP_MESSAGE_ORDER = " ORDER BY gm.id";
	
	public List<GroupMessage> getGroupMessages(Group group, long lastSeenSerial) {
		List<?> messages = em.createQuery(GROUP_MESSAGE_QUERY + GROUP_MESSAGE_SELECT + GROUP_MESSAGE_ORDER)
		.setParameter("group", group)
		.setParameter("lastSeenSerial", lastSeenSerial)
		.getResultList();
		
		return TypeUtils.castList(GroupMessage.class, messages);
	}
	
	public List<GroupMessage> getNewestGroupMessages(Group group, int maxResults) {
		List<?> messages =  em.createQuery(GROUP_MESSAGE_QUERY + GROUP_MESSAGE_ORDER + " DESC")
		.setParameter("group", group)
		.setMaxResults(maxResults)
		.getResultList();
		
		return TypeUtils.castList(GroupMessage.class, messages);		
	}
	
	public int getGroupMessageCount(Group group) {
		Query q = em.createQuery("SELECT COUNT(gm) FROM GroupMessage gm WHERE gm.group = :group")
			.setParameter("group", group);
		
		return ((Number)q.getSingleResult()).intValue();
	}

	private void addGroupMessage(Group group, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		GroupMessage groupMessage = new GroupMessage(group, fromUser, text, sentiment, timestamp);
		em.persist(groupMessage);

		notifier.onGroupMessageCreated(groupMessage);
	}

	public List<ChatMessageView> viewMessages(List<? extends ChatMessage> messages, Viewpoint viewpoint) {
		List<ChatMessageView> viewedMsgs = new ArrayList<ChatMessageView>();
		for (ChatMessage m : messages) {
			viewedMsgs.add(new ChatMessageView(m, personViewer.getPersonView(viewpoint, m.getFromUser())));
		}
		return viewedMsgs;
	}

	public List<TrackMessage> getNewestTrackMessages(TrackHistory trackHistory, int maxResults) {
		Query q = em.createQuery("SELECT tm from TrackMessage tm WHERE tm.trackHistory = :trackHistory ORDER by tm.timestamp DESC");
		q.setParameter("trackHistory", trackHistory);
		if (maxResults >= 0)
			q.setMaxResults(maxResults);
		
		return TypeUtils.castList(TrackMessage.class, q.getResultList());
	}

	public List<TrackMessage> getTrackMessages(TrackHistory trackHistory, long lastSeenSerial) {
		Query q = em.createQuery("SELECT tm from TrackMessage tm WHERE tm.trackHistory = :trackHistory AND tm.id >= :lastSeenSerial ORDER by tm.id");
		q.setParameter("trackHistory", trackHistory);
		q.setParameter("lastSeenSerial", lastSeenSerial);
		
		return TypeUtils.castList(TrackMessage.class, q.getResultList());
	}
	
	public int getTrackMessageCount(TrackHistory trackHistory) {
		Query q = em.createQuery("SELECT count(tm) from TrackMessage tm WHERE tm.trackHistory = :trackHistory");
		q.setParameter("trackHistory", trackHistory);
		
		return ((Number)q.getSingleResult()).intValue();
	}
	
	private void addTrackMessage(TrackHistory trackHistory, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		TrackMessage trackMessage = new TrackMessage(trackHistory, fromUser, text, timestamp, sentiment);
		em.persist(trackMessage);
		
		notifier.onTrackMessageCreated(trackMessage);
	}
	
	private User getUserFromGuid(Guid guid) {
		try {
			return identitySpider.lookupGuid(User.class, guid);
		} catch (NotFoundException e) {
			throw new RuntimeException("User does not exist: " + guid, e);
		}
	}
	
	private Post getPostForRoom(Guid roomGuid) throws NotFoundException {
		return postingBoard.loadRawPost(SystemViewpoint.getInstance(), roomGuid);
	}
	
	private Group getGroupForRoom(Guid roomGuid) throws NotFoundException {
		return groupSystem.lookupGroupById(SystemViewpoint.getInstance(), roomGuid);
	}
	
	private TrackHistory getTrackHistoryForRoom(Guid roomGuid) throws NotFoundException {
		return musicSystem.lookupTrackHistory(roomGuid);
	}

	private ChatRoomUser newChatRoomUser(User user) {
		return new ChatRoomUser(user.getGuid().toJabberId(null),
				                user.getNickname(), user.getPhotoUrl());
	}
	
	private ChatRoomInfo getChatRoomInfo(Guid roomGuid, Group group) {
		List<? extends ChatMessage> history = getGroupMessages(group, -1);
		return new ChatRoomInfo(ChatRoomKind.GROUP, roomGuid, group.getName(), history, false);
	}

	private ChatRoomInfo getChatRoomInfo(Guid roomGuid, Post post) {
		boolean worldAccessible = true;
		if (post.getVisibility() == PostVisibility.RECIPIENTS_ONLY)
			worldAccessible = false;
		
		List<? extends ChatMessage> history = getPostMessages(post, -1);
		return new ChatRoomInfo(ChatRoomKind.POST, roomGuid, post.getTitle(), history, worldAccessible);
	}
	
	private ChatRoomInfo getChatRoomInfo(Guid roomGuid, TrackHistory trackHistory) {
		TrackView trackView = musicSystem.getTrackView(trackHistory);
		List<? extends ChatMessage> history = getTrackMessages(trackHistory, -1);
		
		return new ChatRoomInfo(ChatRoomKind.MUSIC, roomGuid, trackView.getDisplayTitle(), history, true);
	}
	
	public ChatRoomUser getChatRoomUser(Guid roomGuid, ChatRoomKind kind, String username) {
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
	
	public ChatRoomInfo getChatRoomInfo(Guid roomGuid) {
		Post post = null;
		Group group = null;
		TrackHistory trackHistory = null;
		int count = 0;
		try {
			post = getPostForRoom(roomGuid);
			count++;
		} catch (NotFoundException e) {
		}
		try {
			group = getGroupForRoom(roomGuid);
			count++;
		} catch (NotFoundException e) {
		}
		try {
			trackHistory = getTrackHistoryForRoom(roomGuid);
			count++;
		} catch (NotFoundException e) {
		}
		
		if (count > 1) {
			// this should theoretically be very, very unlikely so we won't bother to fix it unless 
			// it happens on production, and even then we could just munge the database instead of 
			// bothering...
			logger.error("GUID collision... we'll have to put a type marker in the room ID string");
			// we'll just guess
		}
		
		if (post != null)
			return getChatRoomInfo(roomGuid, post);
		else if (group != null)
			return getChatRoomInfo(roomGuid, group);
		else if (trackHistory != null)
			return getChatRoomInfo(roomGuid, trackHistory);
		else {
			logger.debug("Room name {} doesn't correspond to a post or group, or user not allowed to see it", roomGuid.toString());
			return null;
		}
	}
	
	public List<? extends ChatMessage> getChatRoomMessages(Guid roomGuid, ChatRoomKind kind, long lastSeenSerial) {
		switch (kind) {
		case POST:
			Post post;
			try {
				post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("post chat not found", e);
			}
			return getPostMessages(post, lastSeenSerial);
		case GROUP:
			Group group;
			try {
				group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("group chat not found", e);
			}
			return getGroupMessages(group, lastSeenSerial);
		case MUSIC:
			TrackHistory trackHistory;
			try {
				trackHistory = musicSystem.lookupTrackHistory(roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("Track not found", e);
			}
			return getTrackMessages(trackHistory, lastSeenSerial);
		}
		throw new IllegalArgumentException("Bad chat room type");
	}

	public void addChatRoomMessage(Guid roomGuid, ChatRoomKind kind, Guid userId, String text, Sentiment sentiment, Date timestamp) {
		User fromUser = getUserFromGuid(userId);
		UserViewpoint viewpoint = new UserViewpoint(fromUser);
		switch (kind) {
		case POST:
			Post post;
			try {
				post = postingBoard.loadRawPost(viewpoint, roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("post chat not found", e);
			}
			addPostMessage(post, fromUser, text, sentiment, timestamp);
			break;
		case GROUP:
			Group group;
			try {
				group = groupSystem.lookupGroupById(viewpoint, roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("group chat not found", e);
			}
			addGroupMessage(group, fromUser, text, sentiment, timestamp);
			break;
		case MUSIC:
			TrackHistory trackHistory;
			try {
				trackHistory = musicSystem.lookupTrackHistory(roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("track not found", e);
			}
			addTrackMessage(trackHistory, fromUser, text, sentiment, timestamp);
			break;
		}
		
		LiveState.getInstance().queueUpdate(new ChatRoomEvent(roomGuid, ChatRoomEvent.Detail.MESSAGES_CHANGED));
	}

	public boolean canJoinChat(Guid roomGuid, ChatRoomKind kind, Guid userId) {
		try {
			User user = getUserFromGuid(userId);
			UserViewpoint viewpoint = new UserViewpoint(user);
			if (kind == ChatRoomKind.POST) {
				Post post = getPostForRoom(roomGuid);
				return postingBoard.canViewPost(viewpoint, post);
			} else if (kind == ChatRoomKind.GROUP) {
				Group group = getGroupForRoom(roomGuid);
				return groupSystem.isMember(group, user);
			} else if (kind == ChatRoomKind.MUSIC) {
				TrackHistory trackHistory = getTrackHistoryForRoom(roomGuid);
				identitySpider.isViewerSystemOrFriendOf(viewpoint, trackHistory.getUser());
				return true;
			} else
				throw new RuntimeException("Unknown chat room type " + kind);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
	}	
}
