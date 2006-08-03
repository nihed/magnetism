package com.dumbhippo.live;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

public class LiveState {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveState.class);
	
	// This is for poking from the admin console
	static public boolean verboseLogging = false;
	
	private static LiveState theState;
	
	// How often to run time based cleanups
	private static final long CLEANER_INTERVAL = 60 * 1000; // 1 minute
	
	// Maximum number of cleaner intervals for each user
	private static final int MAX_USER_CACHE_AGE = 30;
	
	// Maximum number of cleaner intervals for each group
	private static final int MAX_GROUP_CACHE_AGE = 30;

	// Maximum number of cleaner intervals for each post
	private static final int MAX_POST_CACHE_AGE = 30;
	
	// Maximum number of cleaner intervals for un-pinged XMPP servers
	private static final int MAX_XMPP_SERVER_CACHE_AGE = 2;

	/**
	 * Get the global singleton LiveState object. The methods of the
	 * LiveState object may safely be called from any thread.
	 * 
	 * @return the global LiveState singleton
	 */
	static public LiveState getInstance() {
		synchronized (LiveState.class) {
			if (theState == null)
				theState = new LiveState();
			
			return theState;
		}
	}
	
	/**
	 * Locate or create a LiveUser cache object for a particular user.
	 * The cache age for that user will be reset to zero.
	 * 
	 * @param userId the user ID for which we should get a cache object
	 * @return the LiveUser cache object.
	 */
	public LiveUser getLiveUser(Guid userId) {
		return userCache.get(userId);
	}
	
	/**
	 * Get a LiveUser cache object for a particular user in preparation
	 * for updating it with new values. If the object doesn't already
	 * exist then nothing is done. On a succesful return, you must
	 * update the result and then call updateLiveUser() or subsequent
	 * attempts to retrieve the LiveUser object will hang.  
	 * 
	 * @param userId the user ID for which we should get a cache object
	 * @return a copy of the existing LiveUser cache object if one is currently loaded,
	 *    otherwise null.
	 */
	LiveUser peekLiveUserForUpdate(Guid userId) {
		return (LiveUser)userCache.peekForUpdate(userId).clone();
	}
	
	/**
	 * Get or create a LiveUser cache object for a particular user in 
	 * preparation for updating it with new values. On a succesful return, 
	 * you must update the result and then call updateLiveUser() or subsequent
	 * attempts to retrieve the LiveUser object will hang.  
	 * 
	 * The cache age for that user will be reset to zero.
	 *
	 * @param userId the user ID for which we should get a cache object
	 * @return a copy of the existing or newly created object
	 */
	LiveUser getLiveUserForUpdate(Guid userId) {
		return (LiveUser)userCache.getForUpdate(userId).clone();
	}
	
	/**
	 * Insert an updated LiveUser object into the cache. You must have
	 * previously called peekLiveUserForUpdate or getLiveUserForUpdate().
	 * 
	 * @param user new LiveUser object to insert
	 */
	void updateLiveUser(LiveUser user) {
		userCache.update(user);
	}
	
	/**
	 * Returns a snapshot of the current set of LiveUser objects in
	 * the memory cache.
	 */
	public Set<LiveUser> getLiveUserCacheSnapshot() {
		return userCache.getAllObjects(false);
	}
	

	/**
	 * Returns a snapshot of the current set of available users.
	 */
	public Set<LiveUser> getLiveUserAvailableSnapshot() {
		return userCache.getAllObjects(true);
	}	

	/**
	 * Locate or create a LivePost cache object for a particular user.
	 * See getLiveUser().
	 * 
	 * @param userId the post ID for which we should get a cache object
	 * @return the LivePost cache object.
	 */
	public LivePost getLivePost(Guid postId) {
		return postCache.get(postId);
	}
	
	/**
	 * Get or create a LivePost cache object for a particular post in 
	 * preparation for updating it with new values. See
	 * getLiveUserForUpdate()
	 * 
	 * @param userId the post ID for which we should get a cache object
	 * @return a copy of the existing or newly created object
	 */
	LivePost getLivePostForUpdate(Guid postId) {
		return (LivePost)postCache.getForUpdate(postId).clone();
	}
	
	/**
	 * Insert an updated LivePost object into the cache. See updateLiveUser()
	 * 
	 * @param user new LivePost object to insert
	 */
	void updateLivePost(LivePost post) {
		postCache.update(post);	
	}	
	
	public Set<LivePost> getLivePostSnapshot() {
		return postCache.getAllObjects(false);
	}

	/**
	 * Locate or create a LiveGroup cache object for a particular group.
	 * See getLiveUser()
	 * 
	 * @param groupId the group ID for which we should get a cache object
	 * @return the LiveGroup cache object.
	 */
	public LiveGroup getLiveGroup(Guid groupId) {
		return groupCache.get(groupId);
	}
	
	/**
	 * Get a LiveGroup cache object for a particular group in preparation
	 * for updating it with new values. See getLiveUserForUpdate().  
	 * 
	 * @param groupId the group ID for which we should get a cache object
	 * @return a copy of the existing LiveGroup cache object if one is currently loaded,
	 *    otherwise null.
	 */
	public LiveGroup peekLiveGroupForUpdate(Guid guid) {
		return (LiveGroup)groupCache.peekForUpdate(guid).clone();
	}	
	
	/**
	 * Insert an updated LiveGroup object into the cache. See updateLiveUser()
	 * 
	 * @param group new LiveGroup object to insert
	 */
	public void updateLiveGroup(LiveGroup newGroup) {
		groupCache.update(newGroup);
	}	
	
	/**
	 * Create a new LiveXmppServer object representing a newly connected
	 * instance of a Jabber server. The object can be looked up in
	 * the future by calling getXmppServer() with the unique cookie
	 * from LiveXmppServer.getServerIdentifier().
	 * 
	 * @return the new LiveXmppServer object.
	 */
	public LiveXmppServer createXmppServer() {
		LiveXmppServer xmppServer = new LiveXmppServer(this);
		xmppServers.put(xmppServer.getServerIdentifier(), xmppServer);
		
		return xmppServer;
	}
	
	/**
	 * Looks up a previously created LiveXmppServer, if it hasn't been
	 * timed out in between.
	 * 
	 * @param serverIdentifier cookie from LiveXmppServer.getServerIdentifier().
	 * @return the server, if found, otherwise null.
	 */
	public LiveXmppServer getXmppServer(String serverIdentifier) {
		return xmppServers.get(serverIdentifier);
	}

	/**
	 * Queue an event representing a change to the database state. The
	 * event will be processed asynchronously resulting in updates to
	 * the live state objects and also possibly in notifications sent
	 * to present users via XMPPP.
	 * 
	 * @param event the event
	 */
	public void queueUpdate(LiveEvent event) {
		synchronized(updateQueue) {
			updateQueue.send(updateQueue.createObjectMessage(event));
		}
	}
	
	private class LiveStateTransactionSynchronization implements Synchronization {
		private LiveEvent event;
		
		public LiveStateTransactionSynchronization(LiveEvent event) {
			this.event = event;
		}
		
		public void beforeCompletion() {
		}

		public void afterCompletion(int status) {
			if (status == Status.STATUS_COMMITTED) {
				logger.debug("running post-transaction event " + event);
				LiveState.getInstance().queueUpdate(event);
			}
		}
	}
	
	public void queuePostTransactionUpdate(EntityManager em, LiveEvent event) {
		Synchronization hook = new LiveStateTransactionSynchronization(event);
		TransactionManager tm;
		try {		
			tm = (TransactionManager) (new InitialContext()).lookup("java:/TransactionManager");
			tm.getTransaction().registerSynchronization(hook);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Clean up any persistant resources kept by this LiveState object.
	 * This should be the last call made against this instance of the 
	 * LiveState class; it is invoked when the application is being
	 * unloaded.  
	 */
	public void shutdown() {
		theState = null;
		
		cleaner.interrupt();
		
		try {
			cleaner.join();
			logger.info("Successfully stopped LiveState cleanup thread");
		} catch (InterruptedException e) {
			// Shouldn't happen, just ignore
		}
		
		liveUserUpdater.interrupt();
		
		try {
			liveUserUpdater.join();
			logger.info("Successfully stopped LiveState user updater thread");
		} catch (InterruptedException e) {}
	}
	
	/**************************************************************************/

	private LiveObjectCache<LiveUser> userCache;
	
	private LiveObjectCache<LivePost> postCache;
	
	private LiveObjectCache<LiveGroup> groupCache;

	// Current LiveXmppServer objects. This is simpler than the post and
	// user caches, since we don't want to keep around stray LiveXmppServer
	// objects. If the server fails to ping, we want to unconditionally
	// discard it. We, however, do use some of the same code to implement
	// aging and timing out of xmppServers as is used for cachedUsers and
	// cachedPosts.
	private Map<String, LiveXmppServer> xmppServers;

	private Cleaner cleaner;
	
	private LiveUserPeriodicUpdater liveUserUpdater;
	
	private JmsProducer updateQueue;
	
	private LiveState() {
		userCache = new LiveObjectCache<LiveUser>(
				new LiveObjectFactory<LiveUser>() {
					public LiveUser create(Guid guid) {
						LiveUser liveUser = new LiveUser(guid);			
						LiveUserUpdater userUpdater = EJBUtil.defaultLookup(LiveUserUpdater.class);
						userUpdater.initialize(liveUser);

						return liveUser;
					}
				},
				MAX_USER_CACHE_AGE);
		postCache = new LiveObjectCache<LivePost>(
				new LiveObjectFactory<LivePost>() {
					public LivePost create(Guid guid) {
						LivePost livePost = new LivePost(guid);			
						LivePostUpdater postUpdater = EJBUtil.defaultLookup(LivePostUpdater.class);
						postUpdater.initialize(livePost);

						return livePost;
					}
				},
				MAX_POST_CACHE_AGE);
		groupCache = new LiveObjectCache<LiveGroup>(
				new LiveObjectFactory<LiveGroup>() {
					public LiveGroup create(Guid guid) {
						LiveGroup liveGroup = new LiveGroup(guid);			
						LiveGroupUpdater groupUpdater = EJBUtil.defaultLookup(LiveGroupUpdater.class);
						groupUpdater.initialize(liveGroup);

						return liveGroup;
					}
				},
				MAX_GROUP_CACHE_AGE);
		
		xmppServers = new ConcurrentHashMap<String, LiveXmppServer>();
		
		updateQueue = new JmsProducer(LiveEvent.QUEUE, true);
		
		cleaner = new Cleaner();
		cleaner.start();
		
		liveUserUpdater = new LiveUserPeriodicUpdater();
		liveUserUpdater.setName("LiveUserUpdater");		
		liveUserUpdater.start();
	}
	
	// Internal function to update the availability count for the user;
	// see LiveXmppServer.userAvailable().
	void userAvailable(Guid userId) {
		LiveUser liveUser = getLiveUserForUpdate(userId);
		try {
			liveUser.setAvailableCount(liveUser.getAvailableCount() + 1);
			if (liveUser.getAvailableCount() == 1) {
				logger.debug("User {} is now available", liveUser.getGuid());			
				userCache.addStrongReference(liveUser);
			}
		} finally {
			updateLiveUser(liveUser);
		}
	}

	// Internal function to update the availability count for the user;
	// see LiveXmppServer.userUnavailable().
	void userUnavailable(Guid userId) {
		LiveUser liveUser = peekLiveUserForUpdate(userId);
		try {
			liveUser.setAvailableCount(liveUser.getAvailableCount() - 1);
			if (liveUser.getAvailableCount() == 0) {
				logger.debug("User {} is no longer available", liveUser.getGuid());
				userCache.dropStrongReference(liveUser);
			}
		} finally {
			updateLiveUser(liveUser);
		}
	}
	
	// Internal function to record a user joining the chat room for a post;
	// see LiveXmppServer.postRoomUserAvailable
	void postRoomUserAvailable(Guid postId, Guid userId, boolean isParticipant) {
		LivePost lpost = getLivePostForUpdate(postId);
		try {
			if (lpost.getChattingUserCount() == 0 && lpost.getViewingUserCount() == 0)
				postCache.addStrongReference(lpost);
	
			if (isParticipant)
				lpost.setChattingUserCount(lpost.getChattingUserCount() + 1);
			else
				lpost.setViewingUserCount(lpost.getViewingUserCount() + 1);
		} finally {
			updateLivePost(lpost);
		}

		logger.debug("Post {} now has {} viewing users and " + lpost.getChattingUserCount() + " chatting users", 
				postId, lpost.getViewingUserCount());
	}

	// Internal function to record a user leaving the chat room for a post;
	// see LiveXmppServer.postRoomUserUnavailable
	void postRoomUserUnavailable(Guid postId, Guid userId, boolean wasParticipant) {
		LivePost lpost = getLivePostForUpdate(postId);
		try {
			if (wasParticipant)
				lpost.setChattingUserCount(lpost.getChattingUserCount() - 1);
			else
				lpost.setViewingUserCount(lpost.getViewingUserCount() - 1);
			
			if (lpost.getChattingUserCount() == 0 && lpost.getViewingUserCount() == 0)
				postCache.dropStrongReference(lpost);
		} finally {
			updateLivePost(lpost);
		}
		
		logger.debug("Post {} now has {} viewing users and " + lpost.getChattingUserCount() + " chatting users", 
				postId, lpost.getViewingUserCount());  
	}

	public void resendAllNotifications(Guid guid) {
		LiveUserUpdater userUpdater = EJBUtil.defaultLookup(LiveUserUpdater.class);
		LiveUser luser = getLiveUser(guid);
		userUpdater.sendAllNotifications(luser);
	}	

	
	private void ageXmppServers() throws InterruptedException {
		for (Iterator<LiveXmppServer> i = xmppServers.values().iterator(); i.hasNext();) {
			LiveXmppServer xmppServer = i.next();
			int newAge = xmppServer.increaseCacheAge();
			if (newAge >= MAX_XMPP_SERVER_CACHE_AGE) {
				logger.debug("Discarding timed-out LiveXmppServer");
		                     i.remove();
		                     xmppServer.discard();
			}
		}
	}

	private void clean() throws InterruptedException {
		// Bump the age of all objects, removing ones that pass the maximum age
		userCache.age();
		postCache.age();
		groupCache.age();
		
		ageXmppServers();
	}

	// Thread that ages the different types of objects we keep around, and
	// also takes care of removing stale entries from WeakGuidMap objects.
	private class Cleaner extends Thread {
		@Override
		public void run() {
			long nextTime = System.currentTimeMillis() + CLEANER_INTERVAL;
			
			while (true) {
				try {
					Thread.sleep(nextTime - System.currentTimeMillis());

					clean();

					long currentTime = System.currentTimeMillis();
					nextTime = Math.max(currentTime, nextTime + CLEANER_INTERVAL);
					
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
	
	public void setUserUpdateInterval(int interval) {
		liveUserUpdater.setUserUpdateInterval(interval);
	}
	
	// Periodically decays the hotness of every active user
	private class LiveUserPeriodicUpdater extends Thread {
		int userUpdateInterval;
		
		public LiveUserPeriodicUpdater() {
			Configuration configuration = EJBUtil.defaultLookup(Configuration.class);
			String intervalString = configuration.getProperty(HippoProperty.USER_UPDATE_INTERVAL);
			userUpdateInterval = Integer.parseInt(intervalString);
		}
		
		public synchronized void setUserUpdateInterval(int interval) {
			userUpdateInterval = interval;
		}
		
		@Override
		public void run() {
			long nextTime = System.currentTimeMillis() + userUpdateInterval * 1000;
			
			while (true) {
				try {
					Thread.sleep(nextTime - System.currentTimeMillis());

					LiveUserUpdater userUpdater = EJBUtil.defaultLookup(LiveUserUpdater.class);					
					Set<LiveUser> users;
					// Grab a copy of the current user map to avoid locking the whole
					// object for a long time
					users = getLiveUserCacheSnapshot();
					
					for (LiveUser user : users) {
						userUpdater.periodicUpdate(user.getGuid());
					}		
				} catch (InterruptedException e) {
					break; // exit the loop
				} catch (Throwable t) {
					logger.warn("Unexpected exception in LiveUserPeriodicUpdater", t);
				} finally {
					// this is in finally so a recurring exception doesn't busy loop so badly
					long currentTime = System.currentTimeMillis();
					nextTime = Math.max(currentTime, nextTime + userUpdateInterval * 1000);
				}
			}
		}
	}
	
	/**
	 * Do some stress testing of LiveState by dumping all our caches, then trying to
	 * reload all users from the different threads simulaneously.
	 * 
	 * The normal way to run this is from the admin console
	 * 
	 *  com.dumbhippo.live.LiveState.verboseLogging = true;
	 *  com.dumbhippo.live.LiveState.stressTest();
	 */
	public void stressTest() {
		final List<Guid> toLookup = new ArrayList<Guid>();
		
		final TransactionRunner runner = EJBUtil.defaultLookup(TransactionRunner.class);
		runner.runTaskInNewTransaction(new Runnable() {
			public void run() {
				AccountSystem accountSystem = EJBUtil.defaultLookup(AccountSystem.class);
				
				for (Account account : accountSystem.getActiveAccounts()) {
					toLookup.add(account.getOwner().getGuid());
				}
			}
		});
		
		userCache.removeAllWeak();
		postCache.removeAllWeak();
		groupCache.removeAllWeak();
		
		final int NUM_THREADS = 10;
		ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);
		final long baseSeed = System.currentTimeMillis();
		
		List<Future<Object>> futures = new ArrayList<Future<Object>>(); 
		
		for (int i = 0; i < NUM_THREADS; i++) {
			final long seed = baseSeed + i;
			futures.add(threadPool.submit(new Callable<Object>() {
				public Object call() {
					runner.runTaskInNewTransaction(new Runnable() {
						public void run() {
							for (Guid guid : shuffle(toLookup, seed)) {
								getLiveUser(guid);
							}
						}
					});
					
					return null;
				}
			}));
		}
		
		for (Future<Object> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted!", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Test thread hit exception", e);
			}
		}
	}
	
	private static <T> List<T> shuffle(List<T> l, long seed) {
		List<T> result = new ArrayList<T>(l);
		Random r = new Random(seed);
		
		for (int i = 0; i < result.size(); i++) {
			int index = i + r.nextInt(result.size() - i);
			T old = result.get(i);
			result.set(i, result.get(index));
			result.set(index, old);
		}
		
		return result;
	}
}
