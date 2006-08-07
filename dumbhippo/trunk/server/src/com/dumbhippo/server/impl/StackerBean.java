package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.BlockType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;

@Stateless
public class StackerBean implements Stacker {

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
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data1=:data1");
			q.setParameter("data1", data1.toString());
		} else if (data2 >= 0) {
			q = em.createQuery("SELECT block FROM Block block WHERE block.blockType=:type AND block.data2=:data2");
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
			} else {
				UserBlockData data = new UserBlockData(u, block);
				em.persist(data);
			}
		}
		// the rest of "existing" is users who no longer are in the desired set
		for (User u : existing.keySet()) {
			cacheTask.addNeedsClearTimestamp(u.getGuid());
			
			UserBlockData old = existing.get(u);
			em.remove(old);
		}
		
		runner.runTaskOnTransactionCommit(cacheTask);		
	}
	
	public void stackMusicPerson(final Guid userId, final long activity) {
		runner.runTaskRetryingOnConstraintViolation(new Runnable() {
			public void run() {
				Block block = getOrCreateUpdatingTimestamp(BlockType.MUSIC_PERSON, userId, -1, activity);

				User user;
				try {
					user = EJBUtil.lookupGuid(em, User.class, userId);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
				Set<User> peopleWhoCare = identitySpider.getUsersWhoHaveUserAsContact(SystemViewpoint.getInstance(), user);
				peopleWhoCare.add(user); // FIXME include ourselves, this is probably a debug-only thing
				
				updateUserBlockDatas(block, peopleWhoCare);
			}
		});
	}

	public void stackGroupChat(final Guid groupId, final long activity) {
		runner.runTaskRetryingOnConstraintViolation(new Runnable() {
			public void run() {
				Block block = getOrCreateUpdatingTimestamp(BlockType.GROUP_CHAT, groupId, -1, activity);

				Group group;
				try {
					group = EJBUtil.lookupGuid(em, Group.class, groupId);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
				Set<User> groupMembers = groupSystem.getUserMembers(SystemViewpoint.getInstance(), group);
				
				updateUserBlockDatas(block, groupMembers);
			}
		});
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
				" ubd.user = :user AND ubd.block = block AND block.timestamp >= :timestamp ORDER BY block.timestamp DESC");
		q.setFirstResult(start);
		q.setMaxResults(count);
		q.setParameter("user", user);
		q.setParameter("timestamp", lastTimestamp);
		
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
}
