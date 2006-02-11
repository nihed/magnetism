package com.dumbhippo.live;

import java.util.HashSet;
import java.util.Set;

import com.dumbhippo.identity20.Guid;

/**
 * Represents an instance of a connection of a Jabber server to the
 * application server. All presence information for that Jabber server
 * is proxied through here, so if we stop being pinged by the Jabber
 * server we can reliably mark all the users from that Jabber server
 * as no longer present. (Presence is reference counted inside
 * LiveState, so it works if we get a reconnection from a restarted
 * Jabber server before the old one times out.)
 * 
 * @author otaylor
 */
public class LiveXmppServer implements Ageable {
	private LiveState state;
	private Set<Guid> availableUsers;
	private int cacheAge;
	private String serverIdentifier;
	
	LiveXmppServer(LiveState state) {
		this.state = state;
		availableUsers = new HashSet<Guid>();
		
		serverIdentifier = Guid.createNew().toString();
	}
	
	/**
	 * Get this server instances unique cookie
	 * 
	 * @return a unique cookie for this instance of the server;
	 *  the Jabber server passes this token back with subsequent
	 *  presence information or pings.
	 */
	public String getServerIdentifier() {
		return serverIdentifier;
	}
	
	/**
	 * Mark a user as available for this server. Called by the
	 * Jabber server glue code.
	 * 
	 * @param userId the user who is now present
	 */
	public void userAvailable(Guid userId) {
		synchronized (state) {
			if (!availableUsers.contains(userId)) {
				availableUsers.add(userId);
				state.userAvailable(userId);
			}
		}
	}
	
	/**
	 * Mark a user as unavailable for this server. Called by the
	 * Jabber server glue code.
	 * 
	 * @param userId the user who is no longer present
	 */
	public void userUnavailable(Guid userId) {
		synchronized (state) {
			if (availableUsers.contains(userId)) {
				availableUsers.remove(userId);
				state.userUnavailable(userId);
			}
		}
	}
	
	public void postPresenceChange(Guid postId, Guid userId, boolean present) {
		state.postPresenceChange(postId, userId, present);
	}
	
	/**
	 * Keeps an LiveXmppServer object alive, cached, and referencing
	 * it's users. This must be called within 
	 * LiveState.CLEANER_INTERVAL * LiveState.MAX_XMPP_SERVER_CACHE_AGE 
	 * or the object will be discarded. (Even if otherwise referenced,
	 * this is to avoid leaked references causing problems.)
	 */
	public void ping() {
		synchronized(state) {
			setCacheAge(0);
		}
	}	
	
	/********************************************************************/
	
	public int getCacheAge() {
		return cacheAge;
	}

	public void setCacheAge(int age) {
		this.cacheAge = age;
	}
	
	public void discard() {
		for (Guid userId : availableUsers) {
			state.userUnavailable(userId);
		}
		availableUsers = null;
	}
}
