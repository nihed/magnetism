package org.jivesoftware.wildfire;

/**
 * Listener to global session-related events, e.g. watching as sessions come and go.
 * 
 * @author hp
 *
 */
public interface SessionManagerListener {
	/**
	 * Called when a client session becomes available to other clients.
	 * 
	 * @param session the session that's now available
	 */
	public void onClientSessionAvailable(ClientSession session);
	/**
	 * Called when a client session becomes unavailable to other clients.
	 * 
	 * @param session the session that's now unavailable
	 */
	public void onClientSessionUnavailable(ClientSession session);
}
