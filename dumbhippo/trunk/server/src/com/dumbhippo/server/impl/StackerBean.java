package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collection;
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

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.BlockEvent;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockKey;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.FeedPost;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.StackInclusion;
import com.dumbhippo.persistence.StackReason;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmppMessageSender;
import com.dumbhippo.server.blocks.BlockHandler;
import com.dumbhippo.server.blocks.BlockNotVisibleException;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.blocks.BlogBlockHandler;
import com.dumbhippo.server.blocks.FacebookBlockHandler;
import com.dumbhippo.server.blocks.FlickrPersonBlockHandler;
import com.dumbhippo.server.blocks.FlickrPhotosetBlockHandler;
import com.dumbhippo.server.blocks.GroupChatBlockHandler;
import com.dumbhippo.server.blocks.GroupMemberBlockHandler;
import com.dumbhippo.server.blocks.MusicPersonBlockHandler;
import com.dumbhippo.server.blocks.PostBlockHandler;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.PersonMugshotView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Service
public class StackerBean implements Stacker, SimpleServiceMBean, LiveEventListener<BlockEvent> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackerBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private MusicSystem musicSystem;
	
	@EJB
	private PersonViewer personViewer;

	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private XmppMessageSender xmppMessageSystem;	
	
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
		case BLOG_PERSON:
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
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE:
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF:
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
				   "AND block.data1=:data1 AND block.data2='' AND block.data3=:data3 " + 
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
	
	private UserBlockData queryUserBlockData(User user, BlockKey key) throws NotFoundException {
		Guid data1 = key.getData1();
		Guid data2 = key.getData2();
		long data3 = key.getData3();
		StackInclusion inclusion = key.getInclusion();

		Query q;
		if (data1 != null && data2 != null) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data1=:data1 AND block.data2=:data2 AND block.data3=:data3 " +
					"AND block.inclusion = :inclusion");
			q.setParameter("data1", data1.toString());
			q.setParameter("data2", data2.toString());
		} else if (data1 != null) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data1=:data1 AND block.data2='' AND block.data3=:data3 " +
					"AND block.inclusion = :inclusion");
			q.setParameter("data1", data1.toString());
		} else if (data2 != null) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data2=:data2 AND block.data1='' AND block.data3=:data3 " +
					"AND block.inclusion = :inclusion");
			q.setParameter("data2", data2);
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block  " + key);
		}
		q.setParameter("data3", data3);
		q.setParameter("user", user);
		q.setParameter("type", key.getBlockType());
		if (inclusion == null)
			throw new IllegalArgumentException("missing inclusion in key " + key);
		q.setParameter("inclusion", inclusion);
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
	
	/** don't call this awkward method directly, call one of the two other overloads */
	private Block getOrCreateBlock(BlockKey key, boolean changeDefaultPublicity, boolean publicBlockIfCreated) {
		try {
			return queryBlock(key);
		} catch (NotFoundException e) {
			Block block = createBlock(key);
			if (changeDefaultPublicity)
				block.setPublicBlock(publicBlockIfCreated);
			return block;
		}
	}
	
	private Block getOrCreateBlock(BlockKey key, boolean publicBlockIfCreated) {
		return getOrCreateBlock(key, true, publicBlockIfCreated);
	}
	
	public Block getOrCreateBlock(BlockKey key) {
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
	
	private void updateUserBlockDatas(Block block, Set<User> desiredUsers, Guid participantId, StackReason reason) {
		int addCount;
		int removeCount;
		
		Set<Guid> affectedGuids = new HashSet<Guid>();
		
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
					old.setParticipatedTimestamp(block.getTimestamp());
					old.setParticipatedReason(reason);
				}
				if (!old.isIgnored())
					old.setStackTimestamp(block.getTimestamp());
				old.setStackReason(reason);
			} else {
				UserBlockData data = new UserBlockData(u, block, u.getGuid().equals(participantId), reason);
				em.persist(data);
				addCount += 1;
			}
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
		
		BlockEvent event = new BlockEvent(block.getGuid(), block.getTimestampAsLong(), affectedGuids);
		LiveState.getInstance().queueUpdate(event);
	}
	
	private void updateUserBlockDatas(Block block, Guid participantId, StackReason reason) {
		Set<User> desiredUsers = getHandler(block).getInterestedUsers(block);
		
		updateUserBlockDatas(block, desiredUsers, participantId, reason);
	}
	
	private void refreshUserBlockDatasDeleted(Block block) {
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
				if (old.isDeleted())
					old.setDeleted(false);
			}
		}
		// the rest of "existing" is users who no longer are in the desired set
		for (User u : existing.keySet()) {
			UserBlockData old = existing.get(u);
			if (!old.isDeleted())
				old.setDeleted(true);
		}
	}
	
	// note this query includes gbd.deleted=1
	private List<GroupBlockData> queryGroupBlockDatas(Block block) {
		Query q = em.createQuery("SELECT gbd FROM GroupBlockData gbd WHERE gbd.block = :block");
		q.setParameter("block", block);
		return TypeUtils.castList(GroupBlockData.class, q.getResultList());
	}
	
	private void updateGroupBlockDatas(Block block, Set<Group> desiredGroups, boolean isGroupParticipation, StackReason reason) {
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

	private void updateGroupBlockDatas(Block block, boolean isGroupParticipation, StackReason reason) {
		Set<Group> desiredGroups = getHandler(block).getInterestedGroups(block);
		
		updateGroupBlockDatas(block, desiredGroups, isGroupParticipation, reason);
	}
	
	private void refreshGroupBlockDatasDeleted(Block block) {
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
				if (old.isDeleted())
					old.setDeleted(false);
			}
		}
		// the rest of "existing" is groups who no longer are in the desired set
		for (Group g : existing.keySet()) {
			GroupBlockData old = existing.get(g);
			if (!old.isDeleted())
				old.setDeleted(true);
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
	
	public void refreshDeletedFlags(final Block block) {
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				runner.runTaskInNewTransaction(new Runnable() {
					public void run() {
						Block attached = em.find(Block.class, block.getId());
						refreshUserBlockDatasDeleted(attached);
						refreshGroupBlockDatasDeleted(attached);
					}
				});
			}
		});		
	}
	
	public void stack(final Block block, final long activity, final User participant, final boolean isGroupParticipation, final StackReason reason) {

		// never "roll back" to an earlier timestamp
		if (block.getTimestampAsLong() < activity) { 
			block.setTimestampAsLong(activity);
		}
		
		// Now we need to create demand-create user/group block data objects and update the
		// cached user timestamps. update{User,Group{BlockDatas(block) are always safe to call
		// at any point without worrying about ordering. We queue the update asynchronously
		// after commit, so we can do retries when demand-creating {User,Group}BlockData.
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				runner.runTaskRetryingOnConstraintViolation(new Runnable() {
					public void run() {
						Block attached = em.find(Block.class, block.getId());
						updateUserBlockDatas(attached, (participant != null ? participant.getGuid() : null), reason);
						updateGroupBlockDatas(attached, isGroupParticipation, reason);
					}
				});
			}
		});
	}
	
	public void stack(Block block, long activity, StackReason reason) {
		stack(block, activity, null, false, reason);
	}
	
	public Block stack(BlockKey key, long activity, User participant, boolean isGroupParticipation, StackReason reason) {
		Block block;
		try {
			block = queryBlock(key);
		} catch (NotFoundException e) {
			throw new RuntimeException("stack() called on block that doesn't exist; probably means a migration is needed. key=" + key, e);
		}
        stack(block, activity, participant, isGroupParticipation, reason);
        return block;
	}
	
	public Block stack(BlockKey key, long activity, StackReason reason) {
		return stack(key, activity, null, false, reason);
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
		
		// if we weren't previously clicked on, then increment the count.
		// (FIXME is this a race or does it work due to transaction semantics? not sure)
		if (!ubd.isClicked())
			ubd.getBlock().setClickedCount(ubd.getBlock().getClickedCount() + 1);
		
		if (ubd.getClickedTimestampAsLong() < clickedTime)
			ubd.setClickedTimestampAsLong(clickedTime);
		
		// we automatically unignore anything you click on
		if (ubd.isIgnored())
			setBlockHushed(ubd, false);
		
		logger.debug("due to click, restacking block {} with new time {}",
				ubd.getBlock(), clickedTime);
		
		if (!BlockView.clickedCountIsSignificant(ubd.getBlock().getClickedCount()))
			return;
		
		// now update the timestamp in the block (if it's newer)
		// and update user caches for all users
		stack(ubd.getBlock(), clickedTime, StackReason.VIEWER_COUNT);
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
	private BlockKey getPostKey(Guid postId) {
		return getHandler(PostBlockHandler.class, BlockType.POST).getKey(postId);
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
	private BlockView prepareBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws BlockNotVisibleException {
		return getHandler(block).getUnpopulatedBlockView(viewpoint, block, ubd);
	} 
	
	// Populating the block view fills in all the details that were skipped at
	//   the prepare stage and makes it ready for viewing by the user.
	private void populateBlockView(BlockView blockView) {
		getHandler(blockView).populateBlockView(blockView);
	}
	
	public BlockView getBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws NotFoundException {
		try {
			BlockView blockView = prepareBlockView(viewpoint, block, ubd);
			populateBlockView(blockView);
			return blockView;
		} catch (BlockNotVisibleException e) {
			throw new NotFoundException("Can't see this block", e);
		}
	}
	
	public BlockView loadBlock(Viewpoint viewpoint, UserBlockData ubd) throws NotFoundException {	
		return getBlockView(viewpoint, ubd.getBlock(), ubd);
	}	
	
	/**
	 * 
	 * @param viewpoint
	 * @param user
	 * @param lastTimestamp -1 to ignore this param, otherwise the timestamp to get changes since
	 * @param start
	 * @param count
	 * @param participantOnly if true, only include blocks where someone participated, and sort by participation time 
	 * @return
	 */
	private List<UserBlockData> getBlocks(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count, boolean participantOnly) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("SELECT ubd FROM UserBlockData ubd, Block block " + 
                	 " WHERE ubd.user = :user AND ubd.deleted = 0 AND ubd.block = block ");
		
		if (participantOnly)
			sb.append(" AND ubd.participatedTimestamp IS NOT NULL ");		

		/* Timestamp clause */
		
		// if lastTimestamp == 0 then all blocks are included so just skip the test in the sql
		if (lastTimestamp > 0)
			sb.append(" AND ubd.stackTimestamp >= :timestamp ");
		
		/* Inclusion clause */
		sb.append(" AND (block.inclusion = ");
		sb.append(StackInclusion.IN_ALL_STACKS.ordinal());
		
		sb.append(" OR (block.data1 = :viewedGuid AND block.inclusion = ");
		if (viewpoint.isOfUser(user))
			sb.append(StackInclusion.ONLY_WHEN_VIEWING_SELF.ordinal());
		else
			sb.append(StackInclusion.ONLY_WHEN_VIEWED_BY_OTHERS.ordinal());
		sb.append("))");
		
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
		q.setParameter("user", user);
		q.setParameter("viewedGuid", user.getGuid().toString());
		
		return TypeUtils.castList(UserBlockData.class, q.getResultList());		
	}
	
	private interface BlockSource {
		List<Pair<Block, UserBlockData>> get(int start, int count);
	}

	public void pageStack(Viewpoint viewpoint, BlockSource source, Pageable<BlockView> pageable, int expectedHitFactor) {
		
		// + 1 is for finding out if there are items for the next page
		int targetedNumberOfItems = pageable.getStart() + pageable.getCount() + 1;
		int firstItemToReturn = pageable.getStart();
		
		List<BlockView> stack = new ArrayList<BlockView>();
		int start = 0;
		
		while (stack.size() < targetedNumberOfItems) {
			int count = (targetedNumberOfItems - stack.size()) * expectedHitFactor;
			List<Pair<Block, UserBlockData>> blocks = source.get(start, count);
			if (blocks.isEmpty())
				break;
			
			int resultItemCount = 0;
			// Create BlockView objects for the blocks, performing access control checks
			for (Pair<Block, UserBlockData> pair : blocks) {
				Block block = pair.getFirst();
				UserBlockData ubd = pair.getSecond();
				
				try {
					stack.add(prepareBlockView(viewpoint, block, ubd));
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

	public void pageStack(final Viewpoint viewpoint, final User user, Pageable<BlockView> pageable, final boolean participantOnly) {
		
		logger.debug("getting stack for user {}", user);

       	int expectedHitFactor = 4;
		if (viewpoint.isOfUser(user))
			expectedHitFactor = 2;
		
		pageStack(viewpoint, new BlockSource() {
			public List<Pair<Block, UserBlockData>> get(int start, int count) {
				List<Pair<Block, UserBlockData>> results = new ArrayList<Pair<Block, UserBlockData>>();
				for (UserBlockData ubd : getBlocks(viewpoint, user, -1, start, count, participantOnly)) {
					results.add(new Pair<Block, UserBlockData>(ubd.getBlock(), ubd));
				}
				return results;
			}
		}, pageable, expectedHitFactor);
	}
	
	private interface ItemSource<T> {
		List<T> get(int start, int count, String filter);
	}
	
	private <T> List<T> getDistinctItems(ItemSource<T> source, int start, int count, String filter, int expectedHitFactor) {
		Set<T> distinctItems = new HashSet<T>();
		List<T> returnItems = new ArrayList<T>();
		int max = start + count;

		int chunkStart = 0;
		while (distinctItems.size() < max) {
			int chunkCount = (max - distinctItems.size()) * expectedHitFactor;
			List<T> items = source.get(chunkStart, chunkCount, filter);
			if (items.isEmpty())
				break;
			
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
		    if (items.size() < chunkCount)
		    	break;
	
		    chunkStart = chunkStart + chunkCount;
		    if (resultItemCount > 0)
		    	expectedHitFactor = items.size() / resultItemCount + 1;
		}

		return returnItems;
	}
	
	private List<User> getRecentActivityUsers(int start, int count, String groupUpdatesFilter) {
		// we expect the anonymous viewpoint here, so we only get public blocks
		Query q = em.createQuery("SELECT ubd.user FROM UserBlockData ubd, Block block " + 
                " WHERE ubd.deleted = 0 AND ubd.block = block " +
                " AND ubd.participatedTimestamp IS NOT NULL " +
                " AND block.publicBlock = true " + groupUpdatesFilter +
                " ORDER BY ubd.participatedTimestamp DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		
		return TypeUtils.castList(User.class, q.getResultList());
	}
	
	public List<PersonMugshotView> getRecentUserActivity(Viewpoint viewpoint, int startUser, int userCount, int blockPerUser, boolean includeGroupUpdates) {
		List<PersonMugshotView> mugshots = new ArrayList<PersonMugshotView>();
		
		// select distinct most recently active users		
       	int expectedHitFactor = 2;
		
		String groupUpdatesFilter = "";
		if (!includeGroupUpdates) {
	        groupUpdatesFilter = " AND block.blockType != " + BlockType.GROUP_MEMBER.ordinal() + 
	                             " AND block.blockType != " + BlockType.GROUP_CHAT.ordinal(); 
		}
		
		List<User> distinctUsers = getDistinctItems(new ItemSource<User>() {
			public List<User> get(int start, int count, String filter) {
				return getRecentActivityUsers(start, count, filter);
			}
		}, startUser, userCount, groupUpdatesFilter, expectedHitFactor);
		
		for (User user : distinctUsers) {
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
	         	    BlockView blockView = getBlockView(viewpoint, ubd.getBlock(), ubd);
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
	
	public void pageRecentUserActivity(Viewpoint viewpoint, Pageable<PersonMugshotView> pageable, int blocksPerUser) {
		pageable.setResults(getRecentUserActivity(viewpoint, pageable.getStart(), pageable.getCount(), blocksPerUser, true));
		
		// Doing an exact count is expensive, our assumption is "lots and lots"
		pageable.setTotalCount(pageable.getBound());		
	}

	public List<BlockView> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count) {
	    return getStack(viewpoint, user, lastTimestamp, start, count, false);
    }
	
	public List<BlockView> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count, boolean participantOnly) {		
		
		List<BlockView> stack = new ArrayList<BlockView>();
		List<UserBlockData> blocks = getBlocks(viewpoint, user, lastTimestamp, start, count, participantOnly);
		
		// Create BlockView objects for the blocks, implicitly performing access control
		for (UserBlockData ubd : blocks) {
			try {
				stack.add(getBlockView(viewpoint, ubd.getBlock(), ubd));
			} catch (NotFoundException e) {
				// Do nothing, we can't see this block
			}
		}		

		return stack;
	}
	
	private List<GroupBlockData> getBlocks(Viewpoint viewpoint, Group group, int start, int count, boolean byParticipation) {
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
       	
		pageStack(viewpoint, new BlockSource() {
			public List<Pair<Block, UserBlockData>> get(int start, int count) {
				List<Pair<Block, UserBlockData>> results = new ArrayList<Pair<Block, UserBlockData>>();
				for (GroupBlockData gbd : getBlocks(viewpoint, group, start, count, byParticipation)) {
					results.add(new Pair<Block, UserBlockData>(gbd.getBlock(), null));
				}
				return results;
			}
		}, pageable, expectedHitFactor);
	}

	// When showing recently active groups, we want to exclude activity for
	// users in the group, because we don't want a user playing music to
	// make an old inactive group that they happen to be a member of seem active
	static final String INTERESTING_PUBLIC_GROUP_BLOCK_CLAUSE =  
        " AND block.publicBlock = true " +
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
		List<GroupMugshotView> mugshots = new ArrayList<GroupMugshotView>();
		
		// select distinct most recently active users		
       	int expectedHitFactor = 2;
		
		List<Group> distinctGroups = getDistinctItems(new ItemSource<Group>() {
			public List<Group> get(int start, int count, String filter) {
				return getRecentActivityGroups(start, count);
			}
		}, startGroup, groupCount, "", expectedHitFactor);
		
		for (Group group : distinctGroups) {
			Query q = em.createQuery("Select gbd FROM GroupBlockData gbd, Block block " + 
                " WHERE gbd.group = :group AND gbd.deleted = 0 AND gbd.block = block " +
                INTERESTING_PUBLIC_GROUP_BLOCK_CLAUSE +
                " ORDER BY gbd.participatedTimestamp DESC");
			q.setParameter("group", group);
		    q.setMaxResults(blockPerGroup);
         	List<BlockView> blocks = new ArrayList<BlockView>();
         	for (GroupBlockData gbd : TypeUtils.castList(GroupBlockData.class, q.getResultList())) {
	         	try {
	         	    BlockView blockView = getBlockView(viewpoint, gbd.getBlock(), null);
	         	    blocks.add(blockView);
	         	} catch (NotFoundException e) {
	         		// this is used on the main page, let's not risk it throwing an exception here
	         		logger.error("NotFoundException when getting what must be a public block", e);
	         	}
         	}
         	GroupView groupView = groupSystem.getGroupView(viewpoint, group);
            mugshots.add(new GroupMugshotView(groupView, blocks));      	
		}
		
		return mugshots;		
	}
	
	public void pageRecentGroupActivity(Viewpoint viewpoint, Pageable<GroupMugshotView> pageable, int blocksPerGroup) {
		pageable.setResults(getRecentGroupActivity(viewpoint, pageable.getStart(), pageable.getCount(), blocksPerGroup));
		pageable.setTotalCount(groupSystem.getPublicGroupCount());
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
	 		if (hushed)
	 			userBlockData.setIgnoredTimestampAsLong(userBlockData.getBlock().getTimestampAsLong());
	 		else
	 			userBlockData.setStackTimestampAsLong(userBlockData.getBlock().getTimestampAsLong());
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
		
	static private class PostMigrationTask implements Runnable {
		private String postId;
		
		PostMigrationTask(String postId) {
			this.postId = postId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migratePost(postId);
		}
	}

	static private class PostParticipationMigrationTask implements Runnable {
		private String postId;
		
		PostParticipationMigrationTask(String postId) {
			this.postId = postId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migratePostParticipation(postId);
		}
	}
	
	static private class UserMigrationTask implements Runnable {
		private String userId;
		
		UserMigrationTask(String postId) {
			this.userId = postId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateUser(userId);
		}
	}

	static private class BlockParticipationMigrationTask implements Runnable {
		private String blockId;
		
		BlockParticipationMigrationTask(String blockId) {
			this.blockId = blockId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateBlockParticipation(blockId);
		}
	}
	
	static private class GroupMigrationTask implements Runnable {
		private String groupId;
		
		GroupMigrationTask(String groupId) {
			this.groupId = groupId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateGroupChat(groupId);
			stacker.migrateGroupMembers(groupId);
		}
	}

	static private class GroupParticipationMigrationTask implements Runnable {
		private String groupId;
		
		GroupParticipationMigrationTask(String groupId) {
			this.groupId = groupId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateGroupChatParticipation(groupId);
		}
	}
	
	static private class GroupBlockDataMigrationTask implements Runnable {
		private String blockId;
		
		GroupBlockDataMigrationTask(String blockId) {
			this.blockId = blockId;
		}
		
		public void run() {
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
		private Collection<Runnable> tasks;
		
		public Migration(Collection<Runnable> tasks) {
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
				final Runnable task;
				
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
				TransactionRunner runner = EJBUtil.defaultLookup(TransactionRunner.class);
				runner.runTaskRetryingOnConstraintViolation(task);
				
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
	
	private void runMigration(Collection<Runnable> tasks) {
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
		List<Runnable> tasks = new ArrayList<Runnable>();
		
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
	
		List<Runnable> tasks = new ArrayList<Runnable>();
		for (String id : blocks) {
			tasks.add(new GroupBlockDataMigrationTask(id));
		}
		
		runMigration(tasks);
	}
		
	private List<Runnable> generateParticipationMigrationTasks() {
		List<Runnable> tasks = new ArrayList<Runnable>();
		
		Query q = em.createQuery("SELECT post.id FROM Post post");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new PostParticipationMigrationTask(id));

		q = em.createQuery("SELECT block.id FROM Block block" +
				           " WHERE block.blockType = " + BlockType.MUSIC_PERSON.ordinal() +
				           " OR block.blockType = " + BlockType.FACEBOOK_PERSON.ordinal() +
				           " OR block.blockType = " + BlockType.FACEBOOK_EVENT.ordinal() +
				           " OR block.blockType = " + BlockType.BLOG_PERSON.ordinal());
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new BlockParticipationMigrationTask(id));

		q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new GroupParticipationMigrationTask(id));
		
		return tasks;
	}
	
	public void migrateGroups() {
		List<Runnable> tasks = new ArrayList<Runnable>();

		Query q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList())) {
			tasks.add(new GroupMigrationTask(id));
			tasks.add(new GroupParticipationMigrationTask(id));	
		}

		runMigration(tasks);
	}
	
	public void migrateUsers() {
		List<Runnable> tasks = new ArrayList<Runnable>();

		Query q = em.createQuery("SELECT user.id FROM User user");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new UserMigrationTask(id));
		
		runMigration(tasks);
	}
	
	// migratePostParticipation should also be called to do a complete migration of post participation
	public void migratePost(String postId) {
		logger.debug("    migrating post {}", postId);
		Post post = em.find(Post.class, postId);
		Block block = getOrCreateBlock(getPostKey(post.getGuid()), post.isPublic());
		long activity = post.getPostDate().getTime();
		StackReason reason = StackReason.NEW_BLOCK;
		
		// Now that PersonPostData is gone, we can no longer migrate it here...

		List<PostMessage> messages = postingBoard.getNewestPostMessages(post, 1);
		if (messages.size() > 0) {
			PostMessage m = messages.get(0);
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
		    block = queryBlock(getPostKey(post.getGuid()));		
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
	    // timestamps in the right order, which is what getPostMessages should return
		List<PostMessage> messages = postingBoard.getPostMessages(post, 0);
		for (PostMessage message: messages) {
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
	
	public void migrateUser(String userId) {
		logger.debug("    migrating user {}", userId);
		User user = em.find(User.class, userId);
		BlockKey key = getMusicPersonKey(user.getGuid());
		getOrCreateBlock(key);
		long lastPlayTime = musicSystem.getLatestPlayTime(SystemViewpoint.getInstance(), user);
		if (lastPlayTime != 0) {			
			stack(key, lastPlayTime, StackReason.BLOCK_UPDATE);
		}
		migrateFlickr(user);
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
	public void migrateGroupChat(String groupId) {
		logger.debug("    migrating group chat for {}", groupId);
		Group group = em.find(Group.class, groupId);
		getOrCreateBlock(getGroupChatKey(group.getGuid()), group.isPublic());
		List<GroupMessage> messages = groupSystem.getNewestGroupMessages(group, 1);
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
		List<GroupMessage> messages = groupSystem.getGroupMessages(group, 0);
		for (GroupMessage message: messages) {
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
	
	public void migrateGroupMembers(String groupId) {
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

	public void migrateFlickr(User user) {
		getHandler(FlickrPersonBlockHandler.class, BlockType.FLICKR_PERSON).migrate(user);
	}
	
	public void migrateGroupBlockData(String blockId) {
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
		case MUSIC_PERSON:
		case FACEBOOK_PERSON:
		case FACEBOOK_EVENT:	
		case BLOG_PERSON:
		case FLICKR_PERSON:
		case FLICKR_PHOTOSET:
			isGroupParticipation = false;
			break;
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE:
		case OBSOLETE_EXTERNAL_ACCOUNT_UPDATE_SELF:
			throw new RuntimeException("obsolete block type used " + block);
			// don't add a default, it hides compiler warnings
		}

		updateGroupBlockDatas(block, isGroupParticipation, reason);
	}
}
