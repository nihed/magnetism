package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * This interface is implemented by callback listeners for changes to presence at a location.
 * 
 * @see PresenceMonitor
 * 
 * @author otaylor
 */
public interface PresenceListener {
	/**
	 * Something has changed about the presence of a particular user at the location.
	 * This might be the aggregate presence level of the user, or the set of servers
	 * that the user is on. It's also possible to get this signal when nothing has
	 * changed; to find out the new presence of the user, call PresenceMonitor.getPresence() 
	 * 
	 * The guarantees for delivery of this notification are weak; if the presence
	 * changes twice in succession, you might get two notifications or you might get
	 * only a single notification. Two subsequent calls to getPresence() may return
	 * different values for the presence without an intervening callback. What you
	 * are guaranteed is that if you read the presence value and then it then changes,
	 * you will eventually get a notification callback. That guarantee is sufficient
	 * to make sure that you can keep track of the correct current value during stable
	 * periods when nothing is changing. 
	 * 
	 * When starting monitoring a value, you should call PresenceMonitor.addListener() first, 
	 * then read the state to avoid race conditions. 
	 * 
	 * @param userGuid the GUID for the user whose presence changed
	 */
	void presenceChanged(Guid userGuid);
}
