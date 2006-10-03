package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.ejb.Service;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.BlockEvent;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmppMessageSender;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.BlockView;
import com.dumbhippo.server.views.GroupChatBlockView;
import com.dumbhippo.server.views.GroupMemberBlockView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.MusicPersonBlockView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.PostBlockView;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Service
public class StackerBean implements Stacker, SimpleServiceMBean, LiveEventListener<BlockEvent> {

	static final private boolean disabled = false;
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackerBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider identitySpider;
	
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
	
	private Map<Guid,CacheEntry> userCacheEntries = new HashMap<Guid,CacheEntry>();
	
	public void start() throws Exception {
		LiveState.addEventListener(BlockEvent.class, this);
	}

	public void stop() throws Exception {
		LiveState.removeEventListener(BlockEvent.class, this);
	}
	
	private Block queryBlock(BlockType type, Guid data1, Guid data2, long data3) throws NotFoundException {
		Query q;
		if (data1 != null && data2 != null) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type " +
					           "AND block.data1=:data1 AND block.data2=:data2 AND block.data3=:data3");
			q.setParameter("data1", data1.toString());
			q.setParameter("data2", data2.toString());			
		} else if (data1 != null) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type " +
					           "AND block.data1=:data1 AND block.data2='' AND block.data3=:data3");
			q.setParameter("data1", data1.toString());
		} else if (data2 != null) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type " +
					           "AND block.data2=:data2 AND block.data1='' AND block.data3=:data3");
			q.setParameter("data2", data2.toString());	
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block type " + type);
		}
		q.setParameter("data3", data3);
		q.setParameter("type", type);
		try {
			return (Block) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("no block with type " + type + " data1 " + data1 + " data2 " + data2 + " data3 " + data3, e);
		}
	}

	private UserBlockData queryUserBlockData(User user, BlockType type, Guid data1, Guid data2, long data3) throws NotFoundException {
		Query q;
		if (data1 != null && data2 != null) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data1=:data1 AND block.data2=:data2 AND block.data3=:data3");
			q.setParameter("data1", data1.toString());
			q.setParameter("data2", data2.toString());
		} else if (data1 != null) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data1=:data1 AND block.data2='' AND block.data3=:data3");
			q.setParameter("data1", data1.toString());
		} else if (data2 != null) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data2=:data2 AND block.data1='' AND block.data3=:data3");
			q.setParameter("data2", data2);
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block type " + type);
		}
		q.setParameter("data3", data3);
		q.setParameter("user", user);
		q.setParameter("type", type);
		try {
			return (UserBlockData) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("no UserBlockData with type " + type + " data1 " + data1 + " data2 " + data2 + " user " + user, e);
		}
	}
	
	private Block createBlock(BlockType type, Guid data1, Guid data2, long data3) {
		Block block = new Block(type, data1, data2, data3);
		em.persist(block);
		
		return block;
	}
	
	private Block createBlock(BlockType type, Guid data1, Guid data2) {
		return createBlock(type, data1, data2, -1);
	}
	
	private Block getOrCreateBlock(BlockType type, Guid data1, Guid data2, long data3) {
		try {
			return queryBlock(type, data1, data2, data3);
		} catch (NotFoundException e) {
			return createBlock(type, data1, data2, data3);
		}
	}
	
	private Block getOrCreateBlock(BlockType type, Guid data1, Guid data2) {
		return getOrCreateBlock(type, data1, data2, -1);
	}
	
	private Block getUpdatingTimestamp(BlockType type, Guid data1, Guid data2, long data3, long activity) {
		Block block;
		try {
			logger.debug("will query for a block with data1: {}, data2: {}, data3: {}", new Object[]{data1, data2, data3});
			block = queryBlock(type, data1, data2, data3);
			logger.debug("found block");
		} catch (NotFoundException e) {
			return null;
		}
		if (block.getTimestampAsLong() < activity) // never "roll back"
			block.setTimestampAsLong(activity);
		
		return block;
	}
	
	// note this query includes ubd.deleted=1
	private List<UserBlockData> queryUserBlockDatas(Block block) {
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd WHERE ubd.block = :block");
		q.setParameter("block", block);
		return TypeUtils.castList(UserBlockData.class, q.getResultList());
	}
	
	
	private void updateUserBlockDatas(Block block, Set<User> desiredUsers) {
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
		
		for (User u : desiredUsers) {
			affectedGuids.add(u.getGuid());
			
			UserBlockData old = existing.get(u);
			if (old != null) {
				existing.remove(u);
				old.setDeleted(false);
			} else {
				UserBlockData data = new UserBlockData(u, block);
				em.persist(data);
			}
		}
		// the rest of "existing" is users who no longer are in the desired set
		for (User u : existing.keySet()) {
			UserBlockData old = existing.get(u);
			old.setDeleted(true);
		}
		
		BlockEvent event = new BlockEvent(block.getGuid(), block.getTimestampAsLong(), affectedGuids);
		LiveState.getInstance().queueUpdate(event);
	}

	private Set<User> getUsersWhoCare(Block block) {
		return getUsersWhoCare(block, true);
	}
	
	private Set<User> getUsersWhoCare(Block block, boolean includeSelf) {
		User user;
		try {
			user = EJBUtil.lookupGuid(em, User.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Set<User> peopleWhoCare = identitySpider.getUsersWhoHaveUserAsContact(SystemViewpoint.getInstance(), user);
        if (includeSelf) 
		    peopleWhoCare.add(user);  
		
		return peopleWhoCare;	
	}
	
	private Set<User> getDesiredUsersForMusicPerson(Block block) {
		if (block.getBlockType() != BlockType.MUSIC_PERSON)
			throw new IllegalArgumentException("wrong type block");
        
		return getUsersWhoCare(block);
	}
	
	private Set<User> getDesiredUsersForGroupChat(Block block) {
		if (block.getBlockType() != BlockType.GROUP_CHAT)
			throw new IllegalArgumentException("wrong type block");
		Group group;
		try {
			group = EJBUtil.lookupGuid(em, Group.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Set<User> groupMembers = groupSystem.getUserMembers(SystemViewpoint.getInstance(), group);
		return groupMembers;
	}

	private Set<User> getDesiredUsersForPost(Block block) {
		if (block.getBlockType() != BlockType.POST)
			throw new IllegalArgumentException("wrong type block");

		Post post;
		try {
			post = EJBUtil.lookupGuid(em, Post.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Set<User> postRecipients = new HashSet<User>();
		Set<Resource> resources = post.getExpandedRecipients();
		for (Resource r : resources) {
			AccountClaim a = r.getAccountClaim();
			if (a != null)
				postRecipients.add(a.getOwner());
		}
		return postRecipients;
	}

	private Set<User> getDesiredUsersForGroupMember(Block block) {
		if (block.getBlockType() != BlockType.GROUP_MEMBER)
			throw new IllegalArgumentException("wrong type block");
		
		Group group = em.find(Group.class, block.getData1AsGuid().toString());
		
		Set<User> recipients = groupSystem.getMembershipChangeRecipients(group);
		return recipients;
	}
	
	private Set<User> getDesiredUsersForExternalAccountUpdate(Block block) {
		if (block.getBlockType() != BlockType.EXTERNAL_ACCOUNT_UPDATE)
			throw new IllegalArgumentException("wrong type block");
		
		if (block.getData3() == ExternalAccountType.FACEBOOK.ordinal()) {
			return getUsersWhoCare(block, false);
		}
	
		return getUsersWhoCare(block);
	}

	private Set<User> getDesiredUsersForExternalAccountUpdateSelf(Block block) {
		if (block.getBlockType() != BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF)
			throw new IllegalArgumentException("wrong type block");

        try {
			return Collections.singleton(EJBUtil.lookupGuid(em, User.class, block.getData1AsGuid()));
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private void updateUserBlockDatas(Block block) {
		Set<User> desiredUsers = null;
		switch (block.getBlockType()) {
		case POST:
			desiredUsers = getDesiredUsersForPost(block);
			break;
		case GROUP_CHAT:
			desiredUsers = getDesiredUsersForGroupChat(block);
			break;
		case MUSIC_PERSON:
			desiredUsers = getDesiredUsersForMusicPerson(block);
			break;
		case GROUP_MEMBER:
			desiredUsers = getDesiredUsersForGroupMember(block);
			break;
		case EXTERNAL_ACCOUNT_UPDATE:
			desiredUsers = getDesiredUsersForExternalAccountUpdate(block);
			break;
		case EXTERNAL_ACCOUNT_UPDATE_SELF:
			desiredUsers = getDesiredUsersForExternalAccountUpdateSelf(block);
			// don't add a default, we want a warning if any cases are missing
		}
		
		if (desiredUsers == null)
			throw new IllegalStateException("Trying to update user block data for unhandled block type " + block.getBlockType());
		
		updateUserBlockDatas(block, desiredUsers);
	}
	
	private void stack(Block block, long activity) {
		if (disabled)
			return;

		if (block.getTimestampAsLong() < activity) {
			block.setTimestampAsLong(activity);
			updateUserBlockDatas(block);
		}
	}
	
	private void stack(final BlockType type, final Guid data1, final Guid data2, final long data3, final long activity) {
		if (disabled)
			return;
		
		// Updating the block timestamp is something we want to do as part of the enclosing transaction;
		// if the enclosing transaction is rolled back, the timestamp needs to be rolled back
		final Block block = getUpdatingTimestamp(type, data1, data2, data3, activity);
		if (block == null) {
			logger.warn("No block exists when stacking type={}, data1={}, data2={}, data3{}, migration needed or bug",
					    new Object[] { type, data1, data2, data3 });
			return;
		}

		// Now we need to create demand-create user block data objects and update the
		// cached user timestamps. updateUserBlockDatas(block) always safe to call
		// at any point without worrying about ordering. We queue it asynchronously
		// after commit, so we can do retries when demand-creating UserBlockData.
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				runner.runTaskRetryingOnConstraintViolation(new Runnable() {
					public void run() {
						Block attached = em.find(Block.class, block.getId());
						updateUserBlockDatas(attached);
					}
				});
			}
		});
	}

	private void stack(final BlockType type, final Guid data1, final Guid data2, final long activity) {
        stack(type, data1, data2, -1, activity);
	}
	
	private void click(BlockType type, Guid data1, Guid data2, long data3, User user, long clickTime) {
		if (disabled)
			return;
		
		UserBlockData ubd;
		try {
			ubd = queryUserBlockData(user, type, data1, data2, data3);
		} catch (NotFoundException e) {
			// for now assume this means we don't want to record clicks for the given
			// object, otherwise the ubd should already be created
			logger.debug("No UserBlockData for user {} block type {} data1 " + data1, user, type);
			return;
		}
		
		// if we weren't previously clicked on, then increment the count.
		// (FIXME is this a race or does it work due to transaction semantics? not sure)
		if (!ubd.isClicked())
			ubd.getBlock().setClickedCount(ubd.getBlock().getClickedCount() + 1);
		
		if (ubd.getClickedTimestampAsLong() < clickTime)
			ubd.setClickedTimestampAsLong(clickTime);
		
		// we automatically unignore anything you click on
		if (ubd.isIgnored())
			ubd.setIgnored(false);
		
		logger.debug("due to click, restacking block {} with new time {}", ubd.getBlock(), clickTime);
		// now update the timestamp in the block (if it's newer)
		// and update user caches for all users
		stack(ubd.getBlock(), clickTime);
	}
	
	public void onUserCreated(Guid userId) {
		createBlock(BlockType.MUSIC_PERSON, userId, null);
	}
	
	public void onExternalAccountCreated(Guid userId, ExternalAccountType type) {
		createBlock(BlockType.EXTERNAL_ACCOUNT_UPDATE, userId, null, type.ordinal());
		createBlock(BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF, userId, null, type.ordinal());
	}
	
	public void onGroupCreated(Guid groupId) {
		createBlock(BlockType.GROUP_CHAT, groupId, null);
	}
	
	public void onGroupMemberCreated(GroupMember member) {
		// Blocks only exist for group members which correspond to accounts in the
		// system. If the group member is (say) an email resource, and later joins
		// the system, when they join, we'll delete this GroupMember, add a new one 
		// for the Account and create a block for that GroupMember. 
		AccountClaim a = member.getMember().getAccountClaim();
		if (a != null) {
			createBlock(BlockType.GROUP_CHAT, member.getGroup().getGuid(), a.getOwner().getGuid());
		}
	}
	
	public void onPostCreated(Guid postId) {
		createBlock(BlockType.POST, postId, null);
	}
	
	// don't create or suspend transaction; we will manage our own for now (FIXME) 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackMusicPerson(final Guid userId, final long activity) {
		stack(BlockType.MUSIC_PERSON, userId, null, activity);
	}
	// don't create or suspend transaction; we will manage our own for now (FIXME) 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackGroupChat(final Guid groupId, final long activity) {
		stack(BlockType.GROUP_CHAT, groupId, null, activity);
	}

	// FIXME this is not right; it requires various rationalization with respect to PersonPostData, XMPP, and 
	// so forth, e.g. to work with world posts and be sure we never delete any ignored flags, clicked dates, etc.
	// but it's OK for messing around.
	
	// don't create or suspend transaction; we will manage our own for now (FIXME)	 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackPost(final Guid postId, final long activity) {
		stack(BlockType.POST, postId, null, activity);
	}
	
	// don't create or suspend transaction; we will manage our own for now (FIXME)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackGroupMember(GroupMember member, long activity) {
		
		// Note that GroupMember objects can be deleted (and recreated).
		// They can also be associated with invited addresses and not accounts.
		// The identity of the Block is thus tied to the groupId,userId pair
		// rather than the GroupMember.
		
		switch (member.getStatus()) {
		case ACTIVE:
		case FOLLOWER:
		case REMOVED:
			AccountClaim a = member.getMember().getAccountClaim();
			if (a != null) {
				stack(BlockType.GROUP_MEMBER, member.getGroup().getGuid(), a.getOwner().getGuid(), activity);
			}
			break;
		case INVITED:
		case INVITED_TO_FOLLOW:
		case NONMEMBER:
			// moves to these states don't create a new timestamp
			break;
			// don't add a default case, we want a warning if any are missing
		}
	}
	
	// don't create or suspend transaction; we will manage our own for now (FIXME)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackAccountUpdate(Guid userId, ExternalAccountType type, long activity) {
		stack(BlockType.EXTERNAL_ACCOUNT_UPDATE, userId, null, type.ordinal(), activity);
	}

	// don't create or suspend transaction; we will manage our own for now (FIXME)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackAccountUpdateSelf(Guid userId, ExternalAccountType type, long activity) {
		stack(BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF, userId, null, type.ordinal(), activity);
	}
	
	public void clickedPost(Post post, User user, long clickedTime) {
		click(BlockType.POST, post.getGuid(), null, -1, user, clickedTime);
	}
	
	private BlockView getBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws NotFoundException {
		UserViewpoint userview = null;
		if (viewpoint instanceof UserViewpoint)
			userview = (UserViewpoint) viewpoint;
		switch (block.getBlockType()) {
		case POST: {
			PostView postView = postingBoard.loadPost(SystemViewpoint.getInstance(), block.getData1AsGuid());
			List<PostMessage> recentMessages = postingBoard.getNewestPostMessages(postView.getPost(), PostBlockView.RECENT_MESSAGE_COUNT);
			return new PostBlockView(block, ubd, postView, recentMessages);
		}
		case GROUP_CHAT: {
			GroupView groupView = groupSystem.loadGroup(viewpoint, block.getData1AsGuid());
			List<GroupMessage> recentMessages = groupSystem.getNewestGroupMessages(groupView.getGroup(), GroupChatBlockView.RECENT_MESSAGE_COUNT);
			return new GroupChatBlockView(block, ubd, groupView, recentMessages);
		}
		case GROUP_MEMBER: {
			GroupView groupView = groupSystem.loadGroup(viewpoint, block.getData1AsGuid());
			User user = identitySpider.lookupUser(block.getData2AsGuid());
			PersonView memberView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.PRIMARY_RESOURCE);
			GroupMember member =  groupSystem.getGroupMember(viewpoint, groupView.getGroup(), user);
			
			return new GroupMemberBlockView(block, ubd, groupView, memberView, member.getStatus());
		}
		case MUSIC_PERSON: {
			User user = identitySpider.lookupUser(block.getData1AsGuid());
			PersonView userView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.PRIMARY_RESOURCE);
			List<TrackView> tracks = musicSystem.getLatestTrackViews(viewpoint, user, 1);
			return new MusicPersonBlockView(block, ubd, userView, tracks.get(0));
		}
		case EXTERNAL_ACCOUNT_UPDATE: {
			User user = identitySpider.lookupUser(block.getData1AsGuid());
			// TODO implement
			if (userview != null && userview.getViewer() == user)
				return null;
			return null;
		}
		case EXTERNAL_ACCOUNT_UPDATE_SELF: {	
			User user = identitySpider.lookupUser(block.getData1AsGuid());
			// TODO implement
			if (userview != null && (identitySpider.isViewerSystemOrFriendOf(viewpoint, user)))
				return null;
			return null;
		}
		}
		throw new RuntimeException("Unknown BlockType: " + block.getBlockType());
	}
	
	public BlockView loadBlock(Viewpoint viewpoint, UserBlockData ubd) throws NotFoundException {
		return getBlockView(viewpoint, ubd.getBlock(), ubd);
	}	
	
	public List<BlockView> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count) {
		// keep things sane (e.g. if count provided by an http method API caller)
		if (count > 50)
			count = 50;
		if (count < 1)
			throw new IllegalArgumentException("count must be >0 not " + count);
		
		long cached = getLastTimestamp(user.getGuid());
		if (cached >= 0 && cached <= lastTimestamp)
			return Collections.emptyList(); // nothing new
		
		logger.debug("getBlocks cache miss lastTimestamp {} cached {}", lastTimestamp, cached);
		
		// FIXME this is not exactly the sort order if the user is paging; we want to use ubd.ignoredDate in the sort if the 
		// user has ignored a block, instead of block.timestamp. However, EJBQL doesn't know how to do that.
		// maybe a native sql query or some other solution is required. For now what we'll do is 
		// return blocks in block order, and also pass to the client the ignoredDate.
		// Then, require the client to sort it out. This may well be right anyway.
		
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE " + 
				" ubd.user = :user AND ubd.deleted = 0 AND ubd.block = block AND block.timestamp >= :timestamp ORDER BY block.timestamp DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		q.setParameter("user", user);
		q.setParameter("timestamp", new Date(lastTimestamp));
		
		// If there is 1 block at the lastTimestamp, then the caller for sure already has that
		// block. If there are >1 blocks, then the caller might have only some of them.
		// If there's only 1 block then we need not return it, if there are >1 we return 
		// them all.
		// The reason we bother with this is that when we have a cached timestamp we return 
		// nothing if there's nothing newer than lastTimestamp, so we want to be consistent
		// and still return nothing if we did the db query.
		
		List<UserBlockData> list = TypeUtils.castList(UserBlockData.class, q.getResultList());
		List<BlockView> stack = new ArrayList<BlockView>();
		
		// Create BlockView objects for the blocks, implicitly performing access control
		for (UserBlockData ubd : list) {
			try {
				stack.add(getBlockView(viewpoint, ubd.getBlock(), ubd));
			} catch (NotFoundException e) {
				// Do nothing, we can't see this block
			}
		}		
		
		if (stack.isEmpty())
			return stack;
		
		long newestTimestamp = -1;
		int newestTimestampCount = 0;
		
		int countAtLastTimestamp = 0; 
		for (BlockView bv : stack) {
			long stamp = bv.getBlock().getTimestampAsLong();
			
			if (stamp < lastTimestamp) {
				// FIXME I think the problem here may be that the database only goes to seconds not milliseconds,
				// at least when doing comparisons
				boolean secondsMatch = ((cached / 1000) == (newestTimestamp / 1000));
				logger.error("Query returned block at wrong timestamp lastTimestamp {} block {}: match at seconds resolution: " + secondsMatch,
						lastTimestamp, bv.getBlock());
			}
			
			if (stamp == lastTimestamp)
				countAtLastTimestamp += 1;
			
			if (stamp > newestTimestamp) {
				newestTimestamp = stamp;
				newestTimestampCount = 1;
			} else if (stamp == newestTimestamp) {
				newestTimestampCount += 1;
			}
		}
		if (newestTimestamp < 0)
			throw new RuntimeException("had a block with negative timestamp? " + list);
		if (cached >= 0 && cached < newestTimestamp) {
			//	FIXME I think the problem here may be that the database only goes to seconds not milliseconds,
			// at least when doing comparisons
			boolean secondsMatch = ((cached / 1000) == (newestTimestamp / 1000));
			logger.error("Cached timestamp {} was somehow older than actual newest timestamp {}, match at seconds resolution: " + secondsMatch,
					cached, newestTimestamp);
		}
		
		// we only know the timestamp is globally newest if start is 0
		if (start == 0)
			saveLastTimestamp(user.getGuid(), newestTimestamp, newestTimestampCount);
		
		// remove any single blocks that have the requested stamp
		if (countAtLastTimestamp == 1) {
			Iterator<BlockView> i = stack.iterator();
			while (i.hasNext()) {
				BlockView bv = i.next();
				if (bv.getBlock().getTimestampAsLong() == lastTimestamp) {
					i.remove();
					break;
				}
			}
		}
		return stack;
	}
	
	public UserBlockData lookupUserBlockData(UserViewpoint viewpoint, Guid guid) throws NotFoundException {
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND block.id = :blockId AND ubd.user = :user");
		q.setParameter("blockId", guid.toString());
		q.setParameter("user", viewpoint.getViewer());
		try {
			return (UserBlockData) q.getSingleResult();
		} catch (NonUniqueResultException e) {
			throw new NotFoundException("multiple UserBlockData for this block");
		} catch (NoResultException e) {
			throw new NotFoundException("no UserBlockData for blockId " + guid);
		}
	}
	
	static private class CacheEntry {
		public long lastTimestamp;
		public int count;
		
		CacheEntry(long lastTimestamp, int count) {
			this.lastTimestamp = lastTimestamp;
			this.count = count;
		}
		
		@Override
		public String toString() {
			return "{time " + lastTimestamp + " count " + count + "}";
		}
	}
		
	// called whenever we save a new block timestamp, if it's the newest
	// timestamp we've seen for a given user then we save it as the 
	// newest timestamp for that user. We also keep a count of 
	// how many times a given timestamp was saved since we need to 
	// do a db query if >1 block has the same stamp.
	synchronized void updateLastTimestamp(Guid guid, long lastTimestamp) {
		CacheEntry entry = userCacheEntries.get(guid);
		if (entry == null) {
			entry = new CacheEntry(lastTimestamp, 1);
			userCacheEntries.put(guid, entry);
		} else {
			if (entry.lastTimestamp == lastTimestamp) {
				entry.count += 1;
			} else if (entry.lastTimestamp < lastTimestamp) {
				entry.lastTimestamp = lastTimestamp;
				entry.count = 1;
			}
		}
		//logger.debug("updating block timestamp for user {} to {}", guid, entry);
	}
	
	// called when we do a db query and discover the last timestamp and number of blocks
	// with said timestamp. This avoids doing the same db query again, at least 
	// until updateLastTimestamp saves a newer timestamp.
	synchronized void saveLastTimestamp(Guid guid, long lastTimestamp, int blockCount) {
		CacheEntry entry = userCacheEntries.get(guid);
		if (entry == null) {
			entry = new CacheEntry(lastTimestamp, blockCount);
			userCacheEntries.put(guid, entry);
		} else {
			if (entry.lastTimestamp == lastTimestamp) {
				entry.count = blockCount;
			} else if (entry.lastTimestamp < lastTimestamp) {
				entry.lastTimestamp = lastTimestamp;
				entry.count = blockCount;
			}
		}
		//logger.debug("saving block timestamp for user {} as {}", guid, entry);
	}
	
	// returns the newest timestamp for which only one block
	// exists, or -1 if nothing cached or if >1 block for the 
	// newest timestamp, i.e. returns -1 if we need to do a db
	// query
	synchronized long getLastTimestamp(Guid guid) {
		CacheEntry entry = userCacheEntries.get(guid);
		if (entry == null)
			return -1;
		else if (entry.count > 1)
			return -1;
		else
			return entry.lastTimestamp;
	}

	private static final String ELEMENT_NAME = "blocksChanged";
	private static final String NAMESPACE = CommonXmlWriter.NAMESPACE_BLOCKS;
	
	public void onEvent(BlockEvent event) {
		for (Guid guid : event.getAffectedUsers()) {
			updateLastTimestamp(guid, event.getStackTimestamp());
		}
		
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

	static private class GroupMigrationTask implements Runnable {
		private String groupId;
		
		GroupMigrationTask(String postId) {
			this.groupId = postId;
		}
		
		public void run() {
			Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
			stacker.migrateGroupChat(groupId);
			stacker.migrateGroupMembers(groupId);
		}
	}

	static private class Migration implements Runnable {
		@SuppressWarnings("unused")
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
		
		if (disabled)
			throw new RuntimeException("stacking disabled, can't migrate anything");
		
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
		
		runMigration(tasks);
	}
	
	public void migrateGroups() {
		if (disabled)
			throw new RuntimeException("stacking disabled, can't migrate anything");

		List<Runnable> tasks = new ArrayList<Runnable>();

		Query q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new GroupMigrationTask(id));

		runMigration(tasks);
	}
	
	public void migratePost(String postId) {
		logger.debug("    migrating post {}", postId);
		Post post = em.find(Post.class, postId);
		Block block = getOrCreateBlock(BlockType.POST, post.getGuid(), null);
		long activity = post.getPostDate().getTime();

		// we want to move PersonPostData info into UserBlockData
		// (this is obviously expensive as hell)
		List<UserBlockData> userDatas = queryUserBlockDatas(block);
		Map<User,UserBlockData> byUser = new HashMap<User,UserBlockData>();
		for (UserBlockData ubd : userDatas) {
			byUser.put(ubd.getUser(), ubd);
		}

		// There is a race condition if a UserBlockData is created for the Block by
		// some other thread, but that can only happen if the getOrCreateBlock() finds 
		// an existing Block but the UserBlockData for that Block are missing. If it 
		// does happen (indicating that some earlier version of the migration code was 
		// run, perhaps) then the retry-on-constraint-violation that we wrap migration
		// tasks in should task care of it.
		
		Set<PersonPostData> postDatas = post.getPersonPostData();
		for (PersonPostData ppd : postDatas) {
			Person p = ppd.getPerson();
			if (!(p instanceof User))
				continue;
			User user = (User) p;
			UserBlockData ubd = byUser.get(user);
			if (ubd == null) {
				// create a deleted UserBlockData to store the info from the ppd
				ubd = new UserBlockData(user, block);
				ubd.setDeleted(true);
				em.persist(ubd);
			}
			if (ubd.isIgnored()) {
				// keep the existing ignored info
			} else if (ppd.isIgnored()) {
				// there's no ignored date on ppd, so we use the latest block 
				// timestamp (the point of the ignored date is to freeze the block's 
				// stack location so this is perfect really)
				ubd.setIgnored(true);
				ubd.setIgnoredTimestampAsLong(block.getTimestampAsLong());
			} else {
				// neither one is ignored, nothing to migrate
			}
			
			// if ppd has a clicked date and ubd doesn't, copy in from ppd
			if (ubd.getClickedTimestampAsLong() < 0 && 
					ppd.getClickedDateAsLong() >= 0) {
				ubd.setClickedTimestampAsLong(ppd.getClickedDateAsLong());
			}
		}
		
		// now count the denormalized number of viewers. the old 
		// query of UserBlockData is fine since we only added deleted
		// ones.
		int clickedCount = 0;
		for (UserBlockData ubd : userDatas) {
			if (!ubd.isDeleted() && ubd.isClicked()) {
				clickedCount += 1;
				if (ubd.getClickedTimestampAsLong() > activity)
					activity = ubd.getClickedTimestampAsLong();
			}
		}
		if (block.getClickedCount() != clickedCount) {
			logger.debug("  fixing up clicked count from {} to {} for block " + block, block.getClickedCount(), clickedCount);
			block.setClickedCount(clickedCount);
		}
		
		List<PostMessage> messages = postingBoard.getNewestPostMessages(post, 1);
		if (messages.size() > 0) {
			PostMessage m = messages.get(0);
			long newestMessageTime = m.getTimestamp().getTime();
			if (newestMessageTime > activity)
				activity = newestMessageTime;
		}
		
		// This will update the block timestamp then asynchronously update the
		// cached user timestamps after this transaction commits. It would also 
		// create any  UserBlockData objects that didn't exist at that point, but 
		// they should have all been created above by migrating PersonPostData
		stack(block, activity);
	}
	
	public void migrateUser(String userId) {
		logger.debug("    migrating user {}", userId);
		User user = em.find(User.class, userId);
		getOrCreateBlock(BlockType.MUSIC_PERSON, user.getGuid(), null);
		long lastPlayTime = musicSystem.getLatestPlayTime(SystemViewpoint.getInstance(), user);
		stackMusicPerson(user.getGuid(), lastPlayTime);
	}
	
	public void migrateGroupChat(String groupId) {
		logger.debug("    migrating group chat for {}", groupId);
		Group group = em.find(Group.class, groupId);
		getOrCreateBlock(BlockType.GROUP_CHAT, group.getGuid(), null);
		List<GroupMessage> messages = groupSystem.getNewestGroupMessages(group, 1);
		if (messages.isEmpty())
			stackGroupChat(group.getGuid(), 0);
		else
			stackGroupChat(group.getGuid(), messages.get(0).getTimestamp().getTime());
	}
	
	public void migrateGroupMembers(String groupId) {
		logger.debug("    migrating group members for {}", groupId);
		Group group = em.find(Group.class, groupId);
		for (GroupMember member : group.getMembers()) {
			AccountClaim a = member.getMember().getAccountClaim();
			if (a != null) {
				getOrCreateBlock(BlockType.GROUP_CHAT, member.getGroup().getGuid(), a.getOwner().getGuid());
				// we set a timestamp of 0, since we have no way of knowing the right
				// timestamp, and we don't want to make a big pile of group member blocks 
				// at the top of the stack whenever we run a migration
				stackGroupMember(member, 0);
			}
		}
	}

}
