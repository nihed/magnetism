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
		
		updateQueue.close();
	}
	
	/**************************************************************************/

	private LiveObjectCache<LiveUser> userCache;
	
	private LiveObjectCache<LiveGroup> groupCache;

	private Cleaner cleaner;
	
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
