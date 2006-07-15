package com.dumbhippo.live;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jms.JmsProducer;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.util.EJBUtil;

public class LiveState {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveState.class);
	
	private static LiveState theState;
	
	// How often to run time based cleanups
	private static final long CLEANER_INTERVAL = 60 * 1000; // 1 minute
	
	// Maximum number of cleaner intervals for each user
	private static final int MAX_USER_CACHE_AGE = 30;
	
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
	

	// Helper object for LiveObjectCache
	private static class GuidReference<T> extends WeakReference<T> {
		private Guid guid;
		
		GuidReference(T t, Guid guid) {
			super(t);
			this.guid = guid;
		}
	}
	
	/* This class maintains several possible references to a LiveObject.
	   First, an object can be explicitly strongly referenced; e.g. for
	   a LiveUser, we strongly reference users which are currently present.
	   Second, there is a cache of recently touched objects which can 
	   be updated via the touch method.
	   Finally, a pure memory cache of the objects is maintained.  This
	   may be cleared at any time by the JVM if the object is neither
	   explicitly strongly referenced nor recently touched.
	*/
	private static class LiveObjectCache<T extends LiveObject> {
		private HashMap<Guid, T> strongReferences;
		private HashMap<Guid, T> recentReferences;
		private HashMap<Guid, GuidReference<T>> weakReferences;
		private ReferenceQueue<T> queue;
		
		public LiveObjectCache() {
			strongReferences = new HashMap<Guid, T>();
			recentReferences = new HashMap<Guid, T>();			
			weakReferences = new HashMap<Guid, GuidReference<T>>();
			queue = new ReferenceQueue<T>();
		}
		
		/**
		 * Add a LiveObject to the weak cache.
		 * 
		 * @param t object to be inserted
		 */
		public void poke(T t) {
			Guid guid = t.getGuid();			
			GuidReference<T> reference = new GuidReference<T>(t, guid);
			weakReferences.put(guid, reference);
		}
		
		/**
		 * Look for a LiveObject in the weak cache.
		 * 
		 * @param guid Guid whose associated owner to check for in the weak cache
		 * @return associated LiveObject, or null
		 */
		public T peek(Guid guid) {
			GuidReference<T> reference = weakReferences.get(guid);
			if (reference != null)
				return reference.get();
			else
				return null;			
		}
		
		/**
		 * Records a LiveObject as having been recently accessed.
		 * 
		 * @param obj
		 */
		public void touch(T obj) {
			recentReferences.put(obj.getGuid(), obj);
			obj.setCacheAge(0);			
			poke(obj);
		}
		
		/**
		 * Adds an explicit "strong" reference to a LiveObject;
		 * the object will not be evicted until dropStrongReference
		 * is invoked.
		 * 
		 * @param obj
		 */
		public void addStrongReference(T obj) {
			Guid guid = obj.getGuid();
			strongReferences.put(guid, obj);

		}
		
		/**
		 * Records a LiveObject as being a candidate for eviction,
		 * assuming it has not been recently accessed.
		 * 
		 * @param obj
		 */
		public void dropStrongReference(T obj) {
			Guid guid = obj.getGuid();
			strongReferences.remove(guid);
		}
		
		/**
		 * Returns a modifiable collection view of the recent
		 * references; this is used by the aging thread.
		 */
		public Collection<T> getRecentCache() {
			return recentReferences.values();
		}
		
		/**
		 * Returns an unmodifiable copy of the current set
		 * of strong references.
		 */
		public Set<T> getStrongReferenceCopy() {
			return Collections.unmodifiableSet(new HashSet<T>(strongReferences.values()));
		}		
		
		/**
		 * Returns an unmodifiable copy of the entire weak
		 * cache; this is useful for debugging purposes, e.g.
		 * the admin page.
		 */
		public Set<T> getWeakCacheCopy() {
			Set<T> ret = new HashSet<T>();
			for (GuidReference<T> ref : weakReferences.values()) {
				T obj = ref.get();
				if (obj != null)
					ret.add(obj);
			}
			return Collections.unmodifiableSet(ret);
		}
		
		/**
		 * Insert a modified LiveObject into the cache.    
		 */
		public void update(T obj) {
			T old = peek(obj.getGuid());
			// This guid must already exist in the cache, and
			// the object being inserted must not be the same
			// one as already exists.
			assert(old != null && old != obj);
			if (strongReferences.containsKey(obj.getGuid())) {
				strongReferences.put(obj.getGuid(), obj);
			}
			poke(obj);
			touch(obj);
		}		
		
		/**
		 * Invoke periodically to clean up the weak reference cache.
		 */
		public void clean() {
			while (true) {
				GuidReference<? extends T> reference = (GuidReference<? extends T>)queue.poll();
				if (reference == null)
					break;
				weakReferences.remove(reference.guid);
			}
		}
	}
		
	
	/**
	 * Get a LiveUser cache object for a particular user if one is
	 * loaded, but does not create one if one doesn't currently
	 * exist. The cache age of the returned object is unmodified.
	 * Generally, this would not be called by code outside the
	 * com.dumbhippo.live package.
	 * 
	 * @param userId the user ID for which we should get a cache object
	 * @return the LiveUser cache object if one is currently loaded,
	 *    otherwise null.
	 */
	public synchronized LiveUser peekLiveUser(Guid userId) {
		return userCache.peek(userId);
	}
	
	/**
	 * Locate or create a LiveUser cache object for a particular user.
	 * The cache age of the returned object will be reset to zero.
	 * 
	 * @param userId the user ID for which we should get a cache object
	 * @return the LiveUser cache object.
	 */
	public synchronized LiveUser getLiveUser(Guid userId) {
		LiveUser liveUser = peekLiveUser(userId);
		if (liveUser == null) {
			liveUser = new LiveUser(userId);
			LiveUserUpdater userUpdater = EJBUtil.defaultLookup(LiveUserUpdater.class);
			userUpdater.initialize(liveUser);
			userCache.poke(liveUser);
		}
		
		userCache.touch(liveUser);
		
		return liveUser;
	}
	
	/**
	 * Insert an updated LiveUser object into the cache.  Since LiveUser
	 * objects are immutable, for updating them it is necessary to clone
	 * the existing instance, then insert the updated copy into the cache,
	 * overwriting the previous.
	 * 
	 * @param user new LiveUser object to insert
	 * @return the inserted LiveUser object
	 */
	public synchronized LiveUser updateLiveUser(LiveUser user) {
		userCache.update(user);
		return user;
	}
	
	/**
	 * Returns a snapshot of the current set of LiveUser objects in
	 * the memory cache.
	 */
	public synchronized Set<LiveUser> getLiveUserCacheSnapshot() {
		return userCache.getWeakCacheCopy();
	}
	

	/**
	 * Returns a snapshot of the current set of available users.
	 */
	public Set<LiveUser> getLiveUserAvailableSnapshot() {
		return userCache.getStrongReferenceCopy();
	}	
	
	/**
	 * Get a LivePost cache object for a particular post if one is
	 * loaded, but does not create one if one doesn't currently
	 * exist. The cache age of the returned object is unmodified.
	 * Generally, this would not be called by code outside the
	 * com.dumbhippo.live package.
	 * 
	 * @param postId the post ID for which we should get a cache object
	 * @return the LivePost cache object if one is currently loaded,
	 *    otherwise null.
	 */
	public synchronized LivePost peekLivePost(Guid postId) {
		return postCache.peek(postId);
	}
	
	/**
	 * Locate or create a LivePost cache object for a particular post.
	 * The cache age of the returned object will be reset to zero.
	 * 
	 * @param postId the post ID for which we should get a cache object
	 * @return the LivePost cache object.
	 */
	public synchronized LivePost getLivePost(Guid postId) {
		LivePost livePost = peekLivePost(postId);
		if (livePost == null) {

			livePost = new LivePost(postId);			
			LivePostUpdater postUpdater = EJBUtil.defaultLookup(LivePostUpdater.class);
			postUpdater.initialize(livePost);
			
			postCache.poke(livePost);
		}

		postCache.touch(livePost);
		
		return livePost;
	}

	/**
	 * Insert an updated LivePost object into the cache.  See updateLiveUser.
	 * 
	 * @param user new LiveUser object to insert
	 * @return the inserted LiveUser object
	 */
	public synchronized LivePost updateLivePost(LivePost post) {
		postCache.update(post);	
		return post;
	}	
	
	public synchronized Set<LivePost> getLivePostSnapshot() {
		return postCache.getWeakCacheCopy();
	}

	/**
	 * Locate or create a LiveGroup cache object for a particular group.
	 * The cache age of the returned object will be reset to zero.
	 * 
	 * @param groupId the group ID for which we should get a cache object
	 * @return the LiveGroup cache object.
	 */
	public synchronized LiveGroup getLiveGroup(Guid groupId) {
		LiveGroup liveGroup = peekLiveGroup(groupId);
		if (liveGroup == null) {

			liveGroup = new LiveGroup(groupId);			
			LiveGroupUpdater groupUpdater = EJBUtil.defaultLookup(LiveGroupUpdater.class);
			groupUpdater.initialize(liveGroup);
			
			groupCache.poke(liveGroup);
		}

		groupCache.touch(liveGroup);
		
		return liveGroup;
	}
	
	public LiveGroup peekLiveGroup(Guid guid) {
		return groupCache.peek(guid);
	}	
	
	public synchronized void updateLiveGroup(LiveGroup newGroup) {
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
	public synchronized LiveXmppServer createXmppServer() {
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
	public synchronized LiveXmppServer getXmppServer(String serverIdentifier) {
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
		userCache = new LiveObjectCache<LiveUser>();
		postCache = new LiveObjectCache<LivePost>();
		groupCache = new LiveObjectCache<LiveGroup>();
		
		xmppServers = new HashMap<String, LiveXmppServer>();
		
		updateQueue = new JmsProducer(LiveEvent.QUEUE, true);
		
		cleaner = new Cleaner();
		cleaner.start();
		
		liveUserUpdater = new LiveUserPeriodicUpdater();
		liveUserUpdater.setName("LiveUserUpdater");		
		liveUserUpdater.start();
	}
	
	// Internal function to update the availability count for the user;
	// see LiveXmppServer.userAvailable().
	synchronized void userAvailable(Guid userId) {
		LiveUser liveUser = getLiveUser(userId);
		liveUser = (LiveUser) liveUser.clone(); // Create a copy to update
		liveUser.setAvailableCount(liveUser.getAvailableCount() + 1);
		if (liveUser.getAvailableCount() == 1) {
			logger.debug("User {} is now available", liveUser.getGuid());			
			userCache.addStrongReference(liveUser);
		}
		userCache.update(liveUser);
		AccountSystem accounts = EJBUtil.defaultLookup(AccountSystem.class);
		accounts.touchLoginDate(userId);
	}

	// Internal function to update the availability count for the user;
	// see LiveXmppServer.userUnavailable().
	synchronized void userUnavailable(Guid userId) {
		LiveUser liveUser = getLiveUser(userId);
		liveUser = (LiveUser) liveUser.clone();		
		liveUser.setAvailableCount(liveUser.getAvailableCount() - 1);
		if (liveUser.getAvailableCount() == 0) {
			logger.debug("User {} is no longer available", liveUser.getGuid());
			userCache.dropStrongReference(liveUser);
		}
		userCache.update(liveUser);
	}
	
	// Internal function to record a user joining the chat room for a post;
	// see LiveXmppServer.postRoomUserAvailable
	synchronized void postRoomUserAvailable(Guid postId, Guid userId, boolean isParticipant) {
		LivePost lpost = getLivePost(postId);
		lpost = (LivePost) lpost.clone(); // Create a copy to reinsert

		if (lpost.getChattingUserCount() == 0 && lpost.getViewingUserCount() == 0)
			postCache.addStrongReference(lpost);

		if (isParticipant)
			lpost.setChattingUserCount(lpost.getChattingUserCount() + 1);
		else
			lpost.setViewingUserCount(lpost.getViewingUserCount() + 1);
		postCache.update(lpost);

		logger.debug("Post {} now has {} viewing users and " + lpost.getChattingUserCount() + " chatting users", 
				postId, lpost.getViewingUserCount());
	}

	// Internal function to record a user leaving the chat room for a post;
	// see LiveXmppServer.postRoomUserUnavailable
	synchronized void postRoomUserUnavailable(Guid postId, Guid userId, boolean wasParticipant) {
		LivePost lpost = getLivePost(postId);
		lpost = (LivePost) lpost.clone(); // Create a copy to reinsert

		if (wasParticipant)
			lpost.setChattingUserCount(lpost.getChattingUserCount() - 1);
		else
			lpost.setViewingUserCount(lpost.getViewingUserCount() - 1);
		postCache.update(lpost);
		
		if (lpost.getChattingUserCount() == 0 && lpost.getViewingUserCount() == 0)
			postCache.dropStrongReference(lpost);
					
		logger.debug("Post {} now has {} viewing users and " + lpost.getChattingUserCount() + " chatting users", 
				postId, lpost.getViewingUserCount());  
	}

	public synchronized void resendAllNotifications(Guid guid) {
		LiveUserUpdater userUpdater = EJBUtil.defaultLookup(LiveUserUpdater.class);
		LiveUser luser = getLiveUser(guid);
		userUpdater.sendAllNotifications(luser);
	}	
		
	private <T extends Ageable> void age(Collection<T> set, int maxAge) {
		for (Iterator<T> i = set.iterator(); i.hasNext();) {
			T t = i.next();
			int newAge = t.getCacheAge() + 1;
			if (newAge < maxAge) {
				t.setCacheAge(newAge);
			} else {
				logger.debug("Discarding timed-out instance of " + t.getClass().getName());
				t.discard();
				i.remove();
			}
		}
	}
	
	private synchronized void clean() {
		// Bump the age of all objects, removing ones that pass the maximum age
		age(userCache.getRecentCache(), MAX_USER_CACHE_AGE);
		age(postCache.getRecentCache(), MAX_POST_CACHE_AGE);
		age(groupCache.getRecentCache(), MAX_USER_CACHE_AGE);
		age(xmppServers.values(), MAX_XMPP_SERVER_CACHE_AGE);
		
		// Clean up the WeakGuidMap objects
		userCache.clean();
		postCache.clean();
		groupCache.clean();
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
						userUpdater.periodicUpdate(user);
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
}
