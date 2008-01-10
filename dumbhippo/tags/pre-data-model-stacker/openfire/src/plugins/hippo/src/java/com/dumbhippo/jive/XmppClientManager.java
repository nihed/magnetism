package com.dumbhippo.jive;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jivesoftware.openfire.SessionManagerListener;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;

import com.dumbhippo.Site;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.server.MessengerGlue;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxRunnable;
import com.dumbhippo.tx.TxUtils;

public class XmppClientManager implements SessionManagerListener {
	private static XmppClientManager instance;
	ExecutorService senderPool = ThreadUtils.newCachedThreadPool("XmppClient-sender-pool");
	private Map<JID, XmppClient> clients = new HashMap<JID, XmppClient>();
	private Map<Guid, UserInfo> userInfo = new HashMap<Guid, UserInfo>();
	private ExecutorService executor;
	
	private static class UserInfo {
		private int sessionCount;
	}

	public XmppClientManager() {
		// It might be better to use a multiple-thread executor, since the work done when a
		// resource connects is potentially parallelizable. On the other hand, using
		// a single thread here will minimize server load during restart, which is a good
		// thing.
		executor = ThreadUtils.newSingleThreadExecutor("PresenceMonitor-notification");
		
		instance = this;
	}

	/**
	 * @return the static singleton instance of this class. May be null during startup
	 *  or shutdown, but otherwise will always exist.
	 */
	static public XmppClientManager getInstance() {
		return instance;
	}
	
	public void shutdown() {
		senderPool.shutdown();
		executor.shutdown();
		
		instance = null;
	}
	
	public void addClientSender(final XmppClient client) {
		senderPool.execute(new Runnable() {
			public void run() {
				client.processPackets();
			}
		});
	}
	
	private Guid getUserId(ClientSession session) {
		final String user;

		try {
			user = session.getUsername();
		} catch (UserNotFoundException e) {
			Log.error("should not happen, couldn't look up username in " + XmppClientManager.class.getCanonicalName(), e);
			// shouldn't happen afaik since we are only called on non-anon sessions, but don't throw from here
			return null;
		}
		
		// TODO remove this later when we don't have a special admin user
		if (user.equals("admin"))
			return null;
		
		try {
			return Guid.parseJabberId(user);
		} catch (ParseException e) {
			Log.error("Can't parse username as a guid");
			return null;
		}
	}
	
	private void onSessionCountChange(final Guid userId, int increment) {
		int oldCount, newCount;
		
		Log.debug("User '" + userId + "' session count incrementing by " + increment);
		
		synchronized (userInfo) {			
			UserInfo info = userInfo.get(userId);
			if (info == null) {
				info = new UserInfo();
				userInfo.put(userId, info);
			}
			
			oldCount = info.sessionCount;
			newCount = oldCount + increment;
			if (newCount < 0) {
				Log.error("Bug! decremented user session count below 0, old count " + oldCount + " increment " + increment);
				newCount = 0; // "fix it"
			}
			Log.debug("User " + userId + " now has " + newCount + " sessions was " + oldCount);
		
			info.sessionCount = newCount;
			
			PresenceService presenceService = PresenceService.getInstance();
			
			final boolean wasAlreadyConnected = presenceService.getPresence("/users", userId) > 0;
			
			// We are holding a lock on the userInfo object and possibly locks inside
			// Wildfire, so we have to be careful not to do database operations synchronously, 
			// since that could result in deadlocks. updateLoginDate() and updateLogoutDate() 
			// can be safely reversed since we pass in the login/logout timestamps, so we can do
			// them async without worrying about reordering.
			//
			// On the other hand, we must do PresenceService updates synchronously,
			// but that isn't an issue since PresenceService only locks temporarily
			// for data structure integrity and never blocks otherwise.
			
			final Date timestamp = new Date();
			
			if (oldCount > 0 && newCount == 0) {
				presenceService.setLocalPresence("/users", userId, 0);
				
				executor.execute(new Runnable() {
					public void run() {
						MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
						glue.updateLogoutDate(userId, timestamp);
					}
				});
			} else if (oldCount == 0 && newCount > 0) {
				presenceService.setLocalPresence("/users", userId, 1);
			}
			
			// We potentially do lots of work when a resource connects, including sending
			// messages to that user, so we don't want to block the user connecting and possibly 
			// cause reentrancy issues.
			if (newCount > oldCount) {
				executor.execute(new Runnable() {
					public void run() {
						TxUtils.runInTransaction(new TxRunnable() {
							public void run() throws RetryException {
								DataService.getModel().initializeReadWriteSession(new UserViewpoint(userId, Site.XMPP));
								
								MessengerGlue glue = EJBUtil.defaultLookup(MessengerGlue.class);
								glue.updateLoginDate(userId, timestamp);
								glue.sendConnectedResourceNotifications(userId, wasAlreadyConnected);
							}
						});
					}
				});
			}
		}
	}
	
	
	public void onClientSessionAvailable(ClientSession session) {
		Log.debug("Client session now available: " + session.getStreamID());

		Guid userId = getUserId(session);
		if (userId == null)
			return;
		
		XmppClient client = new XmppClient(this, DataService.getModel(), session, userId);
		synchronized(clients) {
			clients.put(session.getAddress(), client);
		}
		
		onSessionCountChange(userId, 1);
	}

	public void onClientSessionUnavailable(ClientSession session) {
		Log.debug("Client session now unavailable: " + session.getStreamID());

		Guid userId = getUserId(session);
		if (userId == null)
			return;

		onSessionCountChange(userId, -1);
		
		XmppClient client;
		synchronized(clients) {
			client = clients.remove(session.getAddress());
		}
		client.close();
	}

	public XmppClient getClient(JID from) {
		synchronized(clients) {
			return clients.get(from);
		}
	}
}
