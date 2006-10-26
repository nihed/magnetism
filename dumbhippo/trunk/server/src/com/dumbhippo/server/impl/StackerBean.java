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
import com.dumbhippo.Pair;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.BlockEvent;
import com.dumbhippo.live.LiveEventListener;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupAccess;
import com.dumbhippo.persistence.GroupBlockData;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SimpleServiceMBean;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmppMessageSender;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.BlockView;
import com.dumbhippo.server.views.BlogBlockView;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.FacebookBlockView;
import com.dumbhippo.server.views.GroupChatBlockView;
import com.dumbhippo.server.views.GroupMemberBlockView;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.server.views.MusicPersonBlockView;
import com.dumbhippo.server.views.PersonMugshotView;
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
	
	@EJB
	private ExternalAccountSystem externalAccountSystem;
	
	@EJB
	private FeedSystem feedSystem;
	
	@EJB
	private FacebookSystem facebookSystem;
	
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
	
	private Block createBlock(BlockType type, Guid data1, Guid data2, long data3, boolean publicBlock) {
		Block block = new Block(type, data1, data2, data3, publicBlock);
		em.persist(block);
		
		return block;
	}
	
	@SuppressWarnings("unused")
	private Block createBlock(BlockType type, Guid data1, Guid data2, long data3) {
	    return createBlock(type, data1, data2, data3, type.isAlwaysPublic());
	}
	
	private Block createBlock(BlockType type, Guid data1, Guid data2, boolean publicBlockIfCreated) {
		return createBlock(type, data1, data2, -1, publicBlockIfCreated);
	}
	
	private Block createBlock(BlockType type, Guid data1, Guid data2) {
		return createBlock(type, data1, data2, -1, type.isAlwaysPublic());
	}
	
	private Block getOrCreateBlock(BlockType type, Guid data1, Guid data2, long data3, boolean publicBlockIfCreated) {
		try {
			return queryBlock(type, data1, data2, data3);
		} catch (NotFoundException e) {
			return createBlock(type, data1, data2, data3, publicBlockIfCreated);
		}
	}
	
	@SuppressWarnings("unused")
	private Block getOrCreateBlock(BlockType type, Guid data1, Guid data2, long data3) {
		return getOrCreateBlock(type, data1, data2, data3, type.isAlwaysPublic());
	}
	
	private Block getOrCreateBlock(BlockType type, Guid data1, Guid data2, boolean publicBlockIfCreated) {
		return getOrCreateBlock(type, data1, data2, -1, publicBlockIfCreated);
	}

	private Block getOrCreateBlock(BlockType type, Guid data1, Guid data2) {
		return getOrCreateBlock(type, data1, data2, -1, type.isAlwaysPublic());
	}
	
	private Block getUpdatingTimestamp(BlockType type, Guid data1, Guid data2, long data3, long activity) {
		Block block;
		try {
			logger.debug("will query for a block with type {}/{} data1: {}, data2: {}, data3: {}", new Object[]{type, type.ordinal(), data1, data2, data3});
			block = queryBlock(type, data1, data2, data3);
			logger.debug("found block {}", block);
		} catch (NotFoundException e) {
			logger.debug("no block found");
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
	
	private void updateUserBlockDatas(Block block, Set<User> desiredUsers, Guid participantId) {
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
				if (u.getGuid().equals(participantId))
					old.setParticipatedTimestamp(block.getTimestamp());
			} else {
				UserBlockData data = new UserBlockData(u, block, u.getGuid().equals(participantId));
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

	private Set<User> getUsersWhoCare(Block block) {
		return getUsersWhoCare(block, true);
	}
	
	// getUsersWhoCare should pretty much always include self, because we use
	// the blocks stacked for self when displaying them to others on the web stacker
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
	
		// we always want to include self in external account updates, because
		// this is how we will get the UserBlockData for the update with the right
		// participation timestamp, which we will display on the person's own stacker
		// when viewed by others
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
	
	private void updateUserBlockDatas(Block block, Guid participantId) {
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
		
		updateUserBlockDatas(block, desiredUsers, participantId);
	}
	
	// note this query includes ubd.deleted=1
	private List<GroupBlockData> queryGroupBlockDatas(Block block) {
		Query q = em.createQuery("SELECT gbd FROM GroupBlockData gbd WHERE gbd.block = :block");
		q.setParameter("block", block);
		return TypeUtils.castList(GroupBlockData.class, q.getResultList());
	}
	
	private void updateGroupBlockDatas(Block block, Set<Group> desiredGroups) {
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
			} else {
				GroupBlockData data = new GroupBlockData(g, block);
				em.persist(data);
				addCount += 1;
			}
		}
		// the rest of "existing" is users who no longer are in the desired set
		for (Group g : existing.keySet()) {
			GroupBlockData old = existing.get(g);
			if (!old.isDeleted())
				removeCount += 1;
			old.setDeleted(true);
		}
		
		logger.debug("block {}, {} total groups {} added {} removed {}", new Object[] { block, affectedGuids.size(), addCount, removeCount } );
	}

	private Set<Group> getGroupsWhoCare(Block block, boolean privateOnly) {
		User user;
		try {
			user = EJBUtil.lookupGuid(em, User.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		
		if (privateOnly)
			return groupSystem.findRawPrivateGroups(SystemViewpoint.getInstance(), user);
		else
			return groupSystem.findRawGroups(SystemViewpoint.getInstance(), user);
	}
	
	private Set<Group> getDesiredGroupsForMusicPerson(Block block) {
		if (block.getBlockType() != BlockType.MUSIC_PERSON)
			throw new IllegalArgumentException("wrong type block");
        
		// For music updates, we show in both public and private groups
		// where that user is a member - will this cause too much noise
		// in big public groups?
		return getGroupsWhoCare(block, false);
	}
	
	private Set<Group> getDesiredGroupsForGroupChat(Block block) {
		if (block.getBlockType() != BlockType.GROUP_CHAT)
			throw new IllegalArgumentException("wrong type block");
		Group group;
		try {
			group = EJBUtil.lookupGuid(em, Group.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		return Collections.singleton(group);
	}

	private Set<Group> getDesiredGroupsForPost(Block block) {
		if (block.getBlockType() != BlockType.POST)
			throw new IllegalArgumentException("wrong type block");

		Post post;
		try {
			post = EJBUtil.lookupGuid(em, Post.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		
		return post.getGroupRecipients();
	}

	private Set<Group> getDesiredGroupsForGroupMember(Block block) {
		if (block.getBlockType() != BlockType.GROUP_MEMBER)
			throw new IllegalArgumentException("wrong type block");
		
		Group group = em.find(Group.class, block.getData1AsGuid().toString());
		
		return Collections.singleton(group);
	}
	
	private Set<Group> getDesiredGroupsForExternalAccountUpdate(Block block) {
		if (block.getBlockType() != BlockType.EXTERNAL_ACCOUNT_UPDATE)
			throw new IllegalArgumentException("wrong type block");
	
		// As a heuristic, we only show external account updates in private
		// groups, because they likely will be small groups of friends or
		// family, who might actually care about your latest Flickr photos.
		return getGroupsWhoCare(block, true);
	}

	private Set<Group> getDesiredGroupsForExternalAccountUpdateSelf(Block block) {
		if (block.getBlockType() != BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF)
			throw new IllegalArgumentException("wrong type block");

		return Collections.emptySet();
	}
	
	private void updateGroupBlockDatas(Block block) {
		Set<Group> desiredGroups = null;
		switch (block.getBlockType()) {
		case POST:
			desiredGroups = getDesiredGroupsForPost(block);
			break;
		case GROUP_CHAT:
			desiredGroups = getDesiredGroupsForGroupChat(block);
			break;
		case MUSIC_PERSON:
			desiredGroups = getDesiredGroupsForMusicPerson(block);
			break;
		case GROUP_MEMBER:
			desiredGroups = getDesiredGroupsForGroupMember(block);
			break;
		case EXTERNAL_ACCOUNT_UPDATE:
			desiredGroups = getDesiredGroupsForExternalAccountUpdate(block);
			break;
		case EXTERNAL_ACCOUNT_UPDATE_SELF:
			desiredGroups = getDesiredGroupsForExternalAccountUpdateSelf(block);
			// don't add a default, we want a warning if any cases are missing
		}
		
		if (desiredGroups == null)
			throw new IllegalStateException("Trying to update user block data for unhandled block type " + block.getBlockType());
		
		updateGroupBlockDatas(block, desiredGroups);
	}
	
	private void stack(Block block, long activity) {
		if (disabled)
			return;

		if (block.getTimestampAsLong() < activity) {
			block.setTimestampAsLong(activity);
			updateUserBlockDatas(block, null);
			updateGroupBlockDatas(block);
		}
	}
	
	private void stack(final BlockType type, final Guid data1, final Guid data2, final long data3, final long activity, final Guid participantId) {
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

		// Now we need to create demand-create user/group block data objects and update the
		// cached user timestamps. update{User,Group{BlockDatas(block) are always safe to call
		// at any point without worrying about ordering. We queue the update asynchronously
		// after commit, so we can do retries when demand-creating {User,Group}BlockData.
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
				runner.runTaskRetryingOnConstraintViolation(new Runnable() {
					public void run() {
						Block attached = em.find(Block.class, block.getId());
						updateUserBlockDatas(attached, participantId);
						updateGroupBlockDatas(attached);
					}
				});
			}
		});
	}

	private void stack(final BlockType type, final Guid data1, final Guid data2, final long activity, final Guid participantId) {
        stack(type, data1, data2, -1, activity, participantId);
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
		User user = identitySpider.lookupUser(userId);
		Block block = createBlock(BlockType.MUSIC_PERSON, userId, null);
		updateMusicPersonPublicity(block, user);
	}
	
	public void onExternalAccountCreated(Guid userId, ExternalAccountType type) {
		createBlock(BlockType.EXTERNAL_ACCOUNT_UPDATE, userId, null, type.ordinal(), true);
		createBlock(BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF, userId, null, type.ordinal(), false);
	}
	
	public void onGroupCreated(Guid groupId, boolean publicGroup) {
		createBlock(BlockType.GROUP_CHAT, groupId, null, publicGroup);
	}
	
	public void onGroupMemberCreated(GroupMember member, boolean publicGroup) {
		// Blocks only exist for group members which correspond to accounts in the
		// system. If the group member is (say) an email resource, and later joins
		// the system, when they join, we'll delete this GroupMember, add a new one 
		// for the Account and create a block for that GroupMember. 
		AccountClaim a = member.getMember().getAccountClaim();
		if (a != null) {
			// This is getOrCreate because a GroupMember can be deleted and then we'll 
			// get onGroupMemberCreated again later for the same group/person if they rejoin
			getOrCreateBlock(BlockType.GROUP_MEMBER, member.getGroup().getGuid(), a.getOwner().getGuid(), publicGroup);
		}
	}
	
	public void onPostCreated(Guid postId, boolean publicPost) {
		createBlock(BlockType.POST, postId, null, publicPost);
	}
	
	private void updateMusicPersonPublicity(Block block, User user) {
		if (!user.getGuid().equals(block.getData1AsGuid()))
			throw new IllegalArgumentException("setMusicPersonPublicity takes the guid from the block");
		block.setPublicBlock(identitySpider.getMusicSharingEnabled(user, Enabled.AND_ACCOUNT_IS_ACTIVE));
	}
	
	private void updateMusicPersonPublicity(Account account) {
		Block block;
		try {
			block = queryBlock(BlockType.MUSIC_PERSON, account.getOwner().getGuid(), null, -1);
		} catch (NotFoundException e) {
			return;
		}
		updateMusicPersonPublicity(block, account.getOwner());
	}
	
	public void onAccountDisabledToggled(Account account) {
		updateMusicPersonPublicity(account);
	}
	
	public void onMusicSharingToggled(Account account) {
		updateMusicPersonPublicity(account);
	}
	
	// don't create or suspend transaction; we will manage our own for now (FIXME) 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackMusicPerson(final Guid userId, final long activity) {
		stack(BlockType.MUSIC_PERSON, userId, null, activity, userId);
	}
	// don't create or suspend transaction; we will manage our own for now (FIXME) 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackGroupChat(final Guid groupId, final long activity, final Guid participantId) {
		stack(BlockType.GROUP_CHAT, groupId, null, activity, participantId);
	}

	// FIXME this is not right; it requires various rationalization with respect to PersonPostData, XMPP, and 
	// so forth, e.g. to work with world posts and be sure we never delete any ignored flags, clicked dates, etc.
	// but it's OK for messing around.
	
	// don't create or suspend transaction; we will manage our own for now (FIXME)	 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackPost(final Guid postId, final long activity, final Guid participantId) {
		stack(BlockType.POST, postId, null, activity, participantId);
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
		case INVITED:
		case INVITED_TO_FOLLOW:			
			AccountClaim a = member.getMember().getAccountClaim();
			if (a != null) {
				stack(BlockType.GROUP_MEMBER, member.getGroup().getGuid(), a.getOwner().getGuid(), activity, a.getOwner().getGuid());
			}
			break;
		case NONMEMBER:
			// moves to these states don't create a new timestamp
			break;
			// don't add a default case, we want a warning if any are missing
		}
	}
	
	// don't create or suspend transaction; we will manage our own for now (FIXME)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackAccountUpdate(Guid userId, ExternalAccountType type, long activity) {
		stack(BlockType.EXTERNAL_ACCOUNT_UPDATE, userId, null, type.ordinal(), activity, userId);
		// we always re-stack the external account update for self when we stack the external account update
		// for others, because right now the external account update for self always includes whatever we 
		// show to others; this allows us to filter out one or the other block type depending on 
		// who is looking at the person's stacker
		stack(BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF, userId, null, type.ordinal(), activity, userId);
	}

	// don't create or suspend transaction; we will manage our own for now (FIXME)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackAccountUpdateSelf(Guid userId, ExternalAccountType type, long activity) {
		stack(BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF, userId, null, type.ordinal(), activity, userId);
	}
	
	public void clickedPost(Post post, User user, long clickedTime) {
		click(BlockType.POST, post.getGuid(), null, -1, user, clickedTime);
	}
	
	// usually for access controls we just use NotFoundException, but in this case 
	// populateBlockContents isn't just a "lookup function" and doesn't naturally 
	// have anything to "not find" - also it calls lots of 
	// other things that throw NotFoundException, some of which should not be 
	// passed out as they don't indicate that we can't see the block
	class BlockContentsNotVisibleException extends Exception {
		private static final long serialVersionUID = 1L;

		public BlockContentsNotVisibleException(String message, Throwable cause) {
			super(message, cause);
		}
		
		public BlockContentsNotVisibleException(String message) {
			super(message);
		}
	}
	
	private BlockView prepareExternalAccountBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws NotFoundException {
		if ((block.getBlockType() != BlockType.EXTERNAL_ACCOUNT_UPDATE) && 
				(block.getBlockType() != BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF)) {
				throw new RuntimeException("Unexpected block type in prepareExternalAccountBlockView: " + block.getBlockType());			
		}

		long accountType = block.getData3();
		if (accountType == ExternalAccountType.BLOG.ordinal()) {			
			return new BlogBlockView(viewpoint, block, ubd);
		} else if (accountType == ExternalAccountType.FACEBOOK.ordinal()) {
			return new FacebookBlockView(viewpoint, block, ubd);
		} else {
			throw new NotFoundException("Unexpected external account type: " + accountType);
		}		
		
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
	private BlockView prepareBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws NotFoundException {
		UserViewpoint userview = null;
		if (viewpoint instanceof UserViewpoint)
			userview = (UserViewpoint) viewpoint;
		
		BlockView blockView = null;
		switch (block.getBlockType()) {
			case POST: {
			    blockView = new PostBlockView(viewpoint, block, ubd);
			    break;
			}
			case GROUP_CHAT: {
				blockView = new GroupChatBlockView(viewpoint, block, ubd);
			    break;
			}
			case GROUP_MEMBER: {
				blockView = new GroupMemberBlockView(viewpoint, block, ubd);
			    break;
			}
			case MUSIC_PERSON: {
				blockView = new MusicPersonBlockView(viewpoint, block, ubd);
			    break;
			}
			case EXTERNAL_ACCOUNT_UPDATE: {
	            blockView = prepareExternalAccountBlockView(viewpoint, block, ubd);   
			    break;
			}
			case EXTERNAL_ACCOUNT_UPDATE_SELF: {	
				User user = identitySpider.lookupUser(block.getData1AsGuid());
				if (userview == null || !userview.getViewer().equals(user)) {
					throw new NotFoundException("Trying to view an external account update for self from a different viewpoint: " + userview);
				}
				blockView = prepareExternalAccountBlockView(viewpoint, block, ubd);  	
			    break;
			}
			default : {
				throw new RuntimeException("Unexpected block type in prepareBlockView: " + block.getBlockType());
			}
		}

		// If a block isn't flagged as always public, then we need to do access control
		// here. We do that by trying to populate the block, and we see if that throws
		// an exception.
		//
		// FIXME: populating a block view is pontentially much more expensive than
		//  what we need to check visibility. For example, when we populate a PostBlockView,
		/// we do a database query to get chat messages for that post. I think we need to go 
		//  to a system where have:
		//
		//  - Skeletal block views with no entities other than the Block / UserBlockData
		//  - Views with some information filled in (e.g., the Post for a PostBlostView)
		//  - Fully populated blocks
		//
		// So, this call here should be 'checkVisibility(blockView) throws BlockContentsNotVisibleException
		// that may *as a side effect* fill in some fields of the block view, but isn't guaranteed to fully populate
		// the block.
		//
	    if (!block.isPublicBlock()) {
			try {
				populateBlockView(blockView);
			} catch (BlockContentsNotVisibleException e) {
				throw new NotFoundException("Contents of the block are not visible", e);
			}
	    }
	    
	    return blockView;
	} 
	
	// Populating the block view fills in all the details that were skipped at
	//   the prepare stage and makes it ready for viewing by the user.
	private void populateBlockView(BlockView blockView) throws BlockContentsNotVisibleException {
		Viewpoint viewpoint = blockView.getViewpoint();
		Block block = blockView.getBlock();

		if (blockView instanceof PostBlockView) {
			PostBlockView postBlockView = (PostBlockView)blockView;
		    PostView postView;
			try {
				postView = postingBoard.loadPost(viewpoint, block.getData1AsGuid());
			} catch (NotFoundException e) {
				throw new BlockContentsNotVisibleException("Post for the block wasn't visible", e);
			}
		    List<ChatMessageView> recentMessages = postingBoard.viewPostMessages(
		        postingBoard.getNewestPostMessages(postView.getPost(), PostBlockView.RECENT_MESSAGE_COUNT),
				viewpoint);
		    postBlockView.setPostView(postView);
		    postBlockView.setRecentMessages(recentMessages);
		    postBlockView.setPopulated(true);
		} else if (blockView instanceof GroupChatBlockView) {
			GroupChatBlockView groupChatBlockView = (GroupChatBlockView)blockView;
			GroupView groupView;
			try {
				groupView = groupSystem.loadGroup(viewpoint, block.getData1AsGuid());
			} catch (NotFoundException e) {
				throw new BlockContentsNotVisibleException("Group for the block is not visible", e);
			}
			List<ChatMessageView> recentMessages = groupSystem.viewGroupMessages(
					groupSystem.getNewestGroupMessages(groupView.getGroup(), GroupChatBlockView.RECENT_MESSAGE_COUNT),
					viewpoint);
			groupChatBlockView.setGroupView(groupView);
			groupChatBlockView.setRecentMessages(recentMessages);
			groupChatBlockView.setPopulated(true);
		} else if (blockView instanceof GroupMemberBlockView) {
			GroupMemberBlockView groupMemberBlockView = (GroupMemberBlockView)blockView;
			GroupView groupView;
			try {
				groupView = groupSystem.loadGroup(viewpoint, block.getData1AsGuid());
			} catch (NotFoundException e) {
				throw new BlockContentsNotVisibleException("Group for the block is not visible", e);
			}
			User user = identitySpider.lookupUser(block.getData2AsGuid());
			PersonView memberView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.PRIMARY_RESOURCE);
			GroupMember member;
			try {
				member = groupSystem.getGroupMember(viewpoint, groupView.getGroup(), user);
			} catch (NotFoundException e) {
				// This could mean the group isn't visible normally, but since we already 
				// did loadGroup above, it should not. Instead, it probably means someone
				// was a follower and we stacked a block, then they removed themselves
				// so now they have no GroupMember.
				member = null;
			}
			groupMemberBlockView.setGroupView(groupView);
			groupMemberBlockView.setMemberView(memberView);
			groupMemberBlockView.setStatus(member != null ? member.getStatus() : MembershipStatus.NONMEMBER);
			if (member != null)
				groupMemberBlockView.setAdders(personViewer.viewUsers(viewpoint, member.getAdders()));
			groupMemberBlockView.setPopulated(true);
		} else if (blockView instanceof MusicPersonBlockView) {
			MusicPersonBlockView musicPersonBlockView = (MusicPersonBlockView)blockView;
			User user = identitySpider.lookupUser(block.getData1AsGuid());
			PersonView userView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.PRIMARY_RESOURCE);
			List<TrackView> tracks = musicSystem.getLatestTrackViews(viewpoint, user, 5);
			if (tracks.isEmpty()) {
				throw new BlockContentsNotVisibleException("No tracks for this person are visible");
			}
			
			userView.setTrackHistory(tracks);
			
			musicPersonBlockView.setUserView(userView);
			musicPersonBlockView.setPopulated(true);
		} else if (blockView instanceof BlogBlockView) {
		    BlogBlockView blogBlockView = (BlogBlockView)blockView;
			User user = identitySpider.lookupUser(block.getData1AsGuid());
			// TODO: check what extras we need to request here
			PersonView userView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.ALL_RESOURCES);
			ExternalAccount blogAccount;
			try {
				blogAccount = externalAccountSystem.lookupExternalAccount(viewpoint, user, ExternalAccountType.BLOG);
			} catch (NotFoundException e) {
				throw new BlockContentsNotVisibleException("external blog account for block not visible", e);
			}  
		    FeedEntry lastEntry = feedSystem.getLastEntry(blogAccount.getFeed());
		    blogBlockView.setUserView(userView);
			blogBlockView.setEntry(lastEntry);
			blogBlockView.setPopulated(true);
		} else if (blockView instanceof FacebookBlockView) {
			FacebookBlockView facebookBlockView = (FacebookBlockView)blockView;
			User user = identitySpider.lookupUser(block.getData1AsGuid());
			// TODO: check what extras we need to request here
			PersonView userView = personViewer.getPersonView(viewpoint, user, PersonViewExtra.ALL_RESOURCES);
			FacebookAccount facebookAccount;
			try {
				facebookAccount = facebookSystem.lookupFacebookAccount(viewpoint, user);
			} catch (NotFoundException e) {
				throw new BlockContentsNotVisibleException("external facebook account for block not visible", e);
			}
			int eventsToRequestCount = 3;
			if (!facebookAccount.isSessionKeyValid() && viewpoint.isOfUser(facebookAccount.getExternalAccount().getAccount().getOwner())) {
			    eventsToRequestCount = 2;
			}
			List<FacebookEvent> facebookEvents = facebookSystem.getLatestEvents(viewpoint, facebookAccount, eventsToRequestCount);
			facebookBlockView.setUserView(userView);
			facebookBlockView.setFacebookEvents(facebookEvents);
			facebookBlockView.setPopulated(true);
		} else {
			throw new RuntimeException("Unknown type of block view in populateBlockView(): " + blockView.getClass().getName());
		}
	}
	
	public BlockView getBlockView(Viewpoint viewpoint, Block block, UserBlockData ubd) throws NotFoundException {
	    BlockView blockView = prepareBlockView(viewpoint, block, ubd);
	    if (!blockView.isPopulated()) {
			try {
				populateBlockView(blockView);
			} catch (BlockContentsNotVisibleException e) {
				throw new NotFoundException("Can't see this block", e);
			}
	    }
	    return blockView;
	}
	
	public BlockView loadBlock(Viewpoint viewpoint, UserBlockData ubd) throws NotFoundException {	
		return getBlockView(viewpoint, ubd.getBlock(), ubd);
	}	
	
	private List<UserBlockData> getBlocks(Viewpoint viewpoint, User user, boolean participantOnly, int start, int count) {
		String participatedClause = "";
		String orderBy = "block.timestamp";
		if (participantOnly) {
			participatedClause = " AND ubd.participatedTimestamp != NULL ";
			orderBy = "ubd.participatedTimestamp";
		}
				
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block " + 
				                 " WHERE ubd.user = :user AND ubd.deleted = 0 AND ubd.block = block " 
				                 + participatedClause +
				                 " AND (block.data1 != :userGuid OR block.blockType != :type)  " +
				                 " ORDER BY " + orderBy + " DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		q.setParameter("user", user);
		q.setParameter("userGuid", user.getGuid().toString());
		if (viewpoint.isOfUser(user)) 
			q.setParameter("type", BlockType.EXTERNAL_ACCOUNT_UPDATE);
		else
			q.setParameter("type", BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF);
		
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
				} catch (NotFoundException e) {
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
		
		// normally, we should not have any block views to remove, but it's better to remove a 
		// block view that was not populated than to let a null pointer exception happen down the line
		Set<BlockView> blockViewsToRemove = new HashSet<BlockView>();
		for (BlockView blockView : blockViewsToReturn) {
		    if (!blockView.isPopulated()) {
				try {
					populateBlockView(blockView);
				} catch (BlockContentsNotVisibleException e) {
					blockViewsToRemove.add(blockView);
					logger.error("Could not populate a block view for a public block {}", blockView.getBlock(), e);				
				}
		    }
		}
		blockViewsToReturn.removeAll(blockViewsToRemove);
		
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
				for (UserBlockData ubd : getBlocks(viewpoint, user, participantOnly, start, count)) {
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
                " AND ubd.participatedTimestamp != NULL " +
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
                " AND ubd.participatedTimestamp != NULL " +
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
		// keep things sane (e.g. if count provided by an http method API caller)
		if (count > 50)
			count = 50;
		if (count < 1)
			throw new IllegalArgumentException("count must be >0 not " + count);
		
		logger.debug("user is {}", user);
		long cached = getLastTimestamp(user.getGuid());
		if (cached >= 0 && cached <= lastTimestamp)
			return Collections.emptyList(); // nothing new
		
		logger.debug("getBlocks cache miss lastTimestamp {} cached {}", lastTimestamp, cached);
		
		// FIXME this is not exactly the sort order if the user is paging; we want to use ubd.ignoredDate in the sort if the 
		// user has ignored a block, instead of block.timestamp. However, EJBQL doesn't know how to do that.
		// maybe a native sql query or some other solution is required. For now what we'll do is 
		// return blocks in block order, and also pass to the client the ignoredDate.
		// Then, require the client to sort it out. This may well be right anyway.
		
		String participatedClause = "";
		String orderBy = "block.timestamp";
		if (participantOnly) {
			participatedClause = " AND ubd.participatedTimestamp != NULL ";
			orderBy = "ubd.participatedTimestamp";
		}
			
		Query q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block " + 
				                 " WHERE ubd.user = :user AND ubd.deleted = 0 AND ubd.block = block " 
				                 + participatedClause +
				                 " AND block.timestamp >= :timestamp " +
				                 " AND (block.data1 != :userGuid OR block.blockType != :type)  " +
				                 " ORDER BY " + orderBy + " DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		q.setParameter("user", user);
		q.setParameter("timestamp", new Date(lastTimestamp));
		q.setParameter("userGuid", user.getGuid().toString());
		if (viewpoint.isOfUser(user)) 
			q.setParameter("type", BlockType.EXTERNAL_ACCOUNT_UPDATE);
		else
			q.setParameter("type", BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF);
		
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
	
	private List<GroupBlockData> getBlocks(Viewpoint viewpoint, Group group, int start, int count) {
		Query q = em.createQuery("SELECT gbd FROM GroupBlockData gbd, Block block " + 
				                 " WHERE gbd.group = :group AND gbd.deleted = 0 AND gbd.block = block " + 
				                 " ORDER BY block.timestamp DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		q.setParameter("group", group);
		
		return TypeUtils.castList(GroupBlockData.class, q.getResultList());
	}
	
	public void pageStack(final Viewpoint viewpoint, final Group group, Pageable<BlockView> pageable) {
		
		logger.debug("getting stack for group {}", group);

		// There may be a few exceptions, but generally if you can see a group page at all
		// you should be able to see all the blocks for the group
       	int expectedHitFactor = 2;
       	
		pageStack(viewpoint, new BlockSource() {
			public List<Pair<Block, UserBlockData>> get(int start, int count) {
				List<Pair<Block, UserBlockData>> results = new ArrayList<Pair<Block, UserBlockData>>();
				for (GroupBlockData gbd : getBlocks(viewpoint, group, start, count)) {
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
        " AND block.blockType != " + BlockType.EXTERNAL_ACCOUNT_UPDATE.ordinal() + 
        " AND block.blockType != " + BlockType.MUSIC_PERSON.ordinal(); 
	
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
                " ORDER BY block.timestamp DESC");
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
                " ORDER BY block.timestamp DESC");
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
	
	public void setBlockHushed(UserBlockData userBlockData, boolean hushed) {
 		if (hushed != userBlockData.isIgnored()) {
	 		userBlockData.setIgnored(hushed);
	 		if (hushed)
	 			userBlockData.setIgnoredTimestampAsLong(userBlockData.getBlock().getTimestampAsLong());
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
		
		tasks.addAll(generateParticipationMigrationTasks());
		
		runMigration(tasks);
	}
	
	public void migrateParticipation() {
		if (disabled)
			throw new RuntimeException("stacking disabled, can't migrate anything");
		
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
				           " WHERE block.blockType = :typeMusicPerson" +
				           " OR block.blockType = :typeExtAccount" +
				           " OR block.blockType = :typeExtAccountSelf");
		q.setParameter("typeMusicPerson", BlockType.MUSIC_PERSON);
		q.setParameter("typeExtAccount", BlockType.EXTERNAL_ACCOUNT_UPDATE);
		q.setParameter("typeExtAccountSelf", BlockType.EXTERNAL_ACCOUNT_UPDATE_SELF);
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new BlockParticipationMigrationTask(id));

		q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList()))
			tasks.add(new GroupParticipationMigrationTask(id));
		
		return tasks;
	}
	
	public void migrateGroups() {
		if (disabled)
			throw new RuntimeException("stacking disabled, can't migrate anything");

		List<Runnable> tasks = new ArrayList<Runnable>();

		Query q = em.createQuery("SELECT group.id FROM Group group");
		for (String id : TypeUtils.castList(String.class, q.getResultList())) {
			tasks.add(new GroupMigrationTask(id));
			tasks.add(new GroupParticipationMigrationTask(id));	
		}

		runMigration(tasks);
	}
	
	// migratePostParticipation should also be called to do a complete migration of post participation
	public void migratePost(String postId) {
		logger.debug("    migrating post {}", postId);
		Post post = em.find(Post.class, postId);
		Block block = getOrCreateBlock(BlockType.POST, post.getGuid(), null, post.isPublic());
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
				long participatedTimestamp = -1;
				if (user.equals(post.getPoster()))
					participatedTimestamp = post.getPostDate().getTime();
				ubd = new UserBlockData(user, block, participatedTimestamp);
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
	
	public void migratePostParticipation(String postId) {
		// set the participatedTimestamp for the corresponding userBlockData to be the time
		// the post was sent or the chat message on the post was sent 
		logger.debug("    migrating post participation {}", postId);
		Post post = em.find(Post.class, postId);
		Block block;
		try {
		    block = queryBlock(BlockType.POST, post.getGuid(), null, -1);		
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
		getOrCreateBlock(BlockType.MUSIC_PERSON, user.getGuid(), null);
		long lastPlayTime = musicSystem.getLatestPlayTime(SystemViewpoint.getInstance(), user);
		stackMusicPerson(user.getGuid(), lastPlayTime);
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
		getOrCreateBlock(BlockType.GROUP_CHAT, group.getGuid(), null, group.isPublic());
		List<GroupMessage> messages = groupSystem.getNewestGroupMessages(group, 1);
		if (messages.isEmpty())
			stackGroupChat(group.getGuid(), 0, null);
		else {
			stackGroupChat(group.getGuid(), messages.get(0).getTimestamp().getTime(), messages.get(0).getFromUser().getGuid());
		}
	}
	
	public void migrateGroupChatParticipation(String groupId) {
		// set the participatedTimestamp for the corresponding userBlockData to be the time when 
		// the group chat message was sent 
		logger.debug("    migrating group chat participation for {}", groupId);
		Group group = em.find(Group.class, groupId);		
		Block block;
		try {
		    block = queryBlock(BlockType.GROUP_CHAT, group.getGuid(), null, -1);		
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
				getOrCreateBlock(BlockType.GROUP_MEMBER, member.getGroup().getGuid(), a.getOwner().getGuid(), group.isPublic());
				// we set a timestamp of 0, since we have no way of knowing the right
				// timestamp, and we don't want to make a big pile of group member blocks 
				// at the top of the stack whenever we run a migration
				stackGroupMember(member, 0);
			}
		}
	}
	
	public void migrateGroupBlockData(String blockId) {
		logger.debug("    migrating group block data for {}", blockId);
		
		Block block = em.find(Block.class, blockId);
		updateGroupBlockDatas(block);
	}
}
