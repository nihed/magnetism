package com.dumbhippo.live;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.jboss.system.ServiceMBeanSupport;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannelFactory;
import org.jgroups.Message;
import org.jgroups.TimeoutException;
import org.jgroups.View;
import org.slf4j.Logger;
import org.w3c.dom.Element;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ListenerList;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.identity20.Guid;

/**
 * This class implements a distributed data structure that tracks what users are
 * present at different logical locations. Logical locations are represented
 * by uninterpreted strings (/kind/instance is conventional); presence is represented
 * by an integer 0 = not present, 1 = present, 2 more present, ...
 * 
 * See http://developer.mugshot.org/wiki/Distributed_Presence for details about the
 * algorithm and protocol, and please update that if you change anything here.
 * 
 * @author otaylor
 */
public class PresenceService extends ServiceMBeanSupport implements PresenceServiceMBean {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PresenceService.class);
	
	static private final String CLUSTER_NAME = "PresenceService";
	
	/* NOTES: Concurrency and Thread Safety
	 * 
	 * Functions in this class can be accessed from a number of different threads:
	 *  - The application's own threads
	 *     - Read local and remote state
	 *     - Modify local state
	 *     - Add and remove listeners
	 *  - Application code running in the thread we use for notifications 
	 *     - Read local and remote state
	 *     - Modify local state (rare, but allowed)
	 *     - Add and remove listeners (rare, but allowed)
	 *  - Our thread for receiving JGroups events (ReceiveThread)
	 *     - Modify remote state
	 *     
	 * The amount of locking needed is reduced by several observations:
	 * 
	 *   - Remote state is modified only from a single thread
	 *   - Items are only added to the map of local location information, never removed.
	 *   - When reading state for an application, we generally just need a snapshot;
	 *     it doesn't even need to be a consistent snapshot - it's fine if the
	 *     returned information shows two users both present even if one left before
	 *     the other joined
	 *     
	 *  These observations allow us to use ConcurrentHashMap rather than full locks
	 *  in a couple of places, and also to synchronize only on individual locations
	 *  rather than globally. (More details in comments at the affected code portions)
	 *  
	 *  The general locking scheme we use is to use a single lock for all modifications
	 *  and reads to a particular LocationInfo (server/location) pair, and to lock
	 *  only as needed at a higher level. A couple of tricky points to watch out for:
	 *  
	 *   - When modifying a LocalLocationInfo we need to hold a lock over the
	 *     modification and queueing the message we send out to preserve ordering.
	 *   - Modifications to LocalLocationInfo.notifyGuids and LocalLocationInfo.listeners 
	 *     need to be protected  by the same lock to prevent race conditions when adding 
	 *     listeners.
	 *  
	 *  It would be possible to use read-write locks on LocationInfo to further improve
	 *  concurrency, but if lookups are taking enough time so that it matters, it might
	 *  make more sense to look at improving the data structures so that you don't
	 *  have to do separate lookups for each server and sum the results.
	 */

	// -----------------------  Data Storage Types ---------------------------
	
	private static class UserInfo {
		public long serial; // only actually needed for RemoteLocationInfo
		public int presence;
	}
	
	// Base class for storing information about users present at a location; read accesses are
	// the same for both remote and local servers, but updates work differently and are
	// implemented in LocalLocationInfo and RemoteLocationInfo.
	private abstract static class LocationInfo {
		// We protect modifications to users and to the contained UserInfo by synchronizing
		// on the LocationInfo object. For LocalLocationInfo, this level of synchronization
		// is necessary because we have updates from multiple application threads. For
		// RemoteLocationInfo updates are only from the thread processing JGroups messages
		// but it's simplest to use the same scheme for that.
		protected Map<Guid, UserInfo> users = new HashMap<Guid, UserInfo>();
		
		public final synchronized int getPresence(Guid guid) {
			UserInfo userInfo = users.get(guid);
			if (userInfo != null) {
				return userInfo.presence;
			}
			else {
				return 0;
			}
		}
		
		public final synchronized void addPresentUsers(Set<Guid> guids, int presence) {
			for (Entry<Guid, UserInfo> entry : users.entrySet()) {
				if (entry.getValue().presence >= presence)
					guids.add(entry.getKey());
			}
		}
		
		public final synchronized int getUserCount() {
			return users.size();
		}
	}
	
	// The LocalLocationInfo structure maintains the information about presence
	// of users on the local server, and is also where we store information about
	// notification listeners (which get notifications to changes about remote
	// presence as well.)
	//
	// We need to retain LocalLocationInfo objects even when there are no present users 
	// and notifications since the distributed presence algorithm relies on having a
	// an increasing serial for each pair of server and location.
	private class LocalLocationInfo extends LocationInfo {
		private String location;

		// Protecting both listeners and notifyListeners with the same synchronization
		// (on the object) is important to prevent race conditions when we check
		// listeners.isEmpty() in scheduleNotification.  
		private ListenerList<PresenceListener> listeners = new ListenerList<PresenceListener>();
		private Set<Guid> notifyUsers = null;

		// This has to be updated atomically along with changes to the users map
		protected long serial = 0;
		
		public LocalLocationInfo(String location) {
			this.location = location;
		}
		
		public final synchronized void setPresence(Guid guid, int localPresence) {
			serial++;
			
			if (localPresence == 0) {
				users.remove(guid);
			} else {
				UserInfo userInfo = users.get(guid);
				if (userInfo == null) {
					userInfo = new UserInfo();
					users.put(guid, userInfo);
				}
				
				userInfo.presence = localPresence;
			}
			
			// It's important to queue the message while synchronized, so that the order
			// we send out messages matches the order of modifications. As long as there is
			// at least one thread in the JGroups protocol stock, channel.send() will never
			// block, so it's safe to call while synchronized.
			PresenceChangedMessage message = new PresenceChangedMessage(location, serial, guid, localPresence); 
			
			try {
				// logger.debug("Broadcasting {}", message);
				channel.send(null, null, message);
			} catch (ChannelNotConnectedException e) {
			} catch (ChannelClosedException e) {
			}
			
			scheduleNotification(guid);
		}
		
		public final synchronized void clearPresence() {
			serial++;

			Map<Guid, UserInfo> oldUsers = users;
			users = new HashMap<Guid, UserInfo>();
			
			for (Guid guid : oldUsers.keySet()) {
				// It would be more a little more efficient to have an "all users left" message in the protocol,
				// but there isn't a big need to optimize shutdown - in production, it's probably best
				// to just kill the server.
				PresenceChangedMessage message = new PresenceChangedMessage(location, serial, guid, 0); 
			
				try {
					// logger.debug("Broadcasting {}", message);
					channel.send(null, null, message);
				} catch (ChannelNotConnectedException e) {
				} catch (ChannelClosedException e) {
				}
			}
			
			scheduleNotification(oldUsers.keySet());
		}

		public synchronized LocationState getState() {
			UserState userStates[] = new UserState[users.size()];
			int i = 0;
			for (Entry<Guid, UserInfo> entry : users.entrySet()) {
				userStates[i] = new UserState(entry.getKey(), entry.getValue().presence);
				i++;
			}
		
			return new LocationState(location, serial, userStates);
		}
		
		public synchronized void addListener(PresenceListener listener) {
			listeners.addListener(listener);
		}

		public synchronized void removeListener(PresenceListener listener) {
			listeners.removeListener(listener);
		}
		
		private void ensureNotifyUsers() {
			if (notifyUsers == null) {
				notifyUsers = new HashSet<Guid>();
				notifyExecutor.execute(new Runnable() {
					public void run() {
						doNotifications();
					}
				});
			}
		}
		
		public synchronized void scheduleNotification(Guid guid) {
			if (listeners.isEmpty())
				return;
			
			ensureNotifyUsers();
			notifyUsers.add(guid);
		}
		
		public synchronized void scheduleNotification(Set<Guid> guids) {
			if (listeners.isEmpty())
				return;
			
			ensureNotifyUsers();
			notifyUsers.addAll(guids);
		}

		public void doNotifications() {
			Set<Guid> guids;
			Iterator<PresenceListener> toNotify; 
			
			// We snapshot listeners and move the notificationUsers set aside
			// so that we don't need to hold a lock during notification.
			synchronized(this) {
				guids = notifyUsers;
				notifyUsers = null;
				toNotify = listeners.iterator();
			}
			
			while (toNotify.hasNext()) {
				PresenceListener listener = toNotify.next();
				for (Guid guid : guids)
					listener.presenceChanged(guid);
			}
		}
		
		@Override
		public String toString() {
			return "[LocalLocationInfo: " + location + "]";
		}
	}

	// Information about the users at one location on a remote server 
	private class RemoteLocationInfo extends LocationInfo {
		private Address address; // Only needed for debug logging
		private String location;
		
		// The awaitingInitialState flag is used to track whether we need to keep around
		// the serial numbers for users that are *not* present but were present earlier.
		// We don't need this information after initial startup, since PresenceChanged
		// messages will be delivered in order, but we don't have guaranteed ordering
		// between multicast PresenceChanged and unicast CurrentState messages. 
		private boolean awaitingInitialState;
		
		public RemoteLocationInfo(Address address, String location, boolean awaitingInitialState) {
			this.address = address;
			this.location = location;
			this.awaitingInitialState = awaitingInitialState;
		}
		
		public final synchronized void updatePresence(Guid guid, long updateSerial, int presence) {
			if (presence == 0 && !awaitingInitialState) {
				users.remove(guid);
			} else {
				UserInfo userInfo = users.get(guid);
				if (userInfo == null) {
					userInfo = new UserInfo();
					users.put(guid, userInfo);
				}
				if (updateSerial > userInfo.serial) {
					// logger.debug("{}: Updating presence for {} to {}, serial {}", 
					//			 new Object[] { this, guid, presence, updateSerial });
					userInfo.serial = updateSerial;
					userInfo.presence = presence;
				} else {
					// logger.debug("{}: Not updating presence, since update serial {} is older than {}", 
					//		     new Object[] { this, updateSerial, userInfo.serial } );
				}
			}
		}
		
		public synchronized void finishInitialState() {
			if (!awaitingInitialState)
				return;
			
			// Now we have the current state for the location/server pair, we don't need 
			// to keep around presence = 0 entries, so walk through and delete them
			for (Iterator<UserInfo> i = users.values().iterator(); i.hasNext();) {
				UserInfo userInfo = i.next();
				if (userInfo.presence == 0)
					i.remove();
			}
			
			awaitingInitialState = false;
		}

		// This is called after removing a server on cluster view change
		public void notifyAllRemoved() {
			scheduleNotification(location, users.keySet());
		}
		
		@Override
		public String toString() {
			return "[RemoteLocationInfo: " + address + " - " + location + "]";
		}
	}
	
	// Information about users on a remote server
	private class ServerInfo {
		// RemoteLocationInfo is read from application threads, but only ever updated from the
		// thread handling incoming JGroups messages. This allows us to use ConcurrentHashMap
		// rather than explicit locking, since the reader threads just need a snapshot
		private Map<String, RemoteLocationInfo> locations = new ConcurrentHashMap<String, RemoteLocationInfo>();
		
		// Set during initial startup until a CurrentState message is received. See note for
		// the corresponding variable in RemoteLocationInfo for the significance.
		private boolean awaitingInitialState = true;

		private Address address;

		private ServerInfo(Address address) {
			this.address = address;
		}
		
		private Address getAddress() {
			return address;
		}
		
		public RemoteLocationInfo peekLocationInfo(String location) {
			return locations.get(location);
		}
		
		public RemoteLocationInfo getLocationInfo(String location) {
			RemoteLocationInfo locationInfo = locations.get(location);
			if (locationInfo == null) {
				locationInfo = new RemoteLocationInfo(address, location, awaitingInitialState);
				locations.put(location, locationInfo);
			}
			
			return locationInfo;
		}

		public int getPresence(String location, Guid guid) {
			LocationInfo locationInfo = peekLocationInfo(location);
			if (locationInfo != null) {
				return locationInfo.getPresence(guid);
			} else {
				return 0;
			}
		}
		
		public void finishInitialState() {
			// We clean up the awaitingInitialState value for any locations that
			// were created by PresenceChanged messages but not in the CurrentState
			// message (which may have been created before the PresenceChanged message)
			for (RemoteLocationInfo locationInfo : locations.values())
				locationInfo.finishInitialState();
			
			awaitingInitialState = false;
		}
		
		private void handleLocationState(LocationState locationState) {
			RemoteLocationInfo locationInfo = getLocationInfo(locationState.location);

			for (UserState userState : locationState.users) {
				locationInfo.updatePresence(userState.user, locationState.serial, userState.presence);
			}
			
			locationInfo.finishInitialState();
			if (locationInfo.getUserCount() == 0)
				locations.remove(locationState.location);
		}
		
		private void updatePresence(String location, Guid user, long serial, int presence) {
			RemoteLocationInfo locationInfo = getLocationInfo(location);
			locationInfo.updatePresence(user, serial, presence);
			
			if (locationInfo.getUserCount() == 0)
				locations.remove(location);
		}
		
		// This is called after removing a server on cluster view change
		public void notifyAllRemoved() {
			for (RemoteLocationInfo location : locations.values())
				location.notifyAllRemoved();
		}
	}
	
	// -------------------- Message Types ---------------------------
	
	// See the protocol documentation on the wiki page for the significance
	
	private static class RequestStateMessage implements Serializable {
		private static final long serialVersionUID = 1L;
		public RequestStateMessage() {}
		
		@Override
		public String toString() {
			return "[RequestStateMessage]";
		}
	}

	private static class UserState implements Serializable {
		private static final long serialVersionUID = 1L;
		public Guid user;
		public int presence;

		public UserState() {}
		public UserState(Guid user, int presence) {
			this.user = user;
			this.presence = presence;
		}
		
		@Override
		public String toString() {
			return "[UserState: " + user + ": " + presence + "]";
		}
	}
	
	private static class LocationState implements Serializable {
		private static final long serialVersionUID = 1L;
		public String location;
		public long serial;
		public UserState users[];
		
		public LocationState() {}
		public LocationState(String location, long serial, UserState users[]) {
			this.location = location;
			this.serial = serial;
			this.users = users;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("[LocationState: ");
			sb.append("location = ").append(location);
			sb.append(" serial = ").append(serial).append(" ");
			for (UserState userState : users) {
				sb.append(userState);
			}
			sb.append("]");
			return new String(sb);
		}
	}
	
	private static class CurrentStateMessage implements Serializable {
		private static final long serialVersionUID = 1L;
		private LocationState locations[];
		
		public CurrentStateMessage() {}
		public CurrentStateMessage(LocationState locations[]) {
			this.locations = locations;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("[CurrentStateMessage: ");
			for (LocationState locationState : locations) {
				sb.append(locationState);
			}
			sb.append("]");
			return new String(sb);
		}
	}
	
	private static class PresenceChangedMessage implements Serializable {
		private static final long serialVersionUID = 1L;
		public String location;
		public long serial;
		public Guid user;
		public int presence;
		
		public PresenceChangedMessage() {}
		public PresenceChangedMessage(String location, long serial, Guid user, int presence) {
			this.location = location;
			this.serial = serial;
			this.user = user;
			this.presence = presence;
		}
		
		@Override
		public String toString() {
			return "[PresenceChangedMessage: location=" + location + " serial=" + serial + " user=" + user + " presence=" + presence + "]";
		}
	}

	// ---- Member variables -----

	private String clusterName = CLUSTER_NAME;
	private Element config;
	private Channel channel;
	
	// servers is read from application threads, but only updated from the
	// thread handling incoming JGroups messages, so we can use a ConcurrentHashMap
	// for it rather than explicit synchronization. The application threads just need a snapshot
	// of the contents since they aren't doing a read-update.
	private Map<Address, ServerInfo> servers = new ConcurrentHashMap<Address, ServerInfo>();
	
	// localLocations is read and updated from application threads, so we synchronize on it whenever
	// we read or update it. We can, however, synchronize separately within the LocationInfo code
	// for subsequent updates of the contents since we never remove elements from the map.
	private Map<String, LocalLocationInfo> localLocations = new HashMap<String, LocalLocationInfo>();
	
	private ReceiveThread receiveThread;
	private ExecutorService notifyExecutor;

	// ---- PresenceServiceMBean methods -----
	
	public void setClusterName(String name) {
		this.clusterName = name;
	}
	
	public String getClusterName() {
		return clusterName;
	}
	
	public void setClusterConfig(Element element) {
		this.config = element;
	}

	// ---- Life-cycle methods -----
	
	static private PresenceService instance;
	
	@Override
	protected void startService() {
		logger.info("Starting PresenceService");

		try {
			JChannelFactory factory = new JChannelFactory(config);
			channel = factory.createChannel();
			channel.connect(clusterName);
		} catch (ChannelException e) {
			throw new RuntimeException("Failed to create JGroups channel", e);
		}
		
		receiveThread = new ReceiveThread();
		receiveThread.start();
		
		notifyExecutor = ThreadUtils.newSingleThreadExecutor("PresenceService-notify");
		
		instance = this;
		
		logger.info("Started PresenceService");
	}

	@Override
	protected void stopService() {
		logger.info("Stopping PresenceService");

		instance = null;
		
		channel.close();
		channel = null;
		
		notifyExecutor.shutdown();
		
		try {
			receiveThread.join();
		} catch (InterruptedException e) {
			// Shouldn't happen, just ignore
		}
		
		logger.info("Stopped PresenceService");
	}
	
	// ---- Public methods -----

	/**
	 * Return the global per-process singleton PresenceService object. We could
	 * have alternatively made this an @Service EJB and used a JNDI lookup to get the
	 * service, or put the PresenceService methods on PresenceServiceMBean and
	 * looked up the service via JMX, but this way is simplest. (Also, it's not
	 * clear how to have XML configuration for an @Service EJB.   
	 */
	public static PresenceService getInstance() {
		return instance;
	}

	/**
	 * Add a listener to changes of presence for a particular location; callbacks
	 * to the listener thread are made asynchronously from a different thread even
	 * when the modifications to presence happen because of changes in the local
	 * presence of a user.
	 * 
	 * @param location location to listen for changes to
	 * @param listener listener object that will be notified of changes.
	 */
	public void addListener(String location, PresenceListener listener) {
		LocalLocationInfo locationInfo = getLocalLocationInfo(location);
		locationInfo.addListener(listener);
	}

	/**
	 * Remove a listener previously added with addListener. Note that if a
	 * notification is already in progress, your listener could be invoked after the 
	 * call to removeListener, so you should be careful that doesn't result in serious failure. 
	 * 
	 * @param location location where the listener was listening to changes
	 * @param listener listener to remove
	 */
	public void removeListener(String location, PresenceListener listener) {
		LocalLocationInfo locationInfo = getLocalLocationInfo(location);
		locationInfo.removeListener(listener);
	}
	
	/**
	 * Get the presence of a user as observed by the current server. This reflects
	 * the up-to-the-moment presence on the local server, combined with a view
	 * of the presence of the user on other clusters of the server that might
	 * be slightly stale. (If you get a stale view of presence, you are guaranteed
	 * to get eventually a notification sent to a listener to the node, so a 
	 * reliable way of tracking presence is to first add a listener, then to check
	 * the presence with getPresence().)
	 * 
	 * @param location a string representing a logical location
	 * @param guid the user to check the presence of
	 * @return an integer representing the presence of the user. 0 means not present,
	 *    higher values indicate different levels of presence.
	 */
	public int getPresence(String location, Guid guid) {
		int result = 0;
		
		LocationInfo localLocation = peekLocalLocationInfo(location);
		if (localLocation != null)
			result = localLocation.getPresence(guid);
		
		for (ServerInfo server : servers.values()) {
			int serverPresence = server.getPresence(location, guid);
			if (serverPresence > result)
				result = serverPresence;
		}
		
		return result;
	}
	
	/**
	 * Gets a list of servers that a user is currently present on for the specified location.
	 * As with getPresence(), the result combines an instantaneous view of the local server
	 * with a possibly slightly stale of the situation on other servers.
	 * 
	 * @param location a string representing a logical location
	 * @param guid the user to check the presence of
	 * @param presence minimum presence that the user needs to have to count as present on
	 *    a server. This must be at least 1 to get a meaningful result: we don't track
	 *    users that have a presence of 0 and are thus *not* present.
	 * @return a list of servers that the specified user is currently on; the resulting
	 *    Address objects in the list can be cast to IpAddress objects as long as the
	 *    underlying group transport is IP multicast, as it is for Mugshot currently.
	 */
	public List<Address> getServers(String location, Guid guid, int presence) {
		List<Address> result = new ArrayList<Address>(); 
		
		LocationInfo localLocation = peekLocalLocationInfo(location);
		if (localLocation != null)
			result.add(channel.getLocalAddress());

		for (ServerInfo server : servers.values()) {
			int serverPresence = server.getPresence(location, guid);
			if (serverPresence >= presence)
				result.add(server.getAddress());
		}

		return result;
	}

	/**
	 * Gets a list of users that are currently present on for the specified location.
	 * As with getPresence(), the result combines an instantaneous view of the local server
	 * with a possibly slightly stale of the situation on other servers.
	 * 
	 * @param location a string representing a logical location
	 * @param presence minimum presence that a user needs to have to count as present on
	 *    a server. This must be at least 1 to get a meaningful result: we don't track
	 *    users that have a presence of 0 and are thus *not* present.
	 * @return the set of users that are present at the location 
	 */
	public Set<Guid> getPresentUsers(String location, int presence) {
		Set<Guid> result = new HashSet<Guid>();
		
		LocationInfo locationInfo = peekLocalLocationInfo(location);
		if (locationInfo != null)
			locationInfo.addPresentUsers(result, presence);
		
		for (ServerInfo server : servers.values()) {
			locationInfo = server.peekLocationInfo(location);
			if (locationInfo != null)
				locationInfo.addPresentUsers(result, presence);
		}
		
		return result;
	}
	
	/**
	 * Gets a list of users that are currently present on for the specified location on
	 * the local server.
	 * 
	 * @param location a string representing a logical location
	 * @param presence minimum presence that a user needs to have to count as present on
	 *    a server. This must be at least 1 to get a meaningful result: we don't track
	 *    users that have a presence of 0 and are thus *not* present.
	 * @return the set of users that are present at the location on the local server 
	 */
	public Set<Guid> getLocalPresentUsers(String location, int presence) {
		Set<Guid> result = new HashSet<Guid>();
		
		LocationInfo locationInfo = peekLocalLocationInfo(location);
		if (locationInfo != null)
			locationInfo.addPresentUsers(result, presence);
		
		return result;
	}

	/**
	 * Update the presence value of a user for this server. If there are any
	 * listeners for the location, they will be notified asynchronously
	 * from a different thread.
	 * 
	 * @param location a string representing a logical location
	 * @param guid the user to update the presence of
	 * @param localPresence the new presence value for the user at the location
	 */
	public void setLocalPresence(String location, Guid guid, int localPresence) {
		LocalLocationInfo locationInfo = getLocalLocationInfo(location);
		locationInfo.setPresence(guid, localPresence);
	}

	/**
	 * Clears all local presence information for all users; use with great caution
	 * if some other code portion might also be setting local presence on locations
	 * you don't know about. In particular, we can use this currently within the
	 * DumbHippo server because all presence is maintained by the Jive server,
	 * so when the Jive service is shut down, it can simply blank the local presence.
	 * 
	 * If we start maintaining users present by other means than XMPP we might
	 * want to introduce idea of merging multiple local "sub-servers" to get the
	 * public view of this server's state. Or we could use multiple PresenceServices,
	 * if it works to bind multiple times to the broadcast address, though there
	 * is obvious inefficiency there for local updates.
	 */
	public void clearLocalPresence() {
		Set<String> locations;
		synchronized (localLocations) {
			locations = new HashSet<String>(localLocations.keySet());
		}
		
		for (String location : locations) {
			peekLocalLocationInfo(location).clearPresence();
		}
	}
	
	// ---- private methods ----

	private LocalLocationInfo peekLocalLocationInfo(String location) {
		synchronized(localLocations) {
			return localLocations.get(location);
		}
	}
	
	private LocalLocationInfo getLocalLocationInfo(String location) {
		synchronized (localLocations) {
			LocalLocationInfo locationInfo = localLocations.get(location); 
			if (locationInfo == null) {
				locationInfo = new LocalLocationInfo(location);
				localLocations.put(location, locationInfo);
			}
			
			return locationInfo;
		}
	}
	
	private void scheduleNotification(String location, Guid guid) {
		LocalLocationInfo locationInfo = peekLocalLocationInfo(location);
		
		// What we want to ensure is that if someone calls
		//
		//    addListener(location, listener);
		//    getPresence(location, guid);
		//
		// Then any changes to the guid's presence which aren't reflected in
		// the result of getPresence() will be notified.
		//
		// As long as scheduleNotification is called after making the change
		// to the data model, then if locationInfo doesn't exist or doesn't 
		// have any listeners at this point, we have nothing to do.  
		
		if (locationInfo != null)
			locationInfo.scheduleNotification(guid);
	}
	
	private void scheduleNotification(String location, Set<Guid> guids) {
		LocalLocationInfo locationInfo = peekLocalLocationInfo(location);
		if (locationInfo != null)
			locationInfo.scheduleNotification(guids);
	}
	
	private void handleViewChange(View newView) {
		logger.debug("New view: {}", newView.toString());
		
		Set<Address> newMembers = new HashSet<Address>();

		for (Object o : newView.getMembers()) {
			Address address = (Address)o;
			if (!address.equals(channel.getLocalAddress()))
				newMembers.add(address);
		}
		
		for (Address address : newMembers) {
			if (!servers.containsKey(address)) {
				ServerInfo server = new ServerInfo(address);
				servers.put(address, server);
				
				RequestStateMessage message = new RequestStateMessage();
				// logger.debug("Sending {} to {}", message, address);
				
				try {
					channel.send(address, null, message);
				} catch (ChannelNotConnectedException e) {
					logger.error("Channel not connected when sending RequestStateMessage");
				} catch (ChannelClosedException e) {
					logger.warn("Channel closed when sending RequestStateMessage");
				}
			}
		}

		for (Iterator<Address> i = servers.keySet().iterator(); i.hasNext();) {
			Address address = i.next();
			if (!newMembers.contains(address)) {
				ServerInfo server = servers.get(address);
				i.remove();
				server.notifyAll();
			}
		}
	}

	private void handleRequestState(Address source, RequestStateMessage message) throws ChannelNotConnectedException, ChannelClosedException {
		// We can afford to be sloppy about exactly what state we respond with; 
		// it doesn't have to be consistent for all locations

		Set<String> locations;
		synchronized (localLocations) {
			locations = new HashSet<String>(localLocations.keySet());
		}
		
		LocationState locationStates[] = new LocationState[locations.size()];
		
		int i = 0;
		for (String location : locations) {
			LocalLocationInfo locationInfo = peekLocalLocationInfo(location);
			locationStates[i] = locationInfo.getState();
			i++;
		}
		CurrentStateMessage response = new CurrentStateMessage(locationStates);
		// logger.debug("Sending {} to {}", response, source);
		channel.send(source, null, response);
	}
	
	private void handleLocationState(ServerInfo server, LocationState locationState) {
		server.handleLocationState(locationState);
		for (UserState userState : locationState.users)
			scheduleNotification(locationState.location, userState.user);
	}
	
	private void handleCurrentState(Address source, CurrentStateMessage message) {
		ServerInfo server = servers.get(source);
		if (server == null)
			return;
		
		for (LocationState locationState : message.locations)
			handleLocationState(server, locationState);
		
		server.finishInitialState();
	}

	private void handlePresenceChanged(Address source, PresenceChangedMessage message) {
		ServerInfo server = servers.get(source);
		if (server == null)
			return;

		server.updatePresence(message.location, message.user, message.serial, message.presence);
		scheduleNotification(message.location, message.user);
	}

	public void handleMessage(Message message) throws ChannelNotConnectedException, ChannelClosedException {
		Object object;
		
		try {
		  	object = org.jgroups.util.Util.objectFromByteBuffer(message.getBuffer());
		} catch (Exception e) {
			logger.warn("Can't deserialize contents of Presence channel message");
			return; // ignore
		}
		
		// logger.debug("Received {} from {}", object, message.getSrc());
		
		if (object instanceof RequestStateMessage)
			handleRequestState(message.getSrc(), (RequestStateMessage)object);
		else if (object instanceof CurrentStateMessage)
			handleCurrentState(message.getSrc(), (CurrentStateMessage)object);
		else if (object instanceof PresenceChangedMessage) 
			handlePresenceChanged(message.getSrc(), (PresenceChangedMessage)object);
		else
			logger.warn("Unknown type of message received on Presence channel", object.getClass().getName());
			
	}
	
	private class ReceiveThread extends Thread {
		public ReceiveThread() {
			setName("PresenceService.ReceiveThread");
		}
		
		@Override
		public void run() {
			while (true) {
				try {
					Object object = channel.receive(-1);
					if (object instanceof Message) {
						handleMessage((Message) object);
					} else if (object instanceof View) {
						handleViewChange((View)object);
					}
				} catch (ChannelNotConnectedException e) {
					logger.error("Channel not connected"); // Shouldn't happen, but won't fix itself, so exit thread
					break;
				} catch (ChannelClosedException e) {
					break;
				} catch (TimeoutException e) {
					logger.error("Unexpected timeout waiting for PresenceService message", e);
				} catch (RuntimeException e) {
					logger.error("Exception dispatching PresenceService message", e);
				}
			}
		}		
	}
}
