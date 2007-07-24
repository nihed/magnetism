package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.IgnoreDependency;
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
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.PostVisibility;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.StackReason;
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
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.GroupChatBlockHandler;
import com.dumbhippo.server.blocks.MusicChatBlockHandler;
import com.dumbhippo.server.blocks.PostBlockHandler;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;

@Stateless
public class ChatSystemBean implements ChatSystem {
	static private final Logger logger = GlobalSetup.getLogger(ChatSystemBean.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	@IgnoreDependency
	private GroupChatBlockHandler groupChatBlockHandler;

	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	@IgnoreDependency
	private MusicChatBlockHandler musicChatBlockHandler;

	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	@IgnoreDependency
	private PostBlockHandler postBlockHandler;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	@IgnoreDependency
	private Stacker stacker;
	
	// We order and select on pm.id, though in rare cases the order by pm.id and by pm.timestamp
	// might be different if two messages arrive almost at once. In this case, the timestamps will
	// likely be within a second of each other and its much cheaper this way.
	private static final String BLOCK_MESSAGE_QUERY = "SELECT pm from BlockMessage pm WHERE pm.block = :block";
	private static final String BLOCK_MESSAGE_SELECT = " AND pm.id >= :lastSeenSerial ";
	private static final String BLOCK_MESSAGE_ORDER = " ORDER BY pm.id";
	
	public List<? extends ChatMessage> getMessages(Block block, long lastSeenSerial) {
		switch (block.getBlockType()) {
		case GROUP_CHAT:
			Group group = em.find(Group.class, block.getData1AsGuid().toString());
			return getGroupMessages(group, lastSeenSerial);
		case MUSIC_CHAT:
			TrackHistory trackHistory = em.find(TrackHistory.class, block.getData2AsGuid().toString());
			return getTrackMessages(trackHistory, lastSeenSerial);
		case POST:
			Post post = em.find(Post.class, block.getData1AsGuid().toString());
			return getPostMessages(post, lastSeenSerial);
		case MUSIC_PERSON:
			// If the caller already has a BlockView and hence the latest TrackHistory,
			// we don't want to look that up again here
			throw new RuntimeException("Use getTrackMessages() instead"); 
		case GROUP_MEMBER:
			return Collections.emptyList();
		default:
			return getBlockMessages(block, lastSeenSerial);
		}
	}
	
	public List<? extends ChatMessage> getNewestMessages(Block block, int maxResults) {
		switch (block.getBlockType()) {
		case GROUP_CHAT:
			Group group = em.find(Group.class, block.getData1AsGuid().toString());
			return getNewestGroupMessages(group, maxResults);
		case MUSIC_CHAT:
			TrackHistory trackHistory = em.find(TrackHistory.class, block.getData2AsGuid().toString());
			return getNewestTrackMessages(trackHistory, maxResults);
		case POST:
			Post post = em.find(Post.class, block.getData1AsGuid().toString());
			return getNewestPostMessages(post, maxResults);
		case MUSIC_PERSON:
			throw new RuntimeException("Use getNewestTrackMessages() instead"); // for efficiency 
		case GROUP_MEMBER:
			return Collections.emptyList();
		default:
			return getNewestBlockMessages(block, maxResults);
		}
	}
		
	public int getMessageCount(Block block) {
		switch (block.getBlockType()) {
		case GROUP_CHAT:
			Group group = em.find(Group.class, block.getData1AsGuid().toString());
			return getGroupMessageCount(group);
		case MUSIC_CHAT:
			TrackHistory trackHistory = em.find(TrackHistory.class, block.getData2AsGuid().toString());
			return getTrackMessageCount(trackHistory);
		case POST:
			Post post = em.find(Post.class, block.getData1AsGuid().toString());
			return getPostMessageCount(post);
		case MUSIC_PERSON:
			throw new RuntimeException("Use getTrackMessageCount() instead"); // for efficiency 
		case GROUP_MEMBER:
			return 0;
		default:	
			return getBlockMessageCount(block);
		}
	}
	
	private List<BlockMessage> getBlockMessages(Block block, long lastSeenSerial) {
		List<?> messages = em.createQuery(BLOCK_MESSAGE_QUERY + BLOCK_MESSAGE_SELECT + BLOCK_MESSAGE_ORDER)
			.setParameter("block", block)
			.setParameter("lastSeenSerial", lastSeenSerial)
			.getResultList();
		
		return TypeUtils.castList(BlockMessage.class, messages);
	}
	
	private List<BlockMessage> getNewestBlockMessages(Block block, int maxResults) {
		List<?> messages = em.createQuery("SELECT bm from BlockMessage bm WHERE bm.block = :block ORDER BY bm.timestamp DESC")
			.setParameter("block", block)
			.setMaxResults(maxResults)
			.getResultList();
		
		return TypeUtils.castList(BlockMessage.class, messages);		
	}
	
	private int getBlockMessageCount(Block block) {
		Query q = em.createQuery("SELECT COUNT(bm) FROM BlockMessage bm WHERE bm.block = :block")
			.setParameter("block", block);
		
		return ((Number)q.getSingleResult()).intValue();
	}
	
	private ChatMessage addBlockMessage(Block block, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		BlockMessage blockMessage = new BlockMessage(block, fromUser, text, sentiment, timestamp);
		em.persist(blockMessage);
		
		return blockMessage;
	}
	
	// We order and select on pm.id, though in rare cases the order by pm.id and by pm.timestamp
	// might be different if two messages arrive almost at once. In this case, the timestamps will
	// likely be within a second of each other and its much cheaper this way.
	private static final String POST_MESSAGE_QUERY = "SELECT pm from PostMessage pm WHERE pm.post = :post";
	private static final String POST_MESSAGE_SELECT = " AND pm.id >= :lastSeenSerial ";
	private static final String POST_MESSAGE_ORDER = " ORDER BY pm.id";
	
	private List<PostMessage> getPostMessages(Post post, long lastSeenSerial) {
		List<?> messages = em.createQuery(POST_MESSAGE_QUERY + POST_MESSAGE_SELECT + POST_MESSAGE_ORDER)
		.setParameter("post", post)
		.setParameter("lastSeenSerial", lastSeenSerial)
		.getResultList();
		
		return TypeUtils.castList(PostMessage.class, messages);
	}
	
	private List<PostMessage> getNewestPostMessages(Post post, int maxResults) {
		List<?> messages = em.createQuery("SELECT pm from PostMessage pm WHERE pm.post = :post ORDER BY pm.timestamp DESC")
		.setParameter("post", post)
		.setMaxResults(maxResults)
		.getResultList();
		
		return TypeUtils.castList(PostMessage.class, messages);		
	}
	
	private int getPostMessageCount(Post post) {
		Query q = em.createQuery("SELECT COUNT(pm) FROM PostMessage pm WHERE pm.post = :post")
			.setParameter("post", post);
		
		return ((Number)q.getSingleResult()).intValue();
	}
	
	private ChatMessage addPostMessage(Post post, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		PostMessage postMessage = new PostMessage(post, fromUser, text, sentiment, timestamp);
		em.persist(postMessage);
		
		return postMessage;
	}

	// We order and select on pm.id, though in rare cases the order by pm.id and by pm.timestamp
	// might be different if two messages arrive almost at once. In this case, the timestamps will
	// likely be within a second of each other and its much cheaper this way.
	private static final String GROUP_MESSAGE_QUERY = "SELECT gm FROM GroupMessage gm WHERE gm.group = :group ";
	private static final String GROUP_MESSAGE_SELECT = " AND gm.id >= :lastSeenSerial ";
	private static final String GROUP_MESSAGE_ORDER = " ORDER BY gm.id";
	
	private List<GroupMessage> getGroupMessages(Group group, long lastSeenSerial) {
		List<?> messages = em.createQuery(GROUP_MESSAGE_QUERY + GROUP_MESSAGE_SELECT + GROUP_MESSAGE_ORDER)
		.setParameter("group", group)
		.setParameter("lastSeenSerial", lastSeenSerial)
		.getResultList();
		
		return TypeUtils.castList(GroupMessage.class, messages);
	}
	
	private List<GroupMessage> getNewestGroupMessages(Group group, int maxResults) {
		List<?> messages =  em.createQuery(GROUP_MESSAGE_QUERY + GROUP_MESSAGE_ORDER + " DESC")
		.setParameter("group", group)
		.setMaxResults(maxResults)
		.getResultList();
		
		return TypeUtils.castList(GroupMessage.class, messages);		
	}
	
	private int getGroupMessageCount(Group group) {
		Query q = em.createQuery("SELECT COUNT(gm) FROM GroupMessage gm WHERE gm.group = :group")
			.setParameter("group", group);
		
		return ((Number)q.getSingleResult()).intValue();
	}

	private ChatMessage addGroupMessage(Group group, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		GroupMessage groupMessage = new GroupMessage(group, fromUser, text, sentiment, timestamp);
		em.persist(groupMessage);

		return groupMessage;
	}

	public List<TrackMessage> getNewestTrackMessages(TrackHistory trackHistory, int maxResults) {
		Query q = em.createQuery("SELECT tm from TrackMessage tm WHERE tm.trackHistory = :trackHistory ORDER by tm.timestamp DESC");
		q.setParameter("trackHistory", trackHistory);
		if (maxResults >= 0)
			q.setMaxResults(maxResults);
		
		return TypeUtils.castList(TrackMessage.class, q.getResultList());
	}

	private List<TrackMessage> getTrackMessages(TrackHistory trackHistory, long lastSeenSerial) {
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
	
	private ChatMessage addTrackMessage(TrackHistory trackHistory, User fromUser, String text, Sentiment sentiment, Date timestamp) {
		TrackMessage trackMessage = new TrackMessage(trackHistory, fromUser, text, timestamp, sentiment);
		em.persist(trackMessage);
		
		return trackMessage;
	}
	
	public List<ChatMessageView> viewMessages(List<? extends ChatMessage> messages, Viewpoint viewpoint) {
		List<ChatMessageView> viewedMsgs = new ArrayList<ChatMessageView>();
		for (ChatMessage m : messages) {
			viewedMsgs.add(new ChatMessageView(m, personViewer.getPersonView(viewpoint, m.getFromUser())));
		}
		return viewedMsgs;
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

	private Block getBlockForRoom(Guid roomGuid) throws NotFoundException {
		return stacker.lookupBlock(roomGuid);
	}
	
	private ChatRoomUser newChatRoomUser(User user) {
		return new ChatRoomUser(user.getGuid().toJabberId(null),
				                user.getNickname(), user.getPhotoUrl());
	}
	
	private ChatRoomInfo getChatRoomInfo(Guid roomGuid, Group group, boolean includeHistory) {
		List<? extends ChatMessage> history = includeHistory ? getGroupMessages(group, -1) : null;
		return new ChatRoomInfo(ChatRoomKind.GROUP, roomGuid, group.getName(), history, false);
	}

	private ChatRoomInfo getChatRoomInfo(Guid roomGuid, Post post, boolean includeHistory) {
		boolean worldAccessible = true;
		if (post.getVisibility() == PostVisibility.RECIPIENTS_ONLY)
			worldAccessible = false;
		
		List<? extends ChatMessage> history = includeHistory ? getPostMessages(post, -1) : null;
		return new ChatRoomInfo(ChatRoomKind.POST, roomGuid, post.getTitle(), history, worldAccessible);
	}
	
	private ChatRoomInfo getChatRoomInfo(Guid roomGuid, TrackHistory trackHistory, boolean includeHistory) {
		TrackView trackView = musicSystem.getTrackView(trackHistory);
		List<? extends ChatMessage> history = includeHistory ? getTrackMessages(trackHistory, -1) : null;
		
		return new ChatRoomInfo(ChatRoomKind.MUSIC, roomGuid, trackView.getDisplayTitle(), history, true);
	}
	
	private ChatRoomInfo getChatRoomInfo(Guid roomGuid, Block block, boolean includeHistory) throws NotFoundException {
		BlockView blockView = stacker.loadBlock(SystemViewpoint.getInstance(), block);
		List<? extends ChatMessage> history = includeHistory ? getBlockMessages(block, -1) : null;
		
		return new ChatRoomInfo(ChatRoomKind.BLOCK, roomGuid, blockView.getSummaryLinkText(), history, block.isPublicBlock());
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
	
	public ChatRoomInfo getChatRoomInfo(Guid roomGuid, boolean includeHistory) {
		Post post = null;
		Group group = null;
		TrackHistory trackHistory = null;
		Block block = null;
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
		try {
			block = getBlockForRoom(roomGuid);
			if (block.getBlockType().isDirectlyChattable())
				count++;
			else
				block = null;
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
			return getChatRoomInfo(roomGuid, post, includeHistory);
		else if (group != null)
			return getChatRoomInfo(roomGuid, group, includeHistory);
		else if (trackHistory != null)
			return getChatRoomInfo(roomGuid, trackHistory, includeHistory);
		else if (block != null) {
			try {
				return getChatRoomInfo(roomGuid, block, includeHistory);
			} catch (NotFoundException e) {
				logger.debug("Block not visible for chat room {}", roomGuid.toString());
				return null;
			}
		} else {
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
		case BLOCK:
			Block block;
			try {
				block = stacker.lookupBlock(roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("Block not found", e);
			}
			return getBlockMessages(block, lastSeenSerial);
		}
		throw new IllegalArgumentException("Bad chat room type");
	}

	public void addChatRoomMessage(Guid roomGuid, ChatRoomKind kind, UserViewpoint fromViewpoint, String text, Sentiment sentiment, Date timestamp) throws RetryException {
		User fromUser = fromViewpoint.getViewer();
		Block block;
		ChatMessage message;
		
		switch (kind) {
		case POST:
			Post post;
			try {
				post = postingBoard.loadRawPost(fromViewpoint, roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("post chat not found", e);
			}
			block = postBlockHandler.lookupBlock(post);
			message = addPostMessage(post, fromUser, text, sentiment, timestamp);
			break;
		case GROUP:
			Group group;
			try {
				group = groupSystem.lookupGroupById(fromViewpoint, roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("group chat not found", e);
			}
			
			block = groupChatBlockHandler.lookupBlock(group);
			message = addGroupMessage(group, fromUser, text, sentiment, timestamp);
			break;
		case MUSIC:
			TrackHistory trackHistory;
			try {
				trackHistory = musicSystem.lookupTrackHistory(roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("track not found", e);
			}
			// We don't create these blocks on TrackHistory creation, since most TrackHistory
			// objects will never be chatted on, so we have to demand create at this time
			block = stacker.getOrCreateBlock(musicChatBlockHandler.getKey(trackHistory));
			message = addTrackMessage(trackHistory, fromUser, text, sentiment, timestamp);
			break;
		case BLOCK:
			try {
				block = stacker.lookupBlock(roomGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("Block not found", e);
			}
			message = addBlockMessage(block, fromUser, text, sentiment, timestamp);
			break;
		default:
			throw new RuntimeException("Can't add a chat message to a chat room of unknown kind");
		}

		stacker.stack(block, message.getTimestamp().getTime(),
					  message.getFromUser(),
					  block.getBlockType().isChatGroupParticipation(),
					  StackReason.CHAT_MESSAGE);
		
		LiveState.getInstance().queueUpdate(new ChatRoomEvent(roomGuid, ChatRoomEvent.Detail.MESSAGES_CHANGED));
	}
	
	public boolean canJoinChat(Guid roomGuid, ChatRoomKind kind, UserViewpoint viewpoint) {
		User user = viewpoint.getViewer();
		switch (kind) {
		case POST:
			try {
				Post post = getPostForRoom(roomGuid);
				return postingBoard.canViewPost(viewpoint, post);
			} catch (NotFoundException e) {
				return false;
			}
		case GROUP:
			// We differ a bit from what we have for other chat types; to
			// chat on a group you have to be a member, not just able to
			// see the group
			try {
				Group group = getGroupForRoom(roomGuid);
				GroupMember member = groupSystem.getGroupMember(viewpoint, group, user);
				return member.getStatus().getCanChat();
			} catch (NotFoundException e) {
				return false;
			}
		case MUSIC:
			// Music blocks are always visible, so everybody can chat on them
			return true;
		case BLOCK:
			try {
				// Most blocks are chattable for anybody that can see them if
				// they are visbile. There are some exceptions like
				// GroupRevision; those are handled in the getChatId()
				// implementation.
				
				Block block = stacker.lookupBlock(roomGuid);
				BlockView blockView = stacker.loadBlock(viewpoint, block);
				return blockView.getChatId() != null;
			} catch (NotFoundException e) {
				return false;
			}
		default:
			throw new RuntimeException("Unknown chat room type " + kind);
		}
	}	
}
