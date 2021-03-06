package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.ejb.Service;
import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.ThreadUtils.DaemonRunnable;
import com.dumbhippo.dm.ReadWriteSession;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.BlockEvent;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.FeedPost;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.StackFilterFlags;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.ChatSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.XmppMessageSender;
import com.dumbhippo.server.blocks.AccountQuestionBlockHandler;
import com.dumbhippo.server.blocks.AccountQuestionBlockView;
import com.dumbhippo.server.blocks.AmazonActivityBlockHandler;
import com.dumbhippo.server.blocks.BlockHandler;
import com.dumbhippo.server.blocks.BlockNotVisibleException;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.BlogBlockHandler;
import com.dumbhippo.server.blocks.DeliciousBlockHandler;
import com.dumbhippo.server.blocks.DiggBlockHandler;
import com.dumbhippo.server.blocks.FacebookBlockHandler;
import com.dumbhippo.server.blocks.FlickrPersonBlockHandler;
import com.dumbhippo.server.blocks.FlickrPhotosetBlockHandler;
import com.dumbhippo.server.blocks.GoogleReaderBlockHandler;
import com.dumbhippo.server.blocks.GroupChatBlockHandler;
import com.dumbhippo.server.blocks.GroupMemberBlockHandler;
import com.dumbhippo.server.blocks.GroupRevisionBlockHandler;
import com.dumbhippo.server.blocks.MusicChatBlockHandler;
import com.dumbhippo.server.blocks.MusicPersonBlockHandler;
import com.dumbhippo.server.blocks.MySpacePersonBlockHandler;
import com.dumbhippo.server.blocks.NetflixBlockHandler;
import com.dumbhippo.server.blocks.PicasaBlockHandler;
import com.dumbhippo.server.blocks.SmugmugBlockHandler;
import com.dumbhippo.server.blocks.PostBlockHandler;
import com.dumbhippo.server.blocks.RedditBlockHandler;
import com.dumbhippo.server.blocks.TwitterPersonBlockHandler;
import com.dumbhippo.server.blocks.YouTubeBlockHandler;
import com.dumbhippo.server.dm.BlockDMO;
import com.dumbhippo.server.dm.BlockDMOKey;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.UserClientMatcher;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonMugshotView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxCallable;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

@Service
public class StackerBean implements Stacker, SimpleServiceMBean, LiveEventListener<BlockEvent> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackerBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	private ChatSystem chatSystem;
	
	@EJB
	private XmppMessageSender xmppMessageSystem;	
	
	@EJB
	private FacebookTracker facebookTracker;
	
	private Map<BlockType,BlockHandler> handlers;
	
	private BlockHandler getHandler(Block block) {
		return getHandler(block.getBlockType());
	}
	
	private BlockHandler getHandler(BlockView blockView) {
		return getHandler(blockView.getBlockType());
	}
	
	private BlockHandler getHandler(BlockType type) {
		
		if (handlers != null) {
			BlockHandler handler = handlers.get(type);
			if (handler != null)
				return handler;
		}
		
		Class<? extends BlockHandler> handlerClass = null;
		
		switch (type) {
		case BLOG_ENTRY:
			handlerClass = BlogBlockHandler.class;
			break;
		case FACEBOOK_PERSON:
		case FACEBOOK_EVENT:	
			handlerClass = FacebookBlockHandler.class;
			break;
		case GROUP_CHAT:
			handlerClass = GroupChatBlockHandler.class;
			break;			
		case GROUP_MEMBER:
			handlerClass = GroupMemberBlockHandler.class;
			break;			
		case MUSIC_CHAT:
			handlerClass = MusicChatBlockHandler.class;
			break;
		case MUSIC_PERSON:
			handlerClass = MusicPersonBlockHandler.class;
			break;			
		case POST:
			handlerClass = PostBlockHandler.class;
			break;
		case FLICKR_PERSON:
			handlerClass = FlickrPersonBlockHandler.class;
			break;
		case FLICKR_PHOTOSET:
			handlerClass = FlickrPhotosetBlockHandler.class;
			break;
		case YOUTUBE_PERSON:
			handlerClass = YouTubeBlockHandler.class;
			break;
		case MYSPACE_PERSON:
			handlerClass = MySpacePersonBlockHandler.class;
			break;
		case DELICIOUS_PUBLIC_BOOKMARK:
			handlerClass = DeliciousBlockHandler.class;
			break;
		case TWITTER_PERSON:
			handlerClass = TwitterPersonBlockHandler.class;
			break;
		case DIGG_DUGG_ENTRY:
			handlerClass = DiggBlockHandler.class;
			break;
		case REDDIT_ACTIVITY_ENTRY:
			handlerClass = RedditBlockHandler.class;
			break;
		case GROUP_REVISION:
			handlerClass = GroupRevisionBlockHandler.class;
			break;
		case NETFLIX_MOVIE:
			handlerClass = NetflixBlockHandler.class;
			break;
		case ACCOUNT_QUESTION:
			handlerClass = AccountQuestionBlockHandler.class;
			break;
		case GOOGLE_READER_SHARED_ITEM:
			handlerClass = GoogleReaderBlockHandler.class;
			break;
		case PICASA_PERSON:
			handlerClass = PicasaBlockHandler.class;
			break;
		case SMUGMUG_PERSON:
			handlerClass = SmugmugBlockHandler.class;
			break;
		case AMAZON_REVIEW:
			handlerClass = AmazonActivityBlockHandler.class;
			break;
		case AMAZON_WISH_LIST_ITEM:
			handlerClass = AmazonActivityBlockHandler.class;
			break;			
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE:
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF:
		case OBSOLETE_BLOG_PERSON:
			break;
			// don't add a default, it hides compiler warnings
		}
		
		if (handlerClass == null)
			throw new RuntimeException("No block handler known for block type " + type);
		
		if (handlers == null)
			handlers = new EnumMap<BlockType,BlockHandler>(BlockType.class);
		
		BlockHandler handler = EJBUtil.defaultLookup(handlerClass);
		handlers.put(type, handler);
		
		return handler;
	}
	
	// All uses of this method indicate something that needs refactoring to be in 
	// the block handler beans instead of this file
	private <SpecificBlockHandler> SpecificBlockHandler
		getHandler(Class<SpecificBlockHandler> klass, BlockType type) {
		BlockHandler handler = getHandler(type);
		return klass.cast(handler);
	}
	
	public void start() throws Exception {
		LiveState.addEventListener(BlockEvent.class, this);
	}

	public void stop() throws Exception {
		LiveState.removeEventListener(BlockEvent.class, this);
	}
	
	private String getBlockClause(BlockKey key) {
		Guid data1 = key.getData1();
		Guid data2 = key.getData2();
		if (data1 != null && data2 != null) {
			return "block.blockType=:type " +
				   "AND block.data1=:data1 AND block.data2=:data2 AND block.data3=:data3 " + 
				   "AND block.inclusion = :inclusion";
		} else if (data1 != null) {
			return "block.blockType=:type " +
				   "AND block.data1=:data1 " + (key.isData2Optional() ? "" : " AND block.data2='' ") + " AND block.data3=:data3 " + 
				   "AND block.inclusion = :inclusion";
		} else if (data2 != null) {
			return "block.blockType=:type " +
				   "AND block.data2=:data2 AND block.data1='' AND block.data3=:data3 " +
				   "AND block.inclusion = :inclusion";
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block type " + key.getBlockType());
		}
	}
	
	private void setBlockParameters(BlockKey key, Query q) {
		Guid data1 = key.getData1();
		Guid data2 = key.getData2();
		long data3 = key.getData3();
		StackInclusion inclusion = key.getInclusion();

		if (data1 != null && data2 != null) {
			q.setParameter("data1", data1.toString());
			q.setParameter("data2", data2.toString());			
		} else if (data1 != null) {
			q.setParameter("data1", data1.toString());
		} else if (data2 != null) {
			q.setParameter("data2", data2.toString());	
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block type " + key.getBlockType());
		}
		q.setParameter("data3", data3);
		q.setParameter("type", key.getBlockType());
		if (inclusion == null)
			throw new IllegalArgumentException("BlockKey should not have null inclusion" + key);
		q.setParameter("inclusion", inclusion);
	}
	
	public Block queryBlock(BlockKey key) throws NotFoundException {
		Query q = em.createQuery("SELECT block FROM Block block WHERE " + getBlockClause(key));
		setBlockParameters(key, q);

		try {
			return (Block) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("no block with key " + key, e);
		}
	}
	
	public Block lookupBlock(Guid guid) throws NotFoundException {
		Block block = em.find(Block.class, guid.toString());
		if (block == null)
			throw new NotFoundException("No such block");
		
		return block;
	}
	
	private UserBlockData queryUserBlockData(User user, BlockKey key) throws NotFoundException {
		Query q = em.createQuery(
				"SELECT ubd FROM UserBlockData ubd, Block block" +
				" WHERE ubd.block = block" +
				"   AND ubd.user = :user " +
				"   AND " + getBlockClause(key));
		setBlockParameters(key, q);
		q.setParameter("user", user);
		try {
			return (UserBlockData) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("no UserBlockData with block key " + key, e);
		}
	}
	
	public Block createBlock(BlockKey key) {
		Block block = new Block(key);
		em.persist(block);
		return block;
	}
	
	/** don't call this awkward method directly, call one of the two other overloads 
	 * @throws RetryException */
	private Block getOrCreateBlock(final BlockKey key, final boolean changeDefaultPublicity, final boolean publicBlockIfCreated) throws RetryException {
		return TxUtils.runNeedsRetry(new TxCallable<Block>() {
			public Block call() throws RetryException {
				try {
					return queryBlock(key);
				} catch (NotFoundException e) {
					Block block = createBlock(key);
					if (changeDefaultPublicity)
						block.setPublicBlock(publicBlockIfCreated);
					return block;
				}
			}
		});
	}
	
	private Block getOrCreateBlock(BlockKey key, boolean publicBlockIfCreated) throws RetryException {
		return getOrCreateBlock(key, true, publicBlockIfCreated);
	}
	
	public Block getOrCreateBlock(BlockKey key) throws RetryException {
		return getOrCreateBlock(key, false, false);
	}
	
	// note this query includes ubd.deleted=1
	private List<UserBlockData> queryUserBlockDatas(Block block) {
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd WHERE ubd.block = :block");
		q.setParameter("block", block);
		return TypeUtils.castList(UserBlockData.class, q.getResultList());
	}
	
	private UserBlockData queryUserBlockData(Block block, User user) throws NoResultException {
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd" +
				                 " WHERE ubd.block = :block AND ubd.user = :user");
		q.setParameter("block", block);
		q.setParameter("user", user);
		try {
		    return (UserBlockData)q.getSingleResult();
		} catch (NonUniqueResultException e) {
			throw new RuntimeException("NonUniqueResultException when getting a UserBlockData", e);
		} catch (IllegalStateException e) {
			throw new RuntimeException("IllegalStateException when getting a UserBlockData", e);
		}
	}
	
	private Pair<Long, StackReason> updateParticipantUserBlockData(final Block block, final Guid participantId, final StackReason reason) throws RetryException {
		return TxUtils.runNeedsRetry(new TxCallable<Pair<Long, StackReason>>() {
			public Pair<Long, StackReason> call() throws RetryException {
				Pair<Long, StackReason> previousParticipation = new Pair<Long, StackReason>(Long.valueOf(-1), null);
				User participant = em.find(User.class, participantId.toString());
				UserBlockData userData = queryUserBlockData(block, participant);
				boolean reasonChanged = false;
				if (userData != null) {
					userData.setDeleted(false);
					
					if (userData.getParticipated())
						previousParticipation = new Pair<Long, StackReason>(userData.getParticipatedTimestamp().getTime(), userData.getParticipatedReason());
					
					userData.setParticipatedTimestamp(block.getTimestamp());
					userData.setParticipatedReason(reason);
					if (!userData.isIgnored())
						userData.setStackTimestamp(block.getTimestamp());
					
					if (reason != userData.getStackReason()) {
						userData.setStackReason(reason);
						reasonChanged = true;
					}
				} else {
					UserBlockData data = new UserBlockData(participant, block, true, reason);
					em.persist(data);
				}
				
				ReadWriteSession session = DataService.currentSessionRW();
				
				// The stackReason is actually viewer dependent, and currently comes
				// from the UserBlockData of the viewer, so notifying just userData.getUser()
				// would work, but long-term we sometimes want to use another UserBlockData;
				// to be forward-looking, we just notify the stackReason globally.
				if (reasonChanged)
					session.changed(BlockDMO.class, new BlockDMOKey(block), "stackReason");		
				
				session.feedChanged(UserDMO.class, participantId, "stack", block.getTimestampAsLong());
				
				return previousParticipation;
			}
		});
	}
	
	// Don't call directly, RetryException is added in the wrapper for readability
	// returns a pair with information about previous participation by the current participant
	private Pair<Long, StackReason> updateUserBlockDatasInternal(Block block, Set<User> desiredUsers, Guid participantId, StackReason reason) {
		ReadWriteSession session = DataService.getModel().currentSessionRW();

		int addCount;
		int removeCount;
		boolean reasonChanged = false; // For any user
		
		Set<Guid> affectedGuids = new HashSet<Guid>();
		
		Pair<Long, StackReason> previousParticipation = new Pair<Long, StackReason>(Long.valueOf(-1), null);
		
		// be sure we have the right UserBlockData. This would be a lot saner to do
		// at the point where it changes... e.g. when people add/remove friends, 
		// or join/leave groups, instead of the expensive query and fixup here.
		// But it would not retroactively fix the existing db or let us change our
		// rules for who gets what...
		//
		// Also, we need to query UserBlockData anyway to update the user 
		// timestamp caches and maybe send out XMPP notifications eventually,
		// so maybe this isn't really adding too much overhead.
		
		List<UserBlockData> userDatas = queryUserBlockDatas(block);
		
		Map<User,UserBlockData> existing = new HashMap<User,UserBlockData>();
		for (UserBlockData ubd : userDatas) {
			existing.put(ubd.getUser(), ubd);
		}
		
		addCount = 0;
		removeCount = 0;
		
		for (User u : desiredUsers) {
			affectedGuids.add(u.getGuid());
			
			UserBlockData old = existing.get(u);
			if (old != null) {
				existing.remove(u);
				if (old.isDeleted())
					addCount += 1;
				old.setDeleted(false);
				if (u.getGuid().equals(participantId)) {
					
					if (old.getParticipated())
						previousParticipation = new Pair<Long, StackReason>(old.getParticipatedTimestamp().getTime(), old.getParticipatedReason());

					old.setParticipatedTimestamp(block.getTimestamp());
					old.setParticipatedReason(reason);
				}
				if (!old.isIgnored())
					old.setStackTimestamp(block.getTimestamp());
				if (reason != old.getStackReason()) {
					old.setStackReason(reason);
					reasonChanged = true;
				}
			} else {
				UserBlockData data = new UserBlockData(u, block, u.getGuid().equals(participantId), reason);
				em.persist(data);
				addCount += 1;
			}
			
			session.feedChanged(UserDMO.class, u.getGuid(), "stack", block.getTimestampAsLong());
		}
		// the rest of "existing" is users who no longer are in the desired set
		for (User u : existing.keySet()) {
			UserBlockData old = existing.get(u);
			if (!old.isDeleted())
				removeCount += 1;
			old.setDeleted(true);
			if (u.getGuid().equals(participantId)) {
				logger.warn("The user {} who was no longer in a set of users who care was a participant for block {}",
						    u, block);
				old.setParticipatedTimestamp(block.getTimestamp());
			}
		}
		
		logger.debug("block {}, {} affected users notified, {} added {} removed", new Object[] { block, affectedGuids.size(), addCount, removeCount } );
		
		// The stackReason is actually viewer dependent, but usually changes for
		// all users at once, so notifying globally is reasonably efficient
		if (reasonChanged)
			session.changed(BlockDMO.class,new BlockDMOKey(block), "stackReason");		
		
		BlockEvent event = new BlockEvent(block.getGuid(), block.getTimestampAsLong(), affectedGuids);
		LiveState.getInstance().queueUpdate(event);
		
		return previousParticipation;
	}
	
	private Pair<Long, StackReason> updateUserBlockDatas(final Block block, final Guid participantId, final StackReason reason) throws RetryException {
		final Set<User> desiredUsers = getHandler(block).getInterestedUsers(block);

		return TxUtils.runNeedsRetry(new TxCallable<Pair<Long, StackReason>>() {
			public Pair<Long, StackReason> call() throws RetryException {
				return updateUserBlockDatasInternal(block, desiredUsers, participantId, reason);
			}
		});
	}
	
	private void refreshUserBlockDatasDeleted(Block block, boolean verbose) {
		Set<User> desiredUsers = getHandler(block).getInterestedUsers(block);

		List<UserBlockData> userDatas = queryUserBlockDatas(block);
		
		Map<User,UserBlockData> existing = new HashMap<User,UserBlockData>();
		for (UserBlockData ubd : userDatas) {
			existing.put(ubd.getUser(), ubd);
		}
		
		for (User u : desiredUsers) {
			UserBlockData old = existing.get(u);
			if (old != null) {
				existing.remove(u);
				if (old.isDeleted()) {
					if (verbose)
						logger.debug("Undeleting user block data {}", old);
					old.setDeleted(false);
				}
			}
		}
		// the rest of "existing" is users who no longer are in the desired set
		for (User u : existing.keySet()) {
			UserBlockData old = existing.get(u);
			if (!old.isDeleted()) {
				if (verbose)
					logger.debug("Deleting user block data {}", old);
				old.setDeleted(true);
			}
		}
	}
	
	// note this query includes gbd.deleted=1
	private List<GroupBlockData> queryGroupBlockDatas(Block block) {
		Query q = em.createQuery("SELECT gbd FROM GroupBlockData gbd WHERE gbd.block = :block");
		q.setParameter("block", block);
		return TypeUtils.castList(GroupBlockData.class, q.getResultList());
	}
	
	// Don't call directly, RetryException is added in the wrapper for readability
	private void updateGroupBlockDatasInternal(Block block, Set<Group> desiredGroups, boolean isGroupParticipation, StackReason reason) {
		int addCount;
		int removeCount;
		
		Set<Guid> affectedGuids = new HashSet<Guid>();
		
		// be sure we have the right GroupBlockData. This would be a lot saner to do
		// at the point where it changes... e.g. when people add/remove friends, 
		// or join/leave groups, instead of the expensive query and fixup here.
		// But it would not retroactively fix the existing db or let us change our
		// rules for who gets what...
		
		List<GroupBlockData> groupDatas = queryGroupBlockDatas(block);
		
		Map<Group,GroupBlockData> existing = new HashMap<Group,GroupBlockData>();
		for (GroupBlockData gbd : groupDatas) {
			existing.put(gbd.getGroup(), gbd);
		}
		
		addCount = 0;
		removeCount = 0;
		
		for (Group g : desiredGroups) {
			affectedGuids.add(g.getGuid());
			
			GroupBlockData old = existing.get(g);
			if (old != null) {
				existing.remove(g);
				if (old.isDeleted())
					addCount += 1;
				old.setDeleted(false);
				old.setStackTimestamp(block.getTimestamp());
				old.setStackReason(reason);
				if (isGroupParticipation) {
					old.setParticipatedTimestamp(block.getTimestamp());
					old.setParticipatedReason(reason);
				}
			} else {
				GroupBlockData data = new GroupBlockData(g, block);
				data.setStackReason(reason);
				if (isGroupParticipation) {
					data.setParticipatedTimestamp(block.getTimestamp());
					data.setParticipatedReason(reason);
				}
				em.persist(data);
				addCount += 1;
			}
		}
		// the rest of "existing" is groups who no longer are in the desired set
		for (Group g : existing.keySet()) {
			GroupBlockData old = existing.get(g);
			if (!old.isDeleted())
				removeCount += 1;
			old.setDeleted(true);
		}
		
		logger.debug("block {}, {} total groups {} added {} removed {}", new Object[] { block, affectedGuids.size(), addCount, removeCount } );
	}		

	private void updateGroupBlockDatas(final Block block, final boolean isGroupParticipation, final StackReason reason) throws RetryException {
		final Set<Group> desiredGroups = getHandler(block).getInterestedGroups(block);
		
		TxUtils.runNeedsRetry(new TxRunnable() {
			public void run() throws RetryException {
				updateGroupBlockDatasInternal(block, desiredGroups, isGroupParticipation, reason);
			}
		});
	}
	
	private void refreshGroupBlockDatasDeleted(Block block, boolean verbose) {
		Set<Group> desiredGroups = getHandler(block).getInterestedGroups(block);

		List<GroupBlockData> groupDatas = queryGroupBlockDatas(block);
		
		Map<Group,GroupBlockData> existing = new HashMap<Group,GroupBlockData>();
		for (GroupBlockData gbd : groupDatas) {
			existing.put(gbd.getGroup(), gbd);
		}
		
		for (Group g : desiredGroups) {
			GroupBlockData old = existing.get(g);
			if (old != null) {
				existing.remove(g);
				if (old.isDeleted()) {
					if (verbose)
						logger.debug("Undeleting group block data {}", old);
					old.setDeleted(false);
				}
			}
		}
		// the rest of "existing" is groups who no longer are in the desired set
		for (Group g : existing.keySet()) {
			GroupBlockData old = existing.get(g);
			if (!old.isDeleted()) {
				if (verbose)
					logger.debug("Deleting group block data {}", old);
				old.setDeleted(true);
			}
		}
	}
	
	public void refreshDeletedFlags(BlockKey key) {
		Block block;
		try {
			block = queryBlock(key);
		} catch (NotFoundException e) {
			logger.debug("refreshDeletedFlags() called on block that doesn't exist which should be harmless, key={}", key);
			return;
		}
		refreshDeletedFlags(block);
	}
	
	public void refreshDeletedFlags(Block block) {
		refreshUserBlockDatasDeleted(block, false);
		refreshGroupBlockDatasDeleted(block, false);
	}

	public void refreshDeletedFlagsOnAllBlocks() {
		refreshDeletedFlagsOnAllBlocksWithType(null);
	}
	
	public void refreshDeletedFlagsOnAllBlocksWithType(String typeName) {
		final List<String> blockIds = new ArrayList<String>();
		
		BlockType blockType = null;
		if (typeName != null)
			blockType = BlockType.valueOf(typeName);
		
		Query q = em.createQuery("SELECT block.id FROM Block block " + 
				(blockType != null ? " WHERE blockType = " + blockType.ordinal() : ""));
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			blockIds.add(id);
		
		Thread t = ThreadUtils.newDaemonThread("refresh deleted", new DaemonRunnable() {
			public void run() {
				int i = 0;
				int failures = 0;
				for (final String id : blockIds) {
					if ((i % 1000) == 0) {
						logger.debug("processing block {} of {}", i, blockIds.size());
					}
					try {
						TxUtils.runInTransaction(new TxRunnable() {
							public void run() {							
								Block attached = em.find(Block.class, id);
								refreshUserBlockDatasDeleted(attached, true);
								refreshGroupBlockDatasDeleted(attached, true);							
							}
						});
					} catch (RuntimeException e) {
						logger.warn("Failed to refresh deleted flags, root exception", ExceptionUtils.getRootCause(e));
						logger.warn("Failed to refresh deleted flags, toplevel exception", e);
						++failures;
						
						if (failures > 50) {
							logger.warn("Failed too many times, stopping");
							break;
						}	
					}
					++i;
				}
				
				logger.debug("Completed refreshing deleted flags on all blocks, {} failures", failures);
			}
		});
		t.start();
	}
	
	public void stack(final Block block, final long activity, final User participant, final boolean isGroupParticipation, final StackReason reason, final boolean updateAllUserBlockDatas) {
		// never "roll back" to an earlier timestamp
		if (block.getTimestampAsLong() < activity) { 
			block.setTimestampAsLong(activity);
			DataService.currentSessionRW().changed(BlockDMO.class, new BlockDMOKey(block), "timestamp");
		}
		
		// Now we need to create demand-create user/group block data objects and update the
		// cached user timestamps. update{User,Group{BlockDatas(block) are always safe to call
		// at any point without worrying about ordering. We queue the update asynchronously
		// after commit, so we can do retries when demand-creating {User,Group}BlockData.
		TxUtils.runInTransactionOnCommit(new TxRunnable() {
			public void run() throws RetryException {
				DataService.getModel().initializeReadWriteSession(SystemViewpoint.getInstance());
				
				Pair<Long, StackReason> previousParticipation = new Pair<Long, StackReason>(Long.valueOf(-1), null);
				
				Block attached = em.find(Block.class, block.getId());
				if (updateAllUserBlockDatas) {
				    previousParticipation = updateUserBlockDatas(attached, (participant != null ? participant.getGuid() : null), reason);
			    } else if (participant != null) {
			        previousParticipation = updateParticipantUserBlockData(attached, participant.getGuid(), reason);	
			    }				
				updateGroupBlockDatas(attached, isGroupParticipation, reason);

				final Pair<Long, StackReason> previousParticipationFinal = previousParticipation;
				
				// FACEBOOK_EVENT blocks are never public, but we might as well check explicitly.
				// We'll need to change this logic if we want to display some private blocks to
				// a subset of person's friends who can also see those private blocks on Mugshot.
				if (participant != null && block.isPublicBlock() && block.getBlockType() != BlockType.FACEBOOK_EVENT) {
				    TxUtils.runOnCommit(new Runnable() {
					    public void run() {
					    	facebookTracker.updateFbmlForUser(participant);
					    	
					    	// We do not want to publish repeated user actions reflected by the same block, such as 
					    	// chatting or playing music. 
					    	// If we kept track of when we last published a story about the block, we would be able
					    	// to publish a story about a similar activity after a certain amount of time from a previous
					    	// story. However, we currently only keep track of when the user last participated in
					    	// a certain activity, as a result this code will publish a new story only after a user
					    	// did not participate in that activity for a certain amount of time. Such as, if the user
					    	// did not play music for more than 6 hours, but then started playing it again, or if the user
					    	// did not chat about something for 6 hours, but then had something new to say about it.
					    	// This is fine because it allows us to create notifications when new events happen.
					    	// The time interval can be different for different block types or stack reasons.
					    	// The downside of this logic is that if someone plays music every 5 hours or comments on
					    	// a block every 5 hours, we'll never create a new update about it after the first one.
					    	//
					    	// Exclude Twitter and MySpace blog updates from such logic, since even though we reuse the
					    	// same block for them, they should not happen too often and each one of them is interesting.
					    	// The exclusion only applies to BLOCK_UPDATE stack reason, and not others such as CHAT_MESSAGE. 
					    	// (Though we don't have chatting on Twitter blocks anyway.)
					    	if (((block.getBlockType() == BlockType.TWITTER_PERSON || block.getBlockType() == BlockType.MYSPACE_PERSON) && reason.equals(StackReason.BLOCK_UPDATE)) ||	
					    		previousParticipationFinal.getSecond() == null || 
					            !previousParticipationFinal.getSecond().equals(reason) ||
								previousParticipationFinal.getFirst() < (new Date().getTime()) - 6 * 60 * 60 * 1000) {
					    		
					    	    facebookTracker.publishUserAction(block, participant);	
					    		 
					    	}      
					    }
				    });
				}	 
			}
		});
	}
	
	public void stack(Block block, long activity, User participant, boolean isGroupParticipation, StackReason reason) {
	    stack(block, activity, participant, isGroupParticipation, reason, true);	
	}
	
	public void stack(Block block, long activity, StackReason reason) {
		stack(block, activity, null, false, reason, true);
	}
	
	public Block stack(BlockKey key, long activity, User participant, boolean isGroupParticipation, StackReason reason, boolean updateAllUserBlockDatas) {
		Block block;
		try {
			block = queryBlock(key);
		} catch (NotFoundException e) {
			throw new RuntimeException("stack() called on block that doesn't exist; probably means a migration is needed. key=" + key, e);
		}
        stack(block, activity, participant, isGroupParticipation, reason, updateAllUserBlockDatas);
        return block;
	}
	
	public Block stack(BlockKey key, long activity, User participant, boolean isGroupParticipation, StackReason reason) {
		return stack(key, activity, participant, isGroupParticipation, reason, true);		
	}
	
	public Block stack(BlockKey key, long activity, StackReason reason) {
		return stack(key, activity, null, false, reason, true);
	}
	
	public Block stackIfFound(BlockKey key, long activity, User participant, boolean isGroupParticipation, StackReason reason, boolean updateAllUserBlockDatas) throws NotFoundException {
		// queryBlock() will throw a NotFoundException if the block is not found
		Block block = queryBlock(key);
        stack(block, activity, participant, isGroupParticipation, reason, updateAllUserBlockDatas);
        return block;		
	}
	
	public void blockClicked(BlockKey key, User user, long clickedTime) {
		UserBlockData ubd;
		try {
			ubd = queryUserBlockData(user, key);
		} catch (NotFoundException e) {
			// for now assume this means we don't want to record clicks for the given
			// object, otherwise the ubd should already be created
			logger.debug("No UserBlockData for user {} block key {}", user, key);
			return;
		}
		
		blockClicked(ubd, clickedTime);
	}
		
	private void invalidateUserBlockDataProperty(UserBlockData ubd, String propertyName) {
		DataService.currentSessionRW().changed(BlockDMO.class,
				   new BlockDMOKey(ubd.getBlock()),
				   propertyName,
				   new UserClientMatcher(ubd.getUser().getGuid()));
	}
	
	public void blockClicked(UserBlockData ubd, long clickedTime) {
		Block block = ubd.getBlock();
		
		// if we weren't previously clicked on, then increment the count.
		// (FIXME this is not a reliable way of incrementing a count, since two transactions
		// can read the same value and write the same value + 1)
		if (!ubd.isClicked())
			block.setClickedCount(block.getClickedCount() + 1);
		
		if (ubd.getClickedTimestampAsLong() < clickedTime) {
			ubd.setClickedTimestampAsLong(clickedTime);
			invalidateUserBlockDataProperty(ubd, "clickedTimestamp");
		}
		
		// we automatically unignore anything you click on
		if (ubd.isIgnored())
			setBlockHushed(ubd, false);
		
		// We call blockClicked for ACCOUNT_QUESTION blocks as well, but 
		// the idea of a "significant clicked count" is specific to post blocks
		
		if (block.getBlockType() != BlockType.POST)
			return;

		if (!BlockView.clickedCountIsSignificant(block.getClickedCount()))
			return;
		
		logger.debug("due to click, restacking block {} with new time {}",
				block, clickedTime);
		
		DataService.currentSessionRW().changed(BlockDMO.class, new BlockDMOKey(block), "significantClickedCount");
		
		// now update the timestamp in the block (if it's newer)
		// and update user caches for all users
		stack(block, clickedTime, StackReason.VIEWER_COUNT);
	}

	// FIXME this function should die since block-type-specific code should not 
	// be in this file
	private BlockKey getMusicPersonKey(Guid userId) {
		return getHandler(MusicPersonBlockHandler.class, BlockType.MUSIC_PERSON).getKey(userId);
	}
	
	// FIXME this function should die since block-type-specific code should not 
	// be in this file
	private BlockKey getGroupChatKey(Guid groupId) {
		return getHandler(GroupChatBlockHandler.class, BlockType.GROUP_CHAT).getKey(groupId);
	}
	
	// FIXME this function should die since block-type-specific code should not 
	// be in this file
	private BlockKey getPostKey(Post post) {
		return getHandler(PostBlockHandler.class, BlockType.POST).getKey(post);
	}
	
	// FIXME this function should die since block-type-specific code should not 
	// be in this file
	private BlockKey getGroupMemberKey(Guid groupId, Guid userId) {
		return getHandler(GroupMemberBlockHandler.class, BlockType.GROUP_MEMBER).getKey(groupId, userId);
	}

	// Preparing a block view creates a skeletal BlockView that has the Block and the 
	// UserBlockData and nothing more. The idea is that when paging a stack of blocks,
	// we want to scan through the pages before the one we are viewing as fast as
	// possible, and then only fill in the full details for the items we are actually
	// viewing.
	//
	// What we do check at this point is access control: this method throws NotFoundException 
	//  if the user can't see the contents of the Block.
	//
	private BlockView prepareBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) throws BlockNotVisibleException {
		return getHandler(block).getUnpopulatedBlockView(viewpoint, block, ubd, participated);
	} 
	
	private BlockView prepareBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) throws BlockNotVisibleException {
		return getHandler(block).getUnpopulatedBlockView(viewpoint, block, gbd, participated);
	} 

	// Populating the block view fills in all the details that were skipped at
	//   the prepare stage and makes it ready for viewing by the user.
	private void populateBlockView(BlockView blockView) {
		getHandler(blockView).populateBlockView(blockView);
	}
	
	private BlockView getBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd, boolean participated) throws NotFoundException {
		try {
			BlockView blockView = prepareBlockView(viewpoint, block, ubd, participated);
			populateBlockView(blockView);
			return blockView;
		} catch (BlockNotVisibleException e) {
			throw new NotFoundException("Can't see this block", e);
		}
	}
	
	private BlockView getBlockView(Viewpoint viewpoint, Block block, GroupBlockData gbd, boolean participated) throws NotFoundException {
		try {
			BlockView blockView = prepareBlockView(viewpoint, block, gbd, participated);
			populateBlockView(blockView);
			return blockView;
		} catch (BlockNotVisibleException e) {
			throw new NotFoundException("Can't see this block", e);
		}
	}
	
	public BlockView loadBlock(Viewpoint viewpoint, Block block) throws NotFoundException {
		UserBlockData ubd = null;
		
		if (viewpoint instanceof UserViewpoint) {
			try {
				ubd = lookupUserBlockData((UserViewpoint)viewpoint, block.getGuid());
			} catch (NotFoundException e) {
			}
		}
		
		return getBlockView(viewpoint, block, ubd, false);
	}
	
	public BlockView loadBlock(Viewpoint viewpoint, UserBlockData ubd) throws NotFoundException {	
		return getBlockView(viewpoint, ubd.getBlock(), ubd, false);
	}	
	
	public BlockView loadBlock(Viewpoint viewpoint, BlockKey key) throws NotFoundException {
		Block block = null;
		UserBlockData ubd = null;
		
		if (viewpoint instanceof UserViewpoint) {
			try {
				ubd = lookupUserBlockData((UserViewpoint)viewpoint, key);
				block = ubd.getBlock();
			} catch (NotFoundException e) {
			}
		}
		
		if (block == null)
			block = queryBlock(key);
		
		return getBlockView(viewpoint, block, ubd, false);
	}	
	
	private static class StackFilterExpression {
		private boolean noFeed = false;
		private boolean noSelfSource = false;
		
		public StackFilterExpression(String expr) {
			if (expr != null) {
				String[] components = expr.split(",");
				for (int i = 0; i < components.length; i++) {
					if (components[i].equals("nofeed"))
						noFeed = true;
					else if (components[i].equals("noselfsource"))
						noSelfSource = true;
				}
			}
		}
		
		public boolean isNoFeed() {
			return noFeed;
		}
		
		public boolean isNoSelfSource() {
			return noSelfSource;
		}
	}
	
	/**
	 * 
	 * @param viewpoint
	 * @param user
	 * @param lastTimestamp -1 to ignore this param, otherwise only return blocks newer than this
	 * @param stackedBefore -1 to ignore this param, otherwise only return blocks older than this
	 * @param start
	 * @param count
	 * @param participantOnly if true, only include blocks where someone participated, and sort by participation time 
	 * @param participantOnly2 
	 * @param filter 
	 * @param noSelfSource 
	 * @return 
	 */
	private List<UserBlockData> getBlocks(Viewpoint viewpoint, User user, 
									      long lastTimestamp, long stackedBefore,
										  int start, int count, 
			                              String filter, boolean participantOnly) {
		StackFilterExpression parsedExpression = new StackFilterExpression(filter);
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT ubd FROM UserBlockData ubd, Block block " + 
                	 " WHERE ubd.user = :user AND ubd.deleted = 0 AND ubd.block = block ");
		
		if (participantOnly)
			sb.append(" AND ubd.participatedTimestamp IS NOT NULL ");		

		/* Timestamp clause */
		
		// if lastTimestamp == 0 then all blocks are included so just skip the test in the sql
		// The timestamp here is the timestamp of the block last seen by the client. 
		// Using > here rather than >= means that if two blocks are stacked within the
		// same millisecond and notified on separately, then the second block will get
		// lost, but that's better than always sending two blocks.
		if (lastTimestamp > 0)
			sb.append(" AND ubd.stackTimestamp > :timestamp ");
		
		// if lastTimestamp == 0 then all blocks are included so just skip the test in the sql
		// The timestamp here is the timestamp of the block last seen by the client. 
		// Using > here rather than >= means that if two blocks are stacked within the
		// same millisecond and notified on separately, then the second block will get
		// lost, but that's better than always sending two blocks.
		if (stackedBefore > 0)
			sb.append(" AND ubd.stackTimestamp < :stackedBefore ");

		if (parsedExpression.isNoFeed())
			sb.append(" AND block.filterFlags != " + StackFilterFlags.FEED.getValue());
		
		if (parsedExpression.isNoSelfSource() && viewpoint instanceof UserViewpoint) {
			
			sb.append(" AND NOT (");
			String data1Clause = getDirectData1UserBlockTypeClause();
			if (data1Clause != null) {
				sb.append("(block.data1 = :viewpointGuid  AND " + data1Clause + ")");
			}
			String data2Clause = getDirectData2UserBlockTypeClause();
			if (data2Clause != null) {
				if (data1Clause != null)
					sb.append(" OR ");
				sb.append("(block.data2 = :viewpointGuid AND " + data2Clause + ")");
			}
			sb.append(")");
		}

		// It's not absolutely clear what SystemViewpoint should return, since the system
		// is neither "the user themself" or "someone else". We don't currently use
		// StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS and 
		// StackInclusion.ONLY_WHEN_VIEWED_BY_SELF is just an optimization of the visibility
		// rules, so we make SystemViewpoint return everything. If we used
		// StackInclusion.ONLY_WHEN_VIEWED_BY_SELF, then this could cause duplicate blocks
		//
		if (!(viewpoint instanceof SystemViewpoint)) {
			/* Inclusion clause */
			sb.append(" AND (block.inclusion = ");
			sb.append(StackInclusion.IN_ALL_STACKS.ordinal());
			
			if (viewpoint instanceof UserViewpoint) {
				sb.append(" OR (block.data1 = :viewpointGuid AND block.inclusion = " + StackInclusion.ONLY_WHEN_VIEWING_SELF.ordinal() + ")");
				sb.append(" OR (block.data1 != :viewpointGuid AND block.inclusion = " + StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS.ordinal() + ")");
			} else {
				sb.append(" OR block.inclusion = " + StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS.ordinal());
			}
			sb.append(")");
		}
		
		/* Ordering clause */
		
		// FIXME this is not exactly the sort order if the user is paging; we want to use ubd.ignoredDate in the sort if the 
		// user has ignored a block, instead of block.timestamp. However, EJBQL doesn't know how to do that.
		// maybe a native sql query or some other solution is required. For now what we'll do is 
		// return blocks in block order, and also pass to the client the ignoredDate.
		// Then, require the client to sort it out. This may well be right anyway.
		
		sb.append(" ORDER BY ");
		if (participantOnly)
			sb.append("ubd.participatedTimestamp");
		else
			sb.append("ubd.stackTimestamp");
		sb.append(" DESC");
		
		Query q = em.createQuery(sb.toString()); 

		q.setFirstResult(start);
		q.setMaxResults(count);
		if (lastTimestamp > 0)
			q.setParameter("timestamp", new Date(lastTimestamp));
		if (stackedBefore > 0)
			q.setParameter("stackedBefore", new Date(stackedBefore));
		q.setParameter("user", user);
		if (viewpoint instanceof UserViewpoint)
			q.setParameter("viewpointGuid", ((UserViewpoint) viewpoint).getViewer().getGuid().toString());
		
		return TypeUtils.castList(UserBlockData.class, q.getResultList());		
	}

	// Note the off-by-one difference betweeb minTimestamp and lastTimestamp
	public List<UserBlockData> getStackBlocks(User user, int start, int count, long minTimestamp) {
		return getBlocks(SystemViewpoint.getInstance(), user, minTimestamp - 1, -1, start, count, null, false);
	}
	
	private interface BlockSource<T> {
		List<Pair<Block, T>> get(int start, int count);
		BlockView prepareView(Viewpoint viewpoint, Block block, T t) throws BlockNotVisibleException;
	}

	private <T> void pageStack(Viewpoint viewpoint, BlockSource<T> source, Pageable<BlockView> pageable, int expectedHitFactor) {
		
		// + 1 is for finding out if there are items for the next page
		int targetedNumberOfItems = pageable.getStart() + pageable.getCount() + 1;
		int firstItemToReturn = pageable.getStart();
		
		List<BlockView> stack = new ArrayList<BlockView>();
		int start = 0;
		
		while (stack.size() < targetedNumberOfItems) {
			int count = (targetedNumberOfItems - stack.size()) * expectedHitFactor;
			List<Pair<Block, T>> blocks = source.get(start, count);
			if (blocks.isEmpty())
				break;
			
			int resultItemCount = 0;
			// Create BlockView objects for the blocks, performing access control checks
			for (Pair<Block, T> pair : blocks) {			
				Block block = pair.getFirst();
				T t = pair.getSecond();
				
				try {
					stack.add(source.prepareView(viewpoint, block, t));
					resultItemCount++;
					if (stack.size() >= targetedNumberOfItems)
						break;
				} catch (BlockNotVisibleException e) {
					// Do nothing, we can't see this block
				}
			}		
		    
		    // nothing else there
		    if (blocks.size() < count)
		    	break;
	
		    start = start + count;
		    if (resultItemCount > 0)
		    	expectedHitFactor = blocks.size() / resultItemCount + 1;
		}
		
		List<BlockView> blockViewsToReturn;				
		if (stack.size() < targetedNumberOfItems) {
			// this will readjust the position, so pageable.getStart() will be readjusted too
			pageable.setTotalCount(stack.size());
			blockViewsToReturn = stack.subList(pageable.getStart(), stack.size());	
		} else {
			// we are not including the last item in the stack list in the results, it's
			// just an indicator that there are more items
			blockViewsToReturn = stack.subList(firstItemToReturn, stack.size() - 1);
            pageable.setTotalCount((pageable.getInitialPerPage() + pageable.getSubsequentPerPage() * 9) 
						           * ((pageable.getPosition() + 1) / 10 + 1));
		}

		// now populate all the block views
		for (BlockView blockView : blockViewsToReturn) {
			populateBlockView(blockView);
		}

		pageable.setResults(blockViewsToReturn);
	}

	public void pageStack(Viewpoint viewpoint, User user, Pageable<BlockView> pageable, boolean participantOnly) {
		pageStack(viewpoint, user, pageable, -1, participantOnly);
	}
	
	public void pageStack(final Viewpoint viewpoint, final User user, Pageable<BlockView> pageable, final long lastTimestamp, final boolean participantOnly) {
		pageStack(viewpoint, user, pageable, lastTimestamp, null, participantOnly);
	}
	
	public void pageStack(final Viewpoint viewpoint, final User user, Pageable<BlockView> pageable, final long lastTimestamp, 
			              final String filter, final boolean participantOnly) {
		
		logger.debug("getting stack for user {}", user);

       	int expectedHitFactor = 4;
		if (viewpoint.isOfUser(user))
			expectedHitFactor = 2;
		
		pageStack(viewpoint, new BlockSource<UserBlockData>() {
			public List<Pair<Block, UserBlockData>> get(int start, int count) {
				List<Pair<Block, UserBlockData>> results = new ArrayList<Pair<Block, UserBlockData>>();
				for (UserBlockData ubd : getBlocks(viewpoint, user, lastTimestamp, -1, start, count, filter, participantOnly)) {                 
					results.add(new Pair<Block, UserBlockData>(ubd.getBlock(), ubd));
				}
				return results;
			}
			public BlockView prepareView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws BlockNotVisibleException {
				BlockView blockView = prepareBlockView(viewpoint, block, ubd, participantOnly);
		        if (block.getBlockType() == BlockType.ACCOUNT_QUESTION) { 
		        	// ACCOUNT_QUESTION blocks should be populated anyway, since they are not public, and we
		        	// populate all blocks that are not public when preparing the blockView, but let's do
		        	// it here just in case
		        	// We could have also used clickedTimestamp to check if the question has been answered, because
		        	// we assign this meaning to it for this block type (see AccountQuestionBlockHandlerBean::questionAnswered()),
		        	// but since we should have the block populated anyway, we might as well check the answer directly.
		        	getHandler(block).populateBlockView(blockView);
		        	if (((AccountQuestionBlockView)blockView).getAnswer() == null) {
		        		throw new BlockNotVisibleException("We don't include unanswered account questions in the regular stack.");
		        	}
		        }
		        return blockView;
			}
		}, pageable, expectedHitFactor);
		
		if (pageable.getPosition() == 0 && !participantOnly && viewpoint.isOfUser(user))
		    pageable.getResults().addAll(0, getUnansweredQuestions((UserViewpoint)viewpoint, System.currentTimeMillis()));
	}	
	
	private static abstract class ItemSource<T> {
		abstract Collection<T> get(int start, int count);
		Collection<T> getRemainder() { return Collections.emptyList(); }
	}
	
	private <T> List<T> getDistinctItems(ItemSource<T> source, int start, int count, int expectedHitFactor) {
		Set<T> distinctItems = new HashSet<T>();
		List<T> returnItems = new ArrayList<T>();
		int max = start + count;

		int chunkStart = 0;
		boolean remainderUsed = false;
		while (distinctItems.size() < max) {
			int chunkCount = (max - distinctItems.size()) * expectedHitFactor;
			Collection<T> items = source.get(chunkStart, chunkCount);
			if (items.isEmpty() && !remainderUsed) {
				items = source.getRemainder();
				remainderUsed = true;
			} else if (items.isEmpty()) {
				break;
			}
			
			int resultItemCount = 0;

			for (T t : items) {
				if (distinctItems.contains(t))
					continue;
				distinctItems.add(t);
				if (distinctItems.size() > start)
					returnItems.add(t);
				resultItemCount++;
				if (returnItems.size() >= count)
					break;
			}		
		    
		    // nothing else there
		    if (items.size() < chunkCount && remainderUsed)
		    	break;
	
		    chunkStart = chunkStart + chunkCount;
		    if (resultItemCount > 0)
		    	expectedHitFactor = items.size() / resultItemCount + 1;
		}

		return returnItems;
	}
	
	private List<User> getRecentActivityUsers(int start, int count, boolean includeGroupUpdates) {
		// Use a native query so we can pass the MySQL STRAIGHT_JOIN hint; if we don't MySQL
		// creatively reorders things and creates a 5 minute query.
		
		String blockTypeClause;
		if (!includeGroupUpdates)
			blockTypeClause = 
				"      AND b.blockType <> " + BlockType.GROUP_MEMBER.ordinal() +
				"      AND b.blockType <> " + BlockType.GROUP_CHAT.ordinal();
		else
			blockTypeClause = "";
			
		Query q = em.createNativeQuery(
				"SELECT STRAIGHT_JOIN u.*, p.nickname " +
				"     FROM UserBlockData ubd " +
				"     JOIN HippoUser u on ubd.user_id = u.id " +
				"     JOIN Person p on ubd.user_id = p.id " +
				"     JOIN Block b on ubd.block_id = b.id " +
				"    WHERE ubd.deleted = 0 AND ubd.participatedTimestamp IS NOT NULL" +
				"      AND b.publicBlock = 1 " +
				blockTypeClause +
				" ORDER BY ubd.participatedTimestamp DESC", User.class);
		q.setFirstResult(start);
		q.setMaxResults(count);
		
		return TypeUtils.castList(User.class, q.getResultList());
	}
	
	private List<PersonMugshotView> getUserMugshotViews(Viewpoint viewpoint, List<User> users, int blockPerUser, boolean includeGroupUpdates) {
		final String groupUpdatesFilter;
		if (!includeGroupUpdates) {
	        groupUpdatesFilter = " AND block.blockType <> " + BlockType.GROUP_MEMBER.ordinal() + 
	                             " AND block.blockType <> " + BlockType.GROUP_CHAT.ordinal(); 
		} else {
			groupUpdatesFilter = "";
		}
		
		List<PersonMugshotView> mugshots = new ArrayList<PersonMugshotView>();		
		for (User user : users) {
			Query qu = em.createQuery("Select ubd FROM UserBlockData ubd, Block block " + 
                " WHERE ubd.user = :user AND ubd.deleted = 0 AND ubd.block = block " +
                " AND ubd.participatedTimestamp IS NOT NULL " +
                " AND block.publicBlock = true " + groupUpdatesFilter +
                " ORDER BY ubd.participatedTimestamp DESC");
			qu.setParameter("user", user);
		    qu.setMaxResults(blockPerUser);
         	List<BlockView> blocks = new ArrayList<BlockView>();
         	for (UserBlockData ubd : TypeUtils.castList(UserBlockData.class, qu.getResultList())) {
	         	try {
	         	    BlockView blockView = getBlockView(viewpoint, ubd.getBlock(), ubd, true);
	         	    blocks.add(blockView);
	         	} catch (NotFoundException e) {
	         		// this is used on the main page, let's not risk it throwing an exception here
	         		logger.error("NotFoundException when getting what must be a public block", e);
	         	}
         	}
         	PersonView personView = personViewer.getPersonView(viewpoint, user, 
         													   PersonViewExtra.EXTERNAL_ACCOUNTS,
         													   PersonViewExtra.CONTACT_STATUS);
            mugshots.add(new PersonMugshotView(personView, blocks));
		}
        return mugshots; 
	}
	
	public List<PersonMugshotView> getRecentUserActivity(Viewpoint viewpoint, int startUser, int userCount, int blockPerUser, final boolean includeGroupUpdates) {
		
		// select distinct most recently active users		
       	int expectedHitFactor = 2;
		
		List<User> distinctUsers = getDistinctItems(new ItemSource<User>() {
			@Override
			public Collection<User> get(int start, int count) {
				return getRecentActivityUsers(start, count, includeGroupUpdates);
			}
		}, startUser, userCount, expectedHitFactor);
		
		return getUserMugshotViews(viewpoint, distinctUsers, blockPerUser, includeGroupUpdates);		
	}
	
	public void pageRecentUserActivity(Viewpoint viewpoint, Pageable<PersonMugshotView> pageable, int blocksPerUser) {
		pageable.setResults(getRecentUserActivity(viewpoint, pageable.getStart(), pageable.getCount(), blocksPerUser, true));
		
		// Doing an exact count is expensive, our assumption is "lots and lots"
		pageable.setTotalCount(pageable.getBound());		
	}
	
	public User getRandomActiveUser(Viewpoint viewpoint) {
		List<User> list = getRecentActivityUsers(0, 1, true);
		if (!list.isEmpty())
			return list.get(0);
		else {
			// just pick any user that exists if nobody was active - should never happen on production
			logger.warn("No active users, picking someone at random");
			Query q = em.createQuery("SELECT user FROM User user");
			q.setMaxResults(1);
			return (User) q.getSingleResult();
		}
	}
	
	private List<GroupBlockData> getBlocks(Viewpoint viewpoint, Group group, int start, int count, boolean byParticipation) {
		if (!groupSystem.canSeeContent(viewpoint, group))
			return Collections.emptyList();
		
		String orderBy;
		if (byParticipation)
			orderBy = " AND gbd.participatedTimestamp IS NOT NULL ORDER BY gbd.participatedTimestamp DESC";
		else
			orderBy = " ORDER BY gbd.stackTimestamp DESC";
		
		Query q = em.createQuery("SELECT gbd FROM GroupBlockData gbd " + 
				                 " WHERE gbd.group = :group AND gbd.deleted = 0 AND gbd.block = block " + 
				                 orderBy);
		q.setFirstResult(start);
		q.setMaxResults(count);
		q.setParameter("group", group);
		
		return TypeUtils.castList(GroupBlockData.class, q.getResultList());
	}
	
	public void pageStack(final Viewpoint viewpoint, final Group group, Pageable<BlockView> pageable, final boolean byParticipation) {
		
		logger.debug("getting stack for group {}", group);

		// There may be a few exceptions, but generally if you can see a group page at all
		// you should be able to see all the blocks for the group
       	int expectedHitFactor = 2;
       	
		pageStack(viewpoint, new BlockSource<GroupBlockData>() {
			public List<Pair<Block, GroupBlockData>> get(int start, int count) {
				List<Pair<Block, GroupBlockData>> results = new ArrayList<Pair<Block, GroupBlockData>>();
				for (GroupBlockData gbd : getBlocks(viewpoint, group, start, count, byParticipation)) {
					results.add(new Pair<Block, GroupBlockData>(gbd.getBlock(), gbd));
				}
				return results;
			}
			public BlockView prepareView(Viewpoint viewpoint, Block block, GroupBlockData gbd) throws BlockNotVisibleException {
				return prepareBlockView(viewpoint, block, gbd, byParticipation);
			}
		}, pageable, expectedHitFactor);
	}
	
	private static Set<BlockType> directData1UserBlockTypes;
	private static Set<BlockType> directData2UserBlockTypes;
	private static Set<BlockType> indirectData1UserBlockTypes;
	private static Set<BlockType> indirectData2UserBlockTypes;

	private static String joinBlockTypeOrdinals(Set<BlockType> types) {
		StringBuilder builder = new StringBuilder();
		Iterator<BlockType> it = types.iterator();		
		while (it.hasNext()) {
			builder.append(Integer.toString(it.next().ordinal()));
			if (it.hasNext()) {
				builder.append(", ");
			}
		}		
		return builder.toString();
	}
	
	private static void initBlockTypeCache() {
		synchronized (StackerBean.class) {
			if (directData1UserBlockTypes == null) {
				directData1UserBlockTypes = new HashSet<BlockType>();
				directData2UserBlockTypes = new HashSet<BlockType>();
				indirectData1UserBlockTypes = new HashSet<BlockType>();
				indirectData2UserBlockTypes = new HashSet<BlockType>();				
				Iterator<BlockType> it = Arrays.asList(BlockType.values()).iterator();
				while (it.hasNext()) {
					BlockType blockType = it.next();
					if (blockType.getBlockOwnership() == BlockType.BlockOwnership.DIRECT_DATA1)
						directData1UserBlockTypes.add(blockType);
					else if (blockType.getBlockOwnership() == BlockType.BlockOwnership.DIRECT_DATA2)
						directData2UserBlockTypes.add(blockType);
					else if (blockType.getBlockOwnership() == BlockType.BlockOwnership.INDIRECT_DATA1)
						indirectData1UserBlockTypes.add(blockType);
					else if (blockType.getBlockOwnership() == BlockType.BlockOwnership.INDIRECT_DATA2)
						indirectData2UserBlockTypes.add(blockType);
				}
			}
		}	
	}
	
	private String getData1UserBlockTypeClause() {
		initBlockTypeCache();
		StringBuilder builder = new StringBuilder(" block.blockType IN (");
		Set<BlockType> allData1 = new HashSet<BlockType>();
		allData1.addAll(directData1UserBlockTypes);
		allData1.addAll(indirectData1UserBlockTypes);
		builder.append(joinBlockTypeOrdinals(allData1));
		builder.append(") ");
		return builder.toString();
	}
	
	private String getDirectData1UserBlockTypeClause() {
		initBlockTypeCache();
		if (directData1UserBlockTypes.size() > 0) {		
			StringBuilder builder = new StringBuilder(" block.blockType IN (");
			builder.append(joinBlockTypeOrdinals(directData1UserBlockTypes));
			builder.append(") ");
			return builder.toString();
		} else {
			return null;
		}
	}
	
	private String getDirectData2UserBlockTypeClause() {
		initBlockTypeCache();
		if (directData2UserBlockTypes.size() > 0) {
			StringBuilder builder = new StringBuilder(" block.blockType IN (");
			builder.append(joinBlockTypeOrdinals(directData2UserBlockTypes));
			builder.append(") ");
			return builder.toString();
		} else {
			return null;
		}
	}		
	
	private List<User> getRecentlyActiveContacts(Viewpoint viewpoint, User user, int start, int count) {
		// The algorithm to find recently active contacts here is simply to select blocks from the user's
		// stack which originated from a user, because currently the only user-originated blocks in
		// a user's stack will be their contacts.  This is an approximation of
		// the ideal which would be the participated blocks for all the user's contacts, but that query
		// would be significantly slower.  This one will miss e.g. group chat and posts at the moment.
		// When we extend this to support posts we need to be sure to filter by the user's actual contacts
		// since the first assumption will no longer be true.  We exclude data2 because posts may be
		// sent to groups by someone who is not the user's contact.
		Query q = em.createQuery("SELECT u FROM User u, UserBlockData ubd, Block block" +
                " WHERE ubd.deleted = 0 AND ubd.block = block " +
                " AND block.data1 = u.id AND " + getData1UserBlockTypeClause() +
                " AND block.publicBlock = true " +
                " AND u != :user " +
                " AND ubd.user = :user " +
                " ORDER BY ubd.stackTimestamp DESC");
		q.setParameter("user", user);
		q.setFirstResult(start);
		q.setMaxResults(count);
		return TypeUtils.castList(User.class, q.getResultList());
	}
	
	public List<PersonMugshotView> getContactActivity(final Viewpoint viewpoint, final User user, int start, int count, int blocksPerUser) {
		List<User> distinctUsers = getDistinctItems(new ItemSource<User>() {
			@Override
			public List<User> get(int start, int count) {
				return getRecentlyActiveContacts(viewpoint, user, start, count);
			}

			@Override
			Collection<User> getRemainder() {
				return identitySpider.getRawUserContacts(viewpoint, user);
			}
		}, start, count, 4);		
		
		return getUserMugshotViews(viewpoint, distinctUsers, blocksPerUser, true);
	}

	public void pageContactActivity(Viewpoint viewpoint, User viewedUser, int blocksPerUser, Pageable<PersonMugshotView> pageable) {
		pageable.setResults(getContactActivity(viewpoint, viewedUser, pageable.getStart(), pageable.getCount(), blocksPerUser));
		pageable.setTotalCount(LiveState.getInstance().getLiveUser(viewedUser.getGuid()).getUserContactsCount());
	}
	
	public List<GroupMugshotView> getGroupMugshotViews(final Viewpoint viewpoint, List<Group> distinctGroups, final int blocksPerGroup, final boolean publicOnly) {
		List<GroupMugshotView> mugshots = new ArrayList<GroupMugshotView>();		
		for (final Group group : distinctGroups) {
			// Lazy initialization
			mugshots.add(new GroupMugshotView() {
				@Override
				public List<BlockView> getBlocks() {
					if (blocks == null) {
						Query q = em.createQuery("Select gbd FROM GroupBlockData gbd, Block block " + 
				                " WHERE gbd.group = :group AND gbd.deleted = 0 AND gbd.block = block " +
				                (publicOnly ? INTERESTING_PUBLIC_GROUP_BLOCK_CLAUSE : INTERESTING_PUBLIC_OR_PRIVATE_GROUP_BLOCK_CLAUSE) + 
				                " ORDER BY gbd.participatedTimestamp DESC");
							q.setParameter("group", group);
						    q.setMaxResults(blocksPerGroup);
				         	blocks = new ArrayList<BlockView>();
				         	for (GroupBlockData gbd : TypeUtils.castList(GroupBlockData.class, q.getResultList())) {
					         	try {
					         	    BlockView blockView = getBlockView(viewpoint, gbd.getBlock(), gbd, true);
					         	    blocks.add(blockView);
					         	} catch (NotFoundException e) {
					         		// this is used on the main page, let's not risk it throwing an exception here
					         		logger.error("NotFoundException when getting what must be a public block", e);
					         	}
				         	}	
					}
					return blocks;
				}
				@Override
				public GroupView getGroupView() {
					if (groupView == null)
						groupView = groupSystem.getGroupView(viewpoint, group);
					return groupView;
				}				
			});   	
		}
		return mugshots;
	}
	
	private MembershipStatus groupQueryTypeToMembershipStatus(GroupQueryType groupType) {
		if (groupType.equals(GroupQueryType.ACTIVE))
			return MembershipStatus.ACTIVE;
		else if (groupType.equals(GroupQueryType.FOLLOWED))
			return MembershipStatus.FOLLOWER;
		else
			throw new RuntimeException("Unknown group query type: " + groupType);
	}
	
	private List<Group> getRecentlyActiveGroups(Viewpoint viewpoint, User user, int start, int count, GroupQueryType groupType) {
		// This query combines searching for participated blocks with group membership. 
		// It is not optimal right now as MySQL tells us it requires a temporary table and file sort.
		// Currently viewers other than the user can only see public groups
		Query q = em.createQuery("SELECT g FROM Group g, GroupBlockData gbd, Block block, GroupMember gm WHERE" +
				" gbd.group = g AND gm.group = g AND gbd.block = block " + 
				" AND gbd.participatedTimestamp IS NOT NULL " +
				(viewpoint.isOfUser(user) ? "" : (" and g.access >= " + GroupAccess.PUBLIC_INVITE.ordinal())) +
				" AND gm.status = " + groupQueryTypeToMembershipStatus(groupType).ordinal() +    
				" AND gm.member = :acct ORDER BY gbd.participatedTimestamp DESC");
		q.setParameter("acct", user.getAccount());
		q.setFirstResult(start);
		q.setMaxResults(count);
		List<Group> results = TypeUtils.castList(Group.class, q.getResultList());
		return results;
	}	
	
	public List<GroupMugshotView> getUserGroupActivity(final Viewpoint viewpoint, final User user, int start, int count, int blocksPerGroup, final GroupQueryType groupType) {
		// select distinct most recently active groups		
       	int expectedHitFactor = 2;
       	
		List<Group> distinctGroups = getDistinctItems(new ItemSource<Group>() {
			@Override
			public Collection<Group> get(int start, int count) {
				return getRecentlyActiveGroups(viewpoint, user, start, count, groupType);
			}

			@Override
			Collection<Group> getRemainder() {
				// This matches the query above
				return groupSystem.findRawGroups(viewpoint, user, groupQueryTypeToMembershipStatus(groupType));
			}
		}, start, count, expectedHitFactor);
		
		return getGroupMugshotViews(viewpoint, distinctGroups, blocksPerGroup, false);			
	}
	
	public void pageUserGroupActivity(Viewpoint viewpoint, User user, int blocksPerGroup, GroupQueryType groupType, Pageable<GroupMugshotView> pageable) {
		pageable.setResults(getUserGroupActivity(viewpoint, user, pageable.getStart(), pageable.getCount(), blocksPerGroup, groupType));
		pageable.setTotalCount(groupSystem.findGroupsCount(viewpoint, user, groupQueryTypeToMembershipStatus(groupType)));
	}

	// When showing recently active groups, we want to exclude activity for
	// users in the group, because we don't want a user playing music to
	// make an old inactive group that they happen to be a member of seem active
	static final String INTERESTING_PUBLIC_GROUP_BLOCK_CLAUSE =  
        " AND block.publicBlock = true " +
        " AND gbd.participatedTimestamp IS NOT NULL ";
	
	static final String INTERESTING_PUBLIC_OR_PRIVATE_GROUP_BLOCK_CLAUSE = 
		" AND gbd.participatedTimestamp IS NOT NULL ";
	
	private List<Group> getRecentActivityGroups(int start, int count) {
		// We get only public blocks, since we are displaying a system-global list
		// of groups. Note that we *also* have to make sure that the groups we 
		// retrieve are public because there are some cases where a public block
		// can be in the GroupBlockData for a private group; an example is
		// a post shared with both a public group and a private group.
		// 
		Query q = em.createQuery("SELECT gbd.group FROM GroupBlockData gbd, Block block " + 
                " WHERE gbd.deleted = 0 AND gbd.block = block " +
                " AND gbd.group.access >= " + GroupAccess.PUBLIC_INVITE.ordinal() +
                INTERESTING_PUBLIC_GROUP_BLOCK_CLAUSE +
                " ORDER BY gbd.participatedTimestamp DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		
		return TypeUtils.castList(Group.class, q.getResultList());
	}
	
	public List<GroupMugshotView> getRecentGroupActivity(Viewpoint viewpoint, int startGroup, int groupCount, int blockPerGroup) {		
		// select distinct most recently active users		
       	int expectedHitFactor = 2;
		
		List<Group> distinctGroups = getDistinctItems(new ItemSource<Group>() {
			@Override
			public Collection<Group> get(int start, int count) {
				return getRecentActivityGroups(start, count);
			}
		}, startGroup, groupCount, expectedHitFactor);
		
		return getGroupMugshotViews(viewpoint, distinctGroups, blockPerGroup, true);	
	}

	public void pageRecentGroupActivity(Viewpoint viewpoint, Pageable<GroupMugshotView> pageable, int blocksPerGroup) {
		pageable.setResults(getRecentGroupActivity(viewpoint, pageable.getStart(), pageable.getCount(), blocksPerGroup));
		pageable.setTotalCount(groupSystem.getPublicGroupCount());
	}
	
	private List<UserBlockData> getUnansweredQuestionsBlockData(UserViewpoint viewpoint, long stackedBefore) {
		// clickedTimestamp is NULL for unanswered blocks
		Query q = em.createQuery("   SELECT ubd FROM UserBlockData ubd " +
                "    WHERE ubd.user = :user " +
                "      AND ubd.block.blockType = " + BlockType.ACCOUNT_QUESTION.ordinal() + " " +
                "      AND ubd.clickedTimestamp IS NULL " +
                "      AND ubd.stackTimestamp < :stackedBefore " +
                " ORDER BY ubd.stackTimestamp DESC")
           .setParameter("user", viewpoint.getViewer())
           .setParameter("stackedBefore", new Date(stackedBefore));
		
		return TypeUtils.castList(UserBlockData.class, q.getResultList());
	}
	
	public List<BlockView> getUnansweredQuestions(UserViewpoint viewpoint, long stackedBefore) {
		List<BlockView> results = new ArrayList<BlockView>();
		
		for (UserBlockData ubd : getUnansweredQuestionsBlockData(viewpoint, stackedBefore)) {
			try {
				results.add(loadBlock(viewpoint, ubd));
			} catch (NotFoundException e) {
				logger.warn("Couldn't find/view question block {} for viewer {}",
						    ubd.getBlock().getId(), viewpoint.getViewer());
			}
		}
		
		return results;
	}	
	
	public List<Block> getOldBlocks(UserViewpoint viewpoint, String filter, long stackedBefore, int desiredCount) {
		List<Block> results = new ArrayList<Block>();
		
		for (UserBlockData ubd : getUnansweredQuestionsBlockData(viewpoint, stackedBefore)) {
			results.add(ubd.getBlock());
		}
		
		for (UserBlockData ubd : getBlocks(viewpoint, viewpoint.getViewer(), -1, stackedBefore, 0, desiredCount - results.size(), filter, false)) {
			results.add(ubd.getBlock());
		}

		
		return results;
	}	

	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Guid guid) throws NotFoundException {
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.user = :user AND ubd.block = block AND block.id = :blockId");
		q.setParameter("user", viewpoint.getViewer());
		q.setParameter("blockId", guid.toString());
		try {
			return (UserBlockData) q.getSingleResult();
		} catch (NonUniqueResultException e) {
			throw new NotFoundException("multiple UserBlockData for this block");
		} catch (NoResultException e) {
			throw new NotFoundException("no UserBlockData for blockId " + guid);
		}
	}
	
	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, BlockKey key) throws NotFoundException {
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.user = :user AND ubd.block = block AND " + getBlockClause(key));
		q.setParameter("user", viewpoint.getViewer());
		setBlockParameters(key, q);
		try {
			return (UserBlockData) q.getSingleResult();
		} catch (NonUniqueResultException e) {
			throw new NotFoundException("multiple UserBlockData for this block key");
		} catch (NoResultException e) {
			throw new NotFoundException("no UserBlockData for blockKey " + key);
		}
	}
	
	public void setBlockHushed(UserBlockData userBlockData, boolean hushed) {
 		if (hushed != userBlockData.isIgnored()) {
	 		userBlockData.setIgnored(hushed);
	 		if (hushed) {
	 			userBlockData.setIgnoredTimestampAsLong(userBlockData.getBlock().getTimestampAsLong());
	 		} else
	 			userBlockData.setStackTimestampAsLong(userBlockData.getBlock().getTimestampAsLong());
	 		
			invalidateUserBlockDataProperty(userBlockData, "ignoredTimestamp");
 		}
	}

	private static final String ELEMENT_NAME = "blocksChanged";
	private static final String NAMESPACE = CommonXmlWriter.NAMESPACE_BLOCKS;
	
	public void onEvent(BlockEvent event) {
		XmlBuilder builder = new XmlBuilder();
		builder.openElement(ELEMENT_NAME, 
				            "xmlns", NAMESPACE, 
				            "blockId", event.getBlockId().toString(),
				            "lastTimestamp", Long.toString(event.getStackTimestamp()));
		builder.closeElement();
		
		xmppMessageSystem.sendLocalMessage(event.getAffectedUsers(), builder.toString());
	}
		
	static private class PostMigrationTask implements TxRunnable {
		private String postId;
		
		PostMigrationTask(String postId) {
			this.postId = postId;
		}
		
		public void run() throws RetryException {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migratePost(postId);
		}
	}

	static private class PostParticipationMigrationTask implements TxRunnable {
		private String postId;
		
		PostParticipationMigrationTask(String postId) {
			this.postId = postId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migratePostParticipation(postId);
		}
	}
	
	static private class UserMigrationTask implements TxRunnable {
		private String userId;
		
		UserMigrationTask(String postId) {
			this.userId = postId;
		}
		
		public void run() throws RetryException {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateUser(userId);
		}
	}

	static private class BlockParticipationMigrationTask implements TxRunnable {
		private String blockId;
		
		BlockParticipationMigrationTask(String blockId) {
			this.blockId = blockId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateBlockParticipation(blockId);
		}
	}
	
	static private class GroupMigrationTask implements TxRunnable {
		private String groupId;
		
		GroupMigrationTask(String groupId) {
			this.groupId = groupId;
		}
		
		public void run() throws RetryException {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateGroupChat(groupId);
			stacker.migrateGroupMembers(groupId);
		}
	}

	static private class GroupParticipationMigrationTask implements TxRunnable {
		private String groupId;
		
		GroupParticipationMigrationTask(String groupId) {
			this.groupId = groupId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateGroupChatParticipation(groupId);
		}
	}
	
	static private class GroupBlockDataMigrationTask implements TxRunnable {
		private String blockId;
		
		GroupBlockDataMigrationTask(String blockId) {
			this.blockId = blockId;
		}
		
		public void run() throws RetryException {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateGroupBlockData(blockId);
		}
	}
	
	static private class Migration implements Runnable {
		@SuppressWarnings({"unused","hiding"})
		static private final Logger logger = GlobalSetup.getLogger(Migration.class);		
		
		private ExecutorService pool;
		private long processed;
		private long errorCount;
		private Collection<TxRunnable> tasks;
		
		public Migration(Collection<TxRunnable> tasks) {
			this.tasks = tasks;
		}

		private <T> T pop(Collection<T> collection) {
			Iterator<T> i = collection.iterator();
			if (i.hasNext()) {
				T t = i.next();
				i.remove();
				return t;
			} else {
				return null;
			}
		}
		
		// the idea is that we can call run() lots of times (potentially concurrently)
		// and each call migrates one thing
		public void run() {
			try {
				final TxRunnable task;
				
				// synchronize access to the pool of work to do
				synchronized(this) {
					task = pop(tasks);
				}
				
				// but do work outside the lock; one reason for doing the retry is
				// that we do "if Block doesn't exist, create it". That should only really 
				// be a problem if two copies of the migration task were run simultaneously.
				// since we never create Blocks for existing database objects in other
				// cases. A more important reason in the long term is that other operations
				// performed by a migration task could need the retry - see, for example,
				// the comment in migratePost().
				TxUtils.runInTransaction(task);
				
				synchronized(this) {
					processed += 1;
					if ((processed % 100) == 0) {
						logger.debug("processed {} items, {} remaining", processed, getRemainingItems());
					}
				}
			} catch (RuntimeException e) {
				// we need to catch the exception ourselves, otherwise it
				// ends up on stdout instead of in the log, and terminates
				// the thread for no good reason
				logger.error("Migration thread threw exception", e);
				errorCount += 1;
			}
		}
		
		public synchronized long getRemainingItems() {
			return tasks.size();
		}
		
		public void start() {
			if (pool != null)
				throw new IllegalStateException("Stacker migration started twice");
			
			// 8 is a guess at what might be good with 4 cpus
			pool = ThreadUtils.newFixedThreadPool("stacker migration", 8);
			
			// run 1 task per item to migrate, keep in mind
			// that these tasks will start while we're still 
			// piling them in, so there's some reentrancy
			// from this point forward
			long items = getRemainingItems();
			
			logger.info("Starting stacker migration with {} items to process", items);
			
			while (items > 0) {
				pool.execute(this);
				--items;
			}
		}
		
		public void shutdown(boolean forceImmediate) {
			if (pool == null)
				throw new IllegalStateException("Stacker migration has not been started");
			
			if (forceImmediate) {
				// this will turn any remaining run() calls into no-ops
				synchronized (this) {
					tasks.clear();
				}
			}
			
			logger.info("Shutting down stacker migration forceImmediate={} processed={}", forceImmediate, processed);

			pool.shutdown();
			try {
				pool.awaitTermination(60*60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.warn("Interrupted while waiting for stacker migration to finish");
			}
			pool = null;
			logger.info("Stacker migration shut down, {} items processed, {} errors", processed, errorCount);
		}
	}
	
	private void runMigration(Collection<TxRunnable> tasks) {
		final Migration migration = new Migration(tasks);
		migration.start();
		
		// start a thread to clean up the migration ; if we blocked to do this
		// our transaction would probably time out
		Thread t = new Thread("stacker migration waiter") {
			@Override
			public void run() {
				migration.shutdown(false); // false = wait for stuff to complete		
			}
		};
		t.setDaemon(true);
		t.start();		
	}
	
	public void migrateEverything() {	
		List<TxRunnable> tasks = new ArrayList<TxRunnable>();
		
		Query q = em.createQuery("SELECT post.id FROM Post post");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new PostMigrationTask(id));

		q = em.createQuery("SELECT user.id FROM User user");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new UserMigrationTask(id));

		q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new GroupMigrationTask(id));
		
		tasks.addAll(generateParticipationMigrationTasks());
		
		runMigration(tasks);
	}
	
	public void migrateParticipation() {		
		runMigration(generateParticipationMigrationTasks());		
	}
	
	public void migrateGroupBlockData() {
		Query q = em.createQuery("SELECT block.id FROM Block block");
		List<String> blocks = TypeUtils.castList(String.class, q.getResultList());
	
		List<TxRunnable> tasks = new ArrayList<TxRunnable>();
		for (String id : blocks) {
			tasks.add(new GroupBlockDataMigrationTask(id));
		}
		
		runMigration(tasks);
	}
		
	private List<TxRunnable> generateParticipationMigrationTasks() {
		List<TxRunnable> tasks = new ArrayList<TxRunnable>();
		
		Query q = em.createQuery("SELECT post.id FROM Post post");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new PostParticipationMigrationTask(id));

		q = em.createQuery("SELECT block.id FROM Block block" +
				           " WHERE block.blockType = " + BlockType.MUSIC_PERSON.ordinal() +
				           " OR block.blockType = " + BlockType.FACEBOOK_PERSON.ordinal() +
				           " OR block.blockType = " + BlockType.FACEBOOK_EVENT.ordinal() +
				           " OR block.blockType = " + BlockType.BLOG_ENTRY.ordinal());
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new BlockParticipationMigrationTask(id));

		q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new GroupParticipationMigrationTask(id));
		
		return tasks;
	}
	
	public void migrateGroups() {
		List<TxRunnable> tasks = new ArrayList<TxRunnable>();

		Query q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList())) {
			tasks.add(new GroupMigrationTask(id));
			tasks.add(new GroupParticipationMigrationTask(id));	
		}

		runMigration(tasks);
	}
	
	public void migrateUsers() {
		List<TxRunnable> tasks = new ArrayList<TxRunnable>();

		Query q = em.createQuery("SELECT user.id FROM User user");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new UserMigrationTask(id));
		
		runMigration(tasks);
	}
	
	// migratePostParticipation should also be called to do a complete migration of post participation
	public void migratePost(String postId) throws RetryException {
		logger.debug("    migrating post {}", postId);
		Post post = em.find(Post.class, postId);
		Block block = getOrCreateBlock(getPostKey(post), post.isPublic());
		long activity = post.getPostDate().getTime();
		StackReason reason = StackReason.NEW_BLOCK;
		
		// Now that PersonPostData is gone, we can no longer migrate it here...

		List<? extends ChatMessage> messages = chatSystem.getNewestMessages(block, 1);
		if (messages.size() > 0) {
			ChatMessage m = messages.get(0);
			long newestMessageTime = m.getTimestamp().getTime();
			if (newestMessageTime > activity) {
				activity = newestMessageTime;
				reason = StackReason.CHAT_MESSAGE;
			}
		}
		
		// This will update the block timestamp then asynchronously update the
		// cached user timestamps after this transaction commits. It would also 
		// create any  UserBlockData objects that didn't exist at that point, but 
		// they should have all been created above by migrating PersonPostData
		stack(block, activity, null, !(post instanceof FeedPost) || messages.size() > 0, reason);		
	}
	
	public void migratePostParticipation(String postId) {
		// set the participatedTimestamp for the corresponding userBlockData to be the time
		// the post was sent or the chat message on the post was sent 
		logger.debug("    migrating post participation {}", postId);
		Post post = em.find(Post.class, postId);
		Block block;
		try {
		    block = queryBlock(getPostKey(post));		
		} catch (NotFoundException e) {
			logger.warn("Block corresponding to post {} was not found, won't migrate post participation", post);
			return;
		}
		// try and catch blocks are per UserBlockData query, because we can still update the other
		// UserBlackData(s) if one is not found
	    try {
	    	// feeds have null for the poster
	    	if (post.getPoster() != null) {
	            UserBlockData posterBlockData = queryUserBlockData(block, post.getPoster());
	            posterBlockData.setParticipatedTimestamp(post.getPostDate());
	    	}
	    } catch (NoResultException e) {
		    logger.warn("UserBlockData for block {} and poster user {} was not found", block, post.getPoster());
	    }
	    // we want the ordering to be ascending by the timestamp to set the participation
	    // timestamps in the right order, which is what getMessages should return
		for (ChatMessage message : chatSystem.getMessages(block, 0)) {
			try {
		        UserBlockData fromUserBlockData = queryUserBlockData(block, message.getFromUser());
		        fromUserBlockData.setParticipatedTimestamp(message.getTimestamp());
			} catch (NoResultException e) {
				logger.warn("UserBlockData for post block {} and from user {} was not found", block, message.getFromUser());
				// the user must have left some group the post was sent to, so was no longer on the expanded 
				// recepients list when the post was migrated, we can still have the participated timestamp 
				// set on a deleted UserBlockData in case the user re-joins the group
				UserBlockData ubd = new UserBlockData(message.getFromUser(), block, message.getTimestamp().getTime());
				ubd.setDeleted(true);
				em.persist(ubd);
			}
		}
	}
	
	public void migrateUser(String userId) throws RetryException {
		logger.debug("    migrating user {}", userId);
		User user = em.find(User.class, userId);
		BlockKey key = getMusicPersonKey(user.getGuid());
		getOrCreateBlock(key);
		long lastPlayTime = musicSystem.getLatestPlayTime(SystemViewpoint.getInstance(), user);
		if (lastPlayTime != 0) {			
			stack(key, lastPlayTime, StackReason.BLOCK_UPDATE);
		}
		migrateFlickr(user);
		migrateYouTube(user);
		migrateMySpace(user);
		migrateTwitter(user);
	}
	
	public void migrateBlockParticipation(String blockId) {
		// the blocks that we get here should have the user set as data1 who played the
		// music or whose external account it is, so just set the participation timestamp to
		// the value of the block's activity timestamp
		logger.debug("    migrating music or external account block participation {}", blockId);
		Block block = em.find(Block.class, blockId);
		User user = em.find(User.class, block.getData1AsGuid().toString());
		try {
	        UserBlockData userBlockData = queryUserBlockData(block, user);
	        userBlockData.setParticipatedTimestamp(block.getTimestamp());
		} catch (NoResultException e) {	
			logger.warn("UserBlockData for block {} and user {} was not found when igrating block participation",
					    block, user);
			// this might happen because we didn't include the user in all external account updates,
			// so it's time to create the UserBlockData (this might actually only happen if you had
			// facebook external accounts, because not including the user was only implemented them, 
			// but not music or blog updates)
			UserBlockData ubd = new UserBlockData(user, block, block.getTimestamp().getTime());
			em.persist(ubd);
		}
	}
	
	// migrateGroupChatParticipation should also be called to do a complete migration of group chat participation
	public void migrateGroupChat(String groupId) throws RetryException {
		logger.debug("    migrating group chat for {}", groupId);
		Group group = em.find(Group.class, groupId);
		Block block = getOrCreateBlock(getGroupChatKey(group.getGuid()), group.isPublic());
		List<? extends ChatMessage> messages = chatSystem.getNewestMessages(block, 1);
		if (!messages.isEmpty()) {
			stack(getGroupChatKey(group.getGuid()), messages.get(0).getTimestamp().getTime(), StackReason.CHAT_MESSAGE);
		}
	}
	
	public void migrateGroupChatParticipation(String groupId) {
		// set the participatedTimestamp for the corresponding userBlockData to be the time when 
		// the group chat message was sent 
		logger.debug("    migrating group chat participation for {}", groupId);
		Group group = em.find(Group.class, groupId);		
		Block block;
		try {
		    block = queryBlock(getGroupChatKey(group.getGuid()));		
		} catch (NotFoundException e) {
			logger.warn("Block corresponding to group {} was not found, won't migrate group participation", group);
			return;
		}
		// try and catch blocks are per UserBlockData query, because we can still update the other
		// UserBlackData(s) if one is not found
		for (ChatMessage message: chatSystem.getMessages(block, 0)) {
			try {
		        UserBlockData fromUserBlockData = queryUserBlockData(block, message.getFromUser());
		        fromUserBlockData.setParticipatedTimestamp(message.getTimestamp());
			} catch (NoResultException e) {
				logger.warn("UserBlockData for group chat block {} and from user {} was not found", block, message.getFromUser());
				// the user must have left the group, we can still have the participated timestamp set on a 
				// deleted UserBlockData in case the user re-joins the group
				UserBlockData ubd = new UserBlockData(message.getFromUser(), block, message.getTimestamp().getTime());
				ubd.setDeleted(true);
				em.persist(ubd);
			}
		}		
	}
	
	public void migrateGroupMembers(String groupId) throws RetryException {
		logger.debug("    migrating group members for {}", groupId);
		Group group = em.find(Group.class, groupId);
		for (GroupMember member : group.getMembers()) {
			AccountClaim a = member.getMember().getAccountClaim();
			if (a != null) {
				BlockKey key = getGroupMemberKey(member.getGroup().getGuid(), a.getOwner().getGuid());
				Block block = getOrCreateBlock(key, group.isPublic());
				// we set a timestamp of 0, since we have no way of knowing the right
				// timestamp, and we don't want to make a big pile of group member blocks 
				// at the top of the stack whenever we run a migration
				stack(block, 0, a.getOwner(), true, StackReason.VIEWER_COUNT);
			}
		}
	}

	public void migrateFlickr(User user) throws RetryException {
		getHandler(FlickrPersonBlockHandler.class, BlockType.FLICKR_PERSON).migrate(user);
	}
	
	public void migrateYouTube(User user) throws RetryException {
		getHandler(YouTubeBlockHandler.class, BlockType.YOUTUBE_PERSON).migrate(user);
	}
	
	public void migrateMySpace(User user) throws RetryException {
		getHandler(MySpacePersonBlockHandler.class, BlockType.MYSPACE_PERSON).migrate(user);
	}	
	
	public void migrateTwitter(User user) throws RetryException {
		getHandler(TwitterPersonBlockHandler.class, BlockType.TWITTER_PERSON).migrate(user);
	}
	
	public void migrateGroupBlockData(String blockId) throws RetryException {
		logger.debug("    migrating group block data for {}", blockId);
		
		Block block = em.find(Block.class, blockId);
        boolean isGroupParticipation;
        StackReason reason = StackReason.BLOCK_UPDATE;

        isGroupParticipation = false;
		switch (block.getBlockType()) {
		case POST:
			// The participation timestamp here will not be set quite right; we
			// ignore chatting which is participation even on a FeedPost, and
			// we use the block timestamp rather than the post timestamp, but
			// we can fix up more accurately with SQL once the GroupBlockData
			// objects are created.
			//
			Post post = em.find(Post.class, block.getData1AsGuid().toString());
			if (post == null) {
				logger.warn("Can't find post when migrating block {}", block.getId());
				return;
			}
			isGroupParticipation = !(post instanceof FeedPost);
			reason = StackReason.NEW_BLOCK;
			break;
		case GROUP_MEMBER:
			break;
		case GROUP_CHAT:
			isGroupParticipation = true;
			reason = StackReason.CHAT_MESSAGE;
			break;
		case GROUP_REVISION:
			isGroupParticipation = true;
			break;
		case MUSIC_PERSON:
		case FACEBOOK_PERSON:
		case FACEBOOK_EVENT:
		case FLICKR_PERSON:
		case FLICKR_PHOTOSET:
		case YOUTUBE_PERSON:
		case MYSPACE_PERSON:
		case BLOG_ENTRY:
		case DELICIOUS_PUBLIC_BOOKMARK:
		case MUSIC_CHAT:
		case TWITTER_PERSON:
		case DIGG_DUGG_ENTRY:
		case REDDIT_ACTIVITY_ENTRY:
		case NETFLIX_MOVIE:
		case ACCOUNT_QUESTION:
		case GOOGLE_READER_SHARED_ITEM:
		case PICASA_PERSON:
		case SMUGMUG_PERSON:
		case AMAZON_REVIEW:
		case AMAZON_WISH_LIST_ITEM:	
			isGroupParticipation = false;
			break;
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE:
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF:
		case OBSOLETE_BLOG_PERSON:
			throw new RuntimeException("obsolete block type used " + block);
			// don't add a default, it hides compiler warnings
		}

		updateGroupBlockDatas(block, isGroupParticipation, reason);
	}

	public String getUserStackFilterPrefs(User user) {
		return user.getAccount().getStackFilter();
	}
	
	public void setUserStackFilterPrefs(User user, String filter) {
		String oldFilter = getUserStackFilterPrefs(user);
		if ((filter == null && oldFilter != null) ||
			(filter != null && !filter.equals(oldFilter))) 
		{
			user.getAccount().setStackFilter(filter);
			DataService.currentSessionRW().changed(UserDMO.class, user.getGuid(), "stackFilter");
		}
	}
}
