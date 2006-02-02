package com.dumbhippo.live;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.jms.JmsProducer;
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
		return userMap.peek(userId);
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

			userMap.poke(userId, liveUser);
		}

		cachedUsers.add(liveUser);
		liveUser.setCacheAge(0);
		
		return liveUser;
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
		return postMap.peek(postId);
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
			
			postMap.poke(postId, livePost);
		}

		cachedPosts.add(livePost);
		livePost.setCacheAge(0);
		
		return livePost;
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
			logger.debug("Successfully stoped LiveState cleanup thread");
		} catch (InterruptedException e) {
			// Shouldn't happen, just ignore
		}
	}
	
	/**************************************************************************/

	// Cache of LiveUser objects. availableUsers strongly references users
	// that are present at the current time. cachedUsers strongly references
	// users that have been recently accessed. userMap allows us to look
	// up any existent LiveUser object by Guid, whether it is in 
	// availableUsers, cachedUsers, or in neither but referenced elsewhere.
	private Set<LiveUser> availableUsers;
	private Set<LiveUser> cachedUsers;
	private WeakGuidMap<LiveUser> userMap;
	
	// Cache of LivePost objects. See comments for LiveUser above; there is
	// no 'availablePosts' set, since presence is a user-only concept
	private Set<LivePost> cachedPosts;
	private WeakGuidMap<LivePost> postMap;

	// Current LiveXmppServer objects. This is simpler than the post and
	// user caches, since we don't want to keep around stray LiveXmppServer
	// objects. If the server fails to ping, we want to unconditionally
	// discard it. We, however, do use some of the same code to implement
	// aging and timing out of xmppServers as is used for cachedUsers and
	// cachedPosts.
	private Map<String, LiveXmppServer> xmppServers;

	private Cleaner cleaner;
	
	private JmsProducer updateQueue;
	
	private LiveState() {
		availableUsers = new HashSet<LiveUser>();
		cachedUsers = new HashSet<LiveUser>();
		userMap = new WeakGuidMap<LiveUser>();

		cachedPosts = new HashSet<LivePost>();
		postMap = new WeakGuidMap<LivePost>();
		
		xmppServers = new HashMap<String, LiveXmppServer>();
		
		updateQueue = new JmsProducer(LiveEvent.QUEUE, true);
		
		cleaner = new Cleaner();
		cleaner.start();
	}
	
	// Internal function to update the availability count for the user;
	// see LiveXmppServer.userAvailable().
	synchronized void userAvailable(Guid userId) {
		LiveUser liveUser = getLiveUser(userId);
		liveUser.setAvailableCount(liveUser.getAvailableCount() + 1);
		if (liveUser.getAvailableCount() == 1) {
			availableUsers.add(liveUser);
			logger.debug("User " + liveUser.getUserId() + " is now available");
		}
	}

	// Internal function to update the availability count for the user;
	// see LiveXmppServer.userUnavailable().
	synchronized void userUnavailable(Guid userId) {
		LiveUser liveUser = getLiveUser(userId);
		liveUser.setAvailableCount(liveUser.getAvailableCount() - 1);
		if (liveUser.getAvailableCount() == 0) {
			availableUsers.remove(liveUser);
			logger.debug("User " + liveUser.getUserId() + " is no longer available");
		}
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
		age(cachedUsers, MAX_USER_CACHE_AGE);
		age(cachedPosts, MAX_POST_CACHE_AGE);
		age(xmppServers.values(), MAX_XMPP_SERVER_CACHE_AGE);
		
		// Clean up the WeakGuidMap objects
		userMap.clean();
		postMap.clean();
	}

	// Helper object for WeakGuidMap
	private static class GuidReference<T> extends WeakReference<T> {
		private Guid guid;
		
		GuidReference(T t, Guid guid) {
			super(t);
			this.guid = guid;
		}
	}
	
	// A map from Guid to a cache object for that Guid. The reference
	// to the object is weak, so if all other references to the object
	// are released, it will be freed and the WeakGuidMap won't keep
	// it alive. We build a cache by combining a WeakGuidMap with
	// other mechanisms for strongly referencing an object. (For
	// LiveUser, the user object is strongly referenced if the user
	// is present, and also for an interval after it is last 
	// looked up from the cache.)
	private static class WeakGuidMap<T> {
		private HashMap<Guid, GuidReference<T>> map;
		private ReferenceQueue<T> queue;
		
		public WeakGuidMap() {
			map = new HashMap<Guid, GuidReference<T>>();
			queue = new ReferenceQueue<T>();
		}
		
		public void poke(Guid guid, T t) {
			GuidReference<T> reference = new GuidReference<T>(t, guid);
			map.put(guid, reference);
		}
		
		public T peek(Guid guid) {
			GuidReference<T> reference = map.get(guid);
			if (reference != null)
				return reference.get();
			else
				return null;			
		}
		
		// Remove any pending weak references
		public void clean() {
			while (true) {
				GuidReference<? extends T> reference = (GuidReference<? extends T>)queue.poll();
				if (reference == null)
					break;
				map.remove(reference.guid);
			}
		}
	}
	
	// Thread that ages the different types of objects we keep around, and
	// also takes care of removing stale entries from WeakGuidMap objects.
	private class Cleaner extends Thread {
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
}
