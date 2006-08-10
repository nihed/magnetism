package com.dumbhippo.server.impl;

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

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonPostData;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class StackerBean implements Stacker {

	static final private boolean disabled = false;
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackerBean.class);
	
	static private UserCache userCache;
	
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
	
	static synchronized private UserCache getUserCache() {
		if (userCache == null) {
			userCache = new UserCache();
		}
		return userCache;
	}

	private Block queryBlock(BlockType type, Guid data1, long data2) throws NotFoundException {
		Query q;
		if (data1 != null && data2 >= 0) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data1=:data1 AND block.data2=:data2");
			q.setParameter("data1", data1.toString());
			q.setParameter("data2", data2);
		} else if (data1 != null) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data1=:data1 AND block.data2=-1");
			q.setParameter("data1", data1.toString());
		} else if (data2 >= 0) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data2=:data2 AND block.data1 IS NULL");
			q.setParameter("data2", data2);
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block type " + type);
		}
		q.setParameter("type", type.ordinal());
		try {
			return (Block) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("no block with type " + type + " data1 " + data1 + " data2 " + data2, e);
		}
	}

	private UserBlockData queryUserBlockData(User user, BlockType type, Guid data1, long data2) throws NotFoundException {
		Query q;
		if (data1 != null && data2 >= 0) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data1=:data1 AND block.data2=:data2");
			q.setParameter("data1", data1.toString());
			q.setParameter("data2", data2);
		} else if (data1 != null) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data1=:data1 AND block.data2=-1");
			q.setParameter("data1", data1.toString());
		} else if (data2 >= 0) {
			q = em.createQuery("SELECT ubd FROM UserBlockData ubd, Block block WHERE ubd.block = block AND ubd.user = :user AND block.blockType=:type AND block.data2=:data2 AND block.data1 IS NULL");
			q.setParameter("data2", data2);
		} else {
			throw new IllegalArgumentException("must provide either data1 or data2 in query for block type " + type);
		}
		q.setParameter("user", user);
		q.setParameter("type", type.ordinal());
		try {
			return (UserBlockData) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException("no UserBlockData with type " + type + " data1 " + data1 + " data2 " + data2 + " user " + user, e);
		}
	}
	
	// this doesn't retry on constraint violation since we do that for a larger transaction,
	// see below
	private Block getOrCreateUpdatingTimestamp(BlockType type, Guid data1, long data2, long activity) {
		Block block;
		try {
			block = queryBlock(type, data1, data2);
		} catch (NotFoundException e) {
			logger.debug("creating new block type {} data1 {} data2 " + data2, type, data1);
			block = new Block(type, data1, data2);
			em.persist(block);
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
	
	
	private static class TimestampCacheTask implements Runnable {
		private long newTimestamp;
		private Set<Guid> needNewTimestamp;
		private Set<Guid> needClearTimestamp;
		
		TimestampCacheTask(long newTimestamp) {
			this.newTimestamp = newTimestamp;
		}
		
		void addNeedsNewTimestamp(Guid guid) {
			if (needNewTimestamp == null)
				needNewTimestamp = new HashSet<Guid>();
			needNewTimestamp.add(guid);
		}
		
		void addNeedsClearTimestamp(Guid guid) {
			if (needClearTimestamp == null)
				needClearTimestamp = new HashSet<Guid>();
			needClearTimestamp.add(guid);
		}
		
		public void run() {
			UserCache cache = getUserCache();
			for (Guid g : needNewTimestamp) {
				cache.updateLastTimestamp(g, newTimestamp);
			}
			for (Guid g : needClearTimestamp) {
				cache.clearLastTimestamp(g);
			}
		}
	}
	
	private void updateUserBlockDatas(Block block, Set<User> desiredUsers) {
		TimestampCacheTask cacheTask = new TimestampCacheTask(block.getTimestampAsLong());
		
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
			cacheTask.addNeedsNewTimestamp(u.getGuid());
			
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
			cacheTask.addNeedsClearTimestamp(u.getGuid());
			
			UserBlockData old = existing.get(u);
			old.setDeleted(true);
		}
		
		runner.runTaskOnTransactionCommit(cacheTask);		
	}

	private Set<User> getDesiredUsersForMusicPerson(Block block) {
		if (block.getBlockType() != BlockType.MUSIC_PERSON)
			throw new IllegalArgumentException("wrong type block");
		User user;
		try {
			user = EJBUtil.lookupGuid(em, User.class, block.getData1AsGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Set<User> peopleWhoCare = identitySpider.getUsersWhoHaveUserAsContact(SystemViewpoint.getInstance(), user);
		peopleWhoCare.add(user); // FIXME include ourselves, this is probably a debug-only thing
		return peopleWhoCare;
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
	
	private void stack(final BlockType type, final Guid data1, final long data2, final long activity) {
		if (disabled)
			return;
		
		// FIXME really we want to retry the whole operation we're part of, not just the block stacking,
		// but there's no current infrastructure for that and the constraint violation creating 
		// user block datas would probably happen plenty in practice for now
		runner.runTaskRetryingOnConstraintViolation(new Runnable() {
			public void run() {
				Block block = getOrCreateUpdatingTimestamp(type, data1, data2, activity);

				updateUserBlockDatas(block);
			}
		});
	}
	
	private void click(BlockType type, Guid data1, long data2, User user, long clickTime) {
		if (disabled)
			return;
		
		UserBlockData ubd;
		try {
			ubd = queryUserBlockData(user, type, data1, data2);
		} catch (NotFoundException e) {
			// for now assume this means we don't want to record clicks for the given
			// object, otherwise the ubd should already be created 
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
		
		// now update the timestamp in the block (if it's newer)
		// and update user caches for all users
		stack(ubd.getBlock(), clickTime);
	}
	
	// don't create or suspend transaction; we will manage our own for now (FIXME) 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackMusicPerson(final Guid userId, final long activity) {
		stack(BlockType.MUSIC_PERSON, userId, -1, activity);
	}
	// don't create or suspend transaction; we will manage our own for now (FIXME) 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackGroupChat(final Guid groupId, final long activity) {
		stack(BlockType.GROUP_CHAT, groupId, -1, activity);
	}

	// FIXME this is not right; it requires various rationalization with respect to PersonPostData, XMPP, and 
	// so forth, e.g. to work with world posts and be sure we never delete any ignored flags, clicked dates, etc.
	// but it's OK for messing around.
	
	// don't create or suspend transaction; we will manage our own for now (FIXME)	 
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void stackPost(final Guid postId, final long activity) {
		stack(BlockType.POST, postId, -1, activity);
	}
	
	public void clickedPost(Post post, User user, long clickedTime) {
		click(BlockType.POST, post.getGuid(), -1, user, clickedTime);
	}
	
	public List<UserBlockData> getStack(Viewpoint viewpoint, User user, long lastTimestamp, int start, int count) {
		if (!(viewpoint.isOfUser(user) || viewpoint instanceof SystemViewpoint))
			throw new RuntimeException("Supporting non-self viewpoints is hard here since you need to check permissions on the posts, etc.");
		
		// keep things sane (e.g. if count provided by an http method API caller)
		if (count > 50)
			count = 50;
		if (count < 1)
			throw new IllegalArgumentException("count must be >0 not " + count);
		
		UserCache cache = getUserCache();
		
		long cached = cache.getLastTimestamp(user.getGuid());
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
		if (list.isEmpty())
			return list;
		
		long newestTimestamp = -1;
		int newestTimestampCount = 0;
		
		int countAtLastTimestamp = 0; 
		for (UserBlockData ubd : list) {
			long stamp = ubd.getBlock().getTimestampAsLong();
			
			if (stamp < lastTimestamp)
				logger.error("Query returned block at wrong timestamp lastTimestamp {} block {}", lastTimestamp, ubd.getBlock());
			
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
			logger.error("Cached timestamp {} was somehow older than actual newest timestamp {}",
					cached, newestTimestamp);
		}
		
		// we only know the timestamp is globally newest if start is 0
		if (start == 0)
			cache.saveLastTimestamp(user.getGuid(), newestTimestamp, newestTimestampCount);
		
		// remove any single blocks that have the requested stamp
		if (countAtLastTimestamp == 1) {
			Iterator<UserBlockData> i = list.iterator();
			while (i.hasNext()) {
				UserBlockData ubd = i.next();
				if (ubd.getBlock().getTimestampAsLong() == lastTimestamp) {
					i.remove();
					break;
				}
			}
		}
		return list;
	}
	
	/** 
	 * Tracks the newest block timestamp for a given user, which allows us to avoid
	 * querying the db if someone asks for blocks at or after this cached timestamp.
	 * 
	 * @author Havoc Pennington
	 *
	 */
	static private class UserCache {
		@SuppressWarnings("unused")
		static private final Logger logger = GlobalSetup.getLogger(UserCache.class);		
		
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
		
		private Map<Guid,CacheEntry> entries;
		
		UserCache() {
			entries = new HashMap<Guid,CacheEntry>();
		}
		
		// called if e.g. we remove a block from a user's list, and thus
		// no longer know the saved last timestamp is truly the latest one
		synchronized void clearLastTimestamp(Guid guid) {
			//logger.debug("clearing last block timestamp for user {}", guid);
			entries.remove(guid);
		}
		
		// called whenever we save a new block timestamp, if it's the newest
		// timestamp we've seen for a given user then we save it as the 
		// newest timestamp for that user. We also keep a count of 
		// how many times a given timestamp was saved since we need to 
		// do a db query if >1 block has the same stamp.
		synchronized void updateLastTimestamp(Guid guid, long lastTimestamp) {
			CacheEntry entry = entries.get(guid);
			if (entry == null) {
				entry = new CacheEntry(lastTimestamp, 1);
				entries.put(guid, entry);
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
			CacheEntry entry = entries.get(guid);
			if (entry == null) {
				entry = new CacheEntry(lastTimestamp, blockCount);
				entries.put(guid, entry);
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
			CacheEntry entry = entries.get(guid);
			if (entry == null)
				return -1;
			else if (entry.count > 1)
				return -1;
			else
				return entry.lastTimestamp;
		}
	}
	
	static private class Migration implements Runnable {
		@SuppressWarnings("unused")
		static private final Logger logger = GlobalSetup.getLogger(Migration.class);		
		
		private ExecutorService pool;
		private long processed;
		private long errorCount;
		private Collection<String> postIds;
		private Collection<String> userIds;
		private Collection<String> groupIds; 
		
		public Migration(Collection<String> postIds, Collection<String> userIds, Collection<String> groupIds) {
			this.postIds = postIds;
			this.userIds = userIds;
			this.groupIds = groupIds;
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
				String postId = null;
				String userId = null;
				String groupId = null;
				
				// synchronize access to the pool of work to do
				synchronized(this) {
					postId = pop(postIds);
					if (postId == null) {
						userId = pop(userIds);
						if (userId == null) {
							groupId = pop(groupIds);
						}
					} 
				}
				
				// but do work outside the lock
				Stacker stacker = EJBUtil.defaultLookup(Stacker.class);
				if (postId != null) {
					stacker.migratePost(postId);
				} else if (userId != null) {
					stacker.migrateUser(userId);
				} else if (groupId != null) {
					stacker.migrateGroup(groupId);
				} else {
					// nothing to do, we may have been 
					// shut down
				}
				
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
			return postIds.size() + userIds.size() + groupIds.size();
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
					postIds.clear();
					userIds.clear();
					groupIds.clear();
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
	
	public void migrateEverything() {
		
		if (disabled)
			throw new RuntimeException("stacking disabled, can't migrate anything");
		
		List<String> postIds;
		List<String> userIds;
		List<String> groupIds;
		
		Query q = em.createQuery("SELECT post.id FROM Post post");
		postIds = TypeUtils.castList(String.class, q.getResultList());

		q = em.createQuery("SELECT user.id FROM User user");
		userIds = TypeUtils.castList(String.class, q.getResultList());

		q = em.createQuery("SELECT group.id FROM Group group");
		groupIds = TypeUtils.castList(String.class, q.getResultList());
		
		final Migration migration = new Migration(postIds, userIds, groupIds);
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
	
	public void migratePost(String postId) {
		logger.debug("    migrating post {}", postId);
		final Post post = em.find(Post.class, postId);

		// this creates its own transaction for now... 
		stackPost(post.getGuid(), post.getPostDate().getTime());

		// FIXME so we need a new transaction here to see the block we just created
		runner.runTaskInNewTransaction(new Runnable() {
			public void run() {
				// we want to move PersonPostData info into UserBlockData
				// (this is obviously expensive as hell)
				
				Block block;
				try {
					block = queryBlock(BlockType.POST, post.getGuid(), -1);
				} catch (NotFoundException e) {
					throw new RuntimeException("no block found for post " + post);
				}
				List<UserBlockData> userDatas = queryUserBlockDatas(block);
				Map<User,UserBlockData> byUser = new HashMap<User,UserBlockData>();
				for (UserBlockData ubd : userDatas) {
					byUser.put(ubd.getUser(), ubd);
				}

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
				long maxClickedTime = 0;
				for (UserBlockData ubd : userDatas) {
					if (!ubd.isDeleted() && ubd.isClicked()) {
						clickedCount += 1;
						if (ubd.getClickedTimestampAsLong() > maxClickedTime)
							maxClickedTime = ubd.getClickedTimestampAsLong();
					}
				}
				if (block.getClickedCount() != clickedCount) {
					logger.debug("  fixing up clicked count from {} to {} for block " + block, block.getClickedCount(), clickedCount);
					block.setClickedCount(clickedCount);
				}
				
				// update the block's timestamp to match and update user cache
				if (maxClickedTime > block.getTimestampAsLong())
					stack(block, maxClickedTime);
			}
		});
	}
	
	public void migrateUser(String userId) {
		logger.debug("    migrating user {}", userId);
		User user = em.find(User.class, userId);
		long lastPlayTime = musicSystem.getLatestPlayTime(SystemViewpoint.getInstance(), user);
		stackMusicPerson(user.getGuid(), lastPlayTime);
	}
	
	public void migrateGroup(String groupId) {
		logger.debug("    migrating group {}", groupId);
		Group group = em.find(Group.class, groupId);
		List<GroupMessage> messages = groupSystem.getNewestGroupMessages(group, 1);
		if (messages.isEmpty())
			stackGroupChat(group.getGuid(), 0);
		else
			stackGroupChat(group.getGuid(), messages.get(0).getTimestamp().getTime());
	}
}
