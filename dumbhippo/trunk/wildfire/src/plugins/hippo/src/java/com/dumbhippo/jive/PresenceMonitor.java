package com.dumbhippo.jive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.ejb.EJBException;
import javax.naming.NamingException;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.SessionManagerListener;
import org.jivesoftware.wildfire.user.UserNotFoundException;

import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.MessengerGlueRemote.NoSuchServerException;
import com.dumbhippo.server.util.EJBUtil;

public class PresenceMonitor implements SessionManagerListener {

	private Map<String, UserInfo> userInfo;
	private BlockingQueue<Notification> notificationQueue;
	private NotifierThread notifierThread;
	
	private String serverIdentifier;
	
	// Time between pings. Must be less than 
	// LiveState.CLEANER_INTERVAL * LiveState.MAX_XMPP_SERVER_CACHE_AGE
	// or we'll be timed out.
	private static final long PING_INTERVAL = 60 * 1000; // 1 minute
	
	// How long to wait before retrying if we need to reconnect to 
	// the application server
	private static final long RETRY_SLEEP = 10 * 1000; // 10 seconds

	private class NotifierThread extends Thread {
		private void sendNotification(MessengerGlueRemote glue, String serverIdentifier, Notification notification) throws NoSuchServerException {
			if (notification.room != null) {
				if (notification.available) {
					Log.debug("Sending notification of user joining room");
					glue.onRoomUserAvailable(serverIdentifier, notification.room, notification.user);
				} else {
					Log.debug("Sending notification of user leaving room");
					glue.onRoomUserUnavailable(serverIdentifier, notification.room, notification.user);
				}
			} else {
				if (notification.available) {
					Log.debug("Sending user available notification");
					glue.onUserAvailable(serverIdentifier, notification.user);
				} else {
					Log.debug("Sending user unavailable notification");
					glue.onUserUnavailable(serverIdentifier, notification.user);
				}
			}		
		}
		
		public void run() {
			// This thread is responsible for keeping the application server 
			// notified of the current set of present users. Three things are 
			// necessary for this; first, we need to register with the
			// application server and provide the current set of users; then
			// we feed the application server with incremental changes. 
			// Finally we have to periodically ping the application server
			// so it knows we haven't died.
			//
			// If we lose the connection to the server for any reason - 
			// it has been started, there is a network problem, or the
			// server times us out despite our efforts to ping it, then we
			// have to start over from scratch with registration.
			
			long lastPingTime = -1;
		
			try {
				while (true) {
					try {
						MessengerGlueRemote glue = getGlue();
	
						if (serverIdentifier == null) {
							Log.debug("Registering with application server");
							
							// Not yet registered; register and send all users
							// We need to atomically clear any existing items in the
							// queue and send the current set of users. However,
							// we don't want to block the connection while sending
							// out the initial notifications, so we use a temporary
							// array.
							List<Notification> notifications = new ArrayList<Notification>();
							
							synchronized(userInfo) {
								notificationQueue.clear();
								
								for (Entry<String, UserInfo> entry : userInfo.entrySet()) {
									UserInfo info = entry.getValue();
									if (info.sessionCount > 0)
										notifications.add(new Notification(entry.getKey(), true));
									if (info.rooms != null) {
										for (String room : info.rooms)
											notifications.add(new Notification(room, entry.getKey(), true));
									}
								}
							}

							long now = lastPingTime = System.currentTimeMillis();
							String serverIdentifier = glue.serverStartup(now);
							setServerIdentifier(serverIdentifier);
							
							for (Notification notification : notifications) {
								sendNotification(glue, serverIdentifier, notification);
							}
						} else {
							long now = System.currentTimeMillis();
	
							// Ping if necessary
							if (now >= lastPingTime + PING_INTERVAL) {
								glue.serverPing(serverIdentifier);
								lastPingTime = now;
							}
							
							// Wait until it's time to ping or until we get another notification
							long sleepTime = (lastPingTime + PING_INTERVAL) - now;
							Notification notification = notificationQueue.poll(sleepTime, TimeUnit.MILLISECONDS);
							if (notification != null) {
								sendNotification(glue, serverIdentifier, notification);
							}
						}
					} catch (NamingException e) {
						Log.debug("Couldn't get handle to MessengerGlue object, restarting presence tracking");
						serverIdentifier = null;
						Thread.sleep(RETRY_SLEEP);
					} catch (EJBException e) {
						Log.debug("Got exception communicating with the application server, restarting presence tracking", e);
						serverIdentifier = null;
						Thread.sleep(RETRY_SLEEP);
					} catch (NoSuchServerException e) {
						Log.debug("Application server doesn't recognize us, restarting presence tracking");
						serverIdentifier = null;
						Thread.sleep(RETRY_SLEEP);
					}
				}
			} catch (InterruptedException e) {
				// all done
			}
		}
	}
	
	private static class UserInfo {
		private int sessionCount;
		private Set<String> rooms;
		
		void addRoom(String room) {
			if (rooms == null)
				rooms = new HashSet<String>();
			rooms.add(room);
		}
		
		void removeRoom(String room) {
			rooms.remove(room);
		}
	}

	private static class Notification {
		private String room;
		private String user;
		private boolean available;

		// Global presence
		Notification(String user, boolean available) {
			this.user = user;
			this.available = available;
		}

		// Presence in a chat room
		Notification(String room, String user, boolean available) {
			this.room = room;
			this.user = user;
			this.available = available;
		}
	}
	
	private MessengerGlueRemote getGlue() throws NamingException {
		// don't cache this for now, so jive will be robust against jboss redeploy,
		// and so we're thread safe automatically (note that our notification queue
		// calls this from another thread)
		return EJBUtil.defaultLookupChecked(MessengerGlueRemote.class);
	}

	private void setServerIdentifier(String serverIdentifier) {	
		Log.debug("Registering as " + serverIdentifier);			
		this.serverIdentifier = serverIdentifier;
	}
	
	public PresenceMonitor() {
		userInfo = new HashMap<String, UserInfo>();
		notificationQueue = new LinkedBlockingQueue<Notification>();
		notifierThread = new NotifierThread();
		notifierThread.start();
	}

	public void shutdown() {
		notifierThread.interrupt();
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
		
		synchronized (userInfo) {			
			UserInfo info = userInfo.get(user);
			if (info == null) {
				info = new UserInfo();
				userInfo.put(user, info);
			}
			
			oldCount = info.sessionCount;
			newCount = oldCount + increment;
			if (newCount < 0) {
				Log.error("Bug! decremented user session count below 0, old count " + oldCount + " increment " + increment);
				newCount = 0; // "fix it"
			}
			Log.debug("User " + user + " now has " + newCount + " sessions was " + oldCount);
		
			info.sessionCount = newCount;
			
			// We queue this stuff so we aren't invoking some database operation in jboss
			// with the lock held and blocking all new clients in the meantime
			if (oldCount > 0 && newCount == 0) {
				notificationQueue.add(new Notification(user, false));
			} else if (oldCount == 0 && newCount > 0) {
				notificationQueue.add(new Notification(user, true));
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
	
	public void onRoomUserAvailable(String room, String user) {
		synchronized (userInfo) {
			UserInfo info = userInfo.get(user);
			if (info == null) {
				Log.warn("Got chat room presence for unavailable user " + user);
				return;
			}
			
			info.addRoom(room);
			
			notificationQueue.add(new Notification(room, user, true));
		}
	}
	
	public void onRoomUserUnavailable(String room, String user) {
		synchronized (userInfo) {
			UserInfo info = userInfo.get(user);
			if (info == null)
				return;
			
			info.removeRoom(room);
			
			notificationQueue.add(new Notification(room, user, false));
		}
	}
}
