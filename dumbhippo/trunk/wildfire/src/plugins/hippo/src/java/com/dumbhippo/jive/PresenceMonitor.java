package com.dumbhippo.jive;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.SessionManagerListener;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.jivesoftware.util.Log;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

public class PresenceMonitor implements SessionManagerListener {

	private Map<String, Integer> sessionCounts;
	private ExecutorService notificationQueue;
	
	private class Notification implements Runnable {

		private String user;
		private boolean available;

		Notification(String user, boolean available) {
			this.user = user;
			this.available = available;
		}
		
		public void run() {
			Log.debug("Running notification that user " + user + " available = " + available);
			if (available) {
				getGlue().onUserAvailable(user);
			} else {
				getGlue().onUserUnavailable(user);
			}
		}
	}
	
	private MessengerGlueRemote getGlue() {
		// don't cache this for now, so jive will be robust against jboss redeploy,
		// and so we're thread safe automatically (note that our notification queue
		// calls this from another thread)
		return EJBUtil.defaultLookup(MessengerGlueRemote.class);
	}
	
	public PresenceMonitor() {
		sessionCounts = new HashMap<String, Integer>();
		notificationQueue = Executors.newSingleThreadExecutor();
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
		
		Log.debug("User '" + user + "' session count incrementing by " + increment);
		
		synchronized (sessionCounts) {			
			Integer tmp = sessionCounts.get(user);
			if (tmp == null)
				oldCount = 0;
			else
				oldCount = tmp;
			
			newCount = oldCount + increment;
			
			if (newCount < 0) {
				Log.error("Bug! decremented user session count below 0, old count " + oldCount + " increment " + increment);
				newCount = 0; // "fix it"
			}
				
			if (oldCount != newCount)
				sessionCounts.put(user, newCount);
			
			Log.debug("User " + user + " now has " + newCount + " sessions was " + oldCount);
		
			// We queue this stuff so we aren't invoking some database operation in jboss
			// with the lock held and blocking all new clients in the meantime
			if (oldCount > 0 && newCount == 0) {
				notificationQueue.execute(new Notification(user, false));
			} else if (oldCount == 0 && newCount > 0) {
				notificationQueue.execute(new Notification(user, true));
			}
		}
	}
	
	
	public void onClientSessionAvailable(ClientSession session) {
		Log.debug("Client session now available: " + session.getStreamID());
		onSessionCountChange(session, 1);
	}

	public void onClientSessionUnavailable(ClientSession session) {
		Log.debug("Client session now unavailable: " + session.getStreamID());
		onSessionCountChange(session, -1);
	}
}
