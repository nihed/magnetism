package com.dumbhippo.jive;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.messenger.ClientSession;
import org.jivesoftware.messenger.SessionManagerListener;
import org.jivesoftware.messenger.user.UserNotFoundException;
import org.jivesoftware.util.Log;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

public class PresenceMonitor implements SessionManagerListener {

	private Map<String, Integer> sessionCounts;
	
	private MessengerGlueRemote getGlue() {
		// don't cache this for now, so jive will be robust against jboss redeploy,
		// and so we're thread safe automatically
		return EJBUtil.defaultLookup(MessengerGlueRemote.class);
	}
	
	public PresenceMonitor() {
		sessionCounts = new HashMap<String, Integer>();
	}

	private void onSessionCountChange(ClientSession session, int increment) {
		int oldCount, newCount;
		String user;
		
		try {
			user = session.getUsername();
		} catch (UserNotFoundException e) {
			Log.error("should not happen, couldn't look up username in " + PresenceMonitor.class.getCanonicalName(), e);
			// shouldn't happen afaik since we are only called on non-anon sessions, but don't throw from here
			return;
		}
		
		synchronized (sessionCounts) {			
			Integer tmp = sessionCounts.get(user);
			if (tmp == null)
				oldCount = 0;
			else
				oldCount = tmp;
			
			if (oldCount == 0) {
				if (increment > 0) {
					newCount = increment;
				} else {
					Log.error("should not happen, decrementing client count when there was no client count for user " + user);
					newCount = oldCount;
				}
			} else {
				newCount = oldCount + increment;
			}
			
			if (newCount < 0) {
				Log.error("Bug! decremented user session count below 0, old count " + oldCount + " increment " + increment);
				newCount = 0; // "fix it"
			}
				
			if (oldCount != newCount)
				sessionCounts.put(user, newCount);
		}
			
		Log.debug("User " + user + " now has " + newCount + " sessions was " + oldCount);
		
		// Very deliberately outside synchronized block
		if (oldCount > 0 && newCount == 0) {
			getGlue().onUserUnavailable(user);
		} else if (oldCount == 0 && newCount > 0) {
			getGlue().onUserAvailable(user);
		}
	}
	
	
	public void onClientSessionAvailable(ClientSession session) {
		onSessionCountChange(session, 1);
	}

	public void onClientSessionUnavailable(ClientSession session) {
		onSessionCountChange(session, -1);
	}
}
