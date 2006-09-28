package com.dumbhippo.live;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ListenerList;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jms.JmsConnectionType;
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
	static private boolean throwRandomExceptionOnEntryCreation = false;
	
	static public final String testExceptionText = "Test exception creating a cache entry.";
	static public final String objectCreationFailedText = "Waited for object creation that failed.";
	
	private static LiveState theState;
	
	// How often to run time based cleanups
	private static final long CLEANER_INTERVAL = 60 * 1000; // 1 minute
	
	// Maximum number of cleaner intervals for each user
	private static final int MAX_USER_CACHE_AGE = 30;
	
	// Maximum number of cleaner intervals for each group
	private static final int MAX_GROUP_CACHE_AGE = 30;

	// Maximum number of cleaner intervals for each post
	private static final int MAX_POST_CACHE_AGE = 30;
	
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
	 * Return a LiveUser cache object if one exists for a given userId.
	 * 
	 * @param userId
	 * @return a LiveUser cache object if one exists for a given userId
	 */
	public LiveUser peekLiveUser(Guid userId) {
		LiveUser current = userCache.peek(userId);
		if (current != null)
			return current;
		else
			return null;
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
		LiveUser current = userCache.peekForUpdate(userId);
		if (current != null)
			return (LiveUser)current.clone();
		else
			return null;
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
	 * Returns the number of LiveUser objects in the memory cache
	 */
	public int getLiveUserCount() {
		return userCache.getObjectCount(false);
	}

	/**
	 * Returns a snapshot of the current set of available users.
	 */
	public Set<LiveUser> getLiveUserAvailableSnapshot() {
		Set<LiveUser> result = new HashSet<LiveUser>();
		for (Guid guid : PresenceService.getInstance().getLocalPresentUsers("/users", 1))
			result.add(getLiveUser(guid));
		
		return result;
	}	
	
	/**
	 * Returns the number of available users
	 */
	public int getLiveUserAvailableCount() {
		return PresenceService.getInstance().getLocalPresentUsers("/users", 1).size();
	}

	/**
	 * Locate or create a LiveClientData cache object for a particular user.
	 * See getLiveClientData()
	 * 
	 * @param userId the user ID for which we should get a cache object
	 * @return the LiveClientData cache object.
	 */
	public LiveClientData getLiveClientData(Guid userId) {
		return clientDataCache.get(userId);
	}
	
	/**
	 * Locate a LiveClientData cache object for a particular user. Does
	 * not force creation if the object doesn't already exist, does not
	 * reset the cache age to zero, and doesn't wait for object update.
	 * (In the last case, it simply returns stale data.)  
	 * 
	 * @param userId the user ID for which we should get a cache object
	 * @return the LiveClientData cache object, or null if the user is
	 *   no client data has currently been computed 
	 */
	public LiveClientData peekLiveClientData(Guid userId) {
		return clientDataCache.peek(userId);
	}
	
	/**
	 * Get or create a LiveClientData cache object for a particular user in 
	 * preparation for updating it with new values. See
	 * getLiveUserForUpdate()
	 * 
	 * @param userId the post ID for which we should get a cache object
	 * @return a copy of the existing or newly created object
	 */
	LiveClientData getLiveClientDataForUpdate(Guid userId) {
		return (LiveClientData)clientDataCache.getForUpdate(userId).clone();
	}
	
	/**
	 * Get a LiveClientData cache object for a particular user in preparation
	 * for updating it with new values. See peekLiveUserForUpdate().  
	 * 
	 * @param user the user ID for which we should get a cache object
	 * @return a copy of the existing LiveClientData cache object if one is currently loaded,
	 *    otherwise null.
	 */
	public LiveClientData peekLiveClientDataForUpdate(Guid userId) {
		LiveClientData current = clientDataCache.peekForUpdate(userId);
		if (current != null)
			return (LiveClientData)current.clone();
		else
			return null;
	}	
	
	/**
	 * Insert an updated LiveClientData object into the cache. See updateLiveUser()
	 * 
	 * @param group new LiveClientData object to insert
	 */
	public void updateLiveClientData(LiveClientData newClientData) {
		clientDataCache.update(newClientData);
	}	
	
	/**
	 * Returns a snapshot of the current set of LiveClientData objects in
	 * the memory cache.
	 */
	public Set<LiveClientData> getLiveClientDataCacheSnapshot() {
		return clientDataCache.getAllObjects(false);
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
	
	/**
	 * Returns a snapshot of the current set of LivePost objects in
	 * the memory cache.
	 */
	public Set<LivePost> getLivePostSnapshot() {
		return postCache.getAllObjects(false);
	}
	
	/**
	 * Returns the number of LiveUser objects in the memory cache
	 */
	public int getLivePostCount() {
		return postCache.getObjectCount(false);
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
		LiveGroup current = groupCache.peekForUpdate(guid);
		if (current != null)
			return (LiveGroup)current.clone();
		else
			return null;
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
	 * Queue an event representing a change to the database state. The
	 * event will be processed asynchronously resulting in updates to
	 * the live state objects and also possibly in notifications sent
	 * to present users via XMPPP.
	 * 
	 * The event is queued as part of the current transaction, and will be
	 * sent out upon commit or discarded if the current transaction is
	 * rolled back.
	 * 
	 * @param event the event
	 */
	public void queueUpdate(LiveEvent event) {
		updateQueue.sendObjectMessage(event);
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
		} catch (InterruptedException e) {
			
		}
		
		updateQueue.close();
	}
	
	/**************************************************************************/

	private LiveObjectCache<LiveUser> userCache;
	
	private LiveObjectCache<LiveClientData> clientDataCache;
	
	private LiveObjectCache<LivePost> postCache;
	
	private LiveObjectCache<LiveGroup> groupCache;

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
		clientDataCache = new LiveObjectCache<LiveClientData>(
				new LiveObjectFactory<LiveClientData>() {
					public LiveClientData create(Guid guid) {
						LiveClientData clientData = new LiveClientData(guid);			
						LiveClientDataUpdater dataUpdater = EJBUtil.defaultLookup(LiveClientDataUpdater.class);
						dataUpdater.initialize(clientData);

						return clientData;
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
		
		updateQueue = new JmsProducer(LiveEvent.TOPIC_NAME, JmsConnectionType.TRANSACTED_IN_SERVER);
		
		cleaner = new Cleaner();
		cleaner.start();
		
		liveUserUpdater = new LiveUserPeriodicUpdater();
		liveUserUpdater.setName("LiveUserUpdater");		
		liveUserUpdater.start();
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
		LiveClientDataUpdater updater = EJBUtil.defaultLookup(LiveClientDataUpdater.class);
		LiveClientData clientData = getLiveClientData(guid);
		updater.sendAllNotifications(clientData);
	}	

	private static Map<Class<?>, ListenerList<?>> listenerLists = new HashMap<Class<?>, ListenerList<?>>();
	
	@SuppressWarnings("unchecked")
	public static <T extends LiveEvent> ListenerList<LiveEventListener<T>> getListenerList(Class<T> clazz, boolean create) {
		ListenerList<LiveEventListener<T>> listeners;
		
		synchronized(listenerLists) {
			 listeners = (ListenerList<LiveEventListener<T>>)listenerLists.get(clazz);
			 if (listeners == null && create) {
				 listeners = new ListenerList<LiveEventListener<T>>();
				 listenerLists.put(clazz, listeners);
			 }
		}
		
		return listeners;
	}

	public static <T extends LiveEvent> void addEventListener(Class<T> clazz, LiveEventListener<T> listener) {
		ListenerList<LiveEventListener<T>> listeners = getListenerList(clazz, true);
		listeners.addListener(listener);
	}
	
	public static <T extends LiveEvent> void removeEventListener(Class<T> clazz, LiveEventListener<T> listener) {
		ListenerList<LiveEventListener<T>> listeners = getListenerList(clazz, false);
		if (listeners != null)
			listeners.removeListener(listener);
	}
	
	@SuppressWarnings("unchecked")
	public void invokeEventListeners(LiveEvent event) {
		ListenerList listeners = getListenerList(event.getClass(), false);
		logger.info("Processing event: " + event);
		if (listeners != null) {
			for (Object o : listeners) {
				((LiveEventListener)o).onEvent(event);
			}
		}
	}
	
	private void clean() throws InterruptedException {
		// Bump the age of all objects, removing ones that pass the maximum age
		userCache.age();
		clientDataCache.age();
		postCache.age();
		groupCache.age();
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
					
					Set<Guid> toUpdate = PresenceService.getInstance().getLocalPresentUsers("/users", 1);

					LiveClientDataUpdater updater = EJBUtil.defaultLookup(LiveClientDataUpdater.class);					
					
					for (Guid guid : toUpdate) {
						updater.periodicUpdate(guid);
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
	 *  com.dumbhippo.live.LiveState.getInstance().stressTest(false); 
	 */
	public void stressTest(boolean throwRandomExceptionOnEntryCreation) {
		LiveState.throwRandomExceptionOnEntryCreation = throwRandomExceptionOnEntryCreation;
		
		// The use of runTaskInNewTransaction here could deadlock if we were
		// called from a transaction that also made database modifications;
		// but since we're just going to be called explicitly from the admin
		// console we are pretty safe.
		
		try {
			
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
									try {
									    getLiveUser(guid);
									} catch (RuntimeException e) {
								    	if (LiveState.throwRandomExceptionOnEntryCreation) {
								    		if ((e.getMessage().startsWith(LiveState.testExceptionText)) ||
								    		    (e.getMessage().startsWith(LiveState.objectCreationFailedText))) {
								    			logger.debug(e.getMessage());
								    		} else {
								    			throw e;
								    		}
								    	} else {
								    		throw e;
								    	}
									}
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
		
		} finally {
		    LiveState.throwRandomExceptionOnEntryCreation = false;
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
	
	public static boolean getThrowRandomExceptionOnEntryCreation() {
		return LiveState.throwRandomExceptionOnEntryCreation;
	}
}
