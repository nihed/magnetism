package com.dumbhippo.jive;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.dumbhippo.jive.rooms.RoomUserStatus;
import com.dumbhippo.server.ChatRoomKind;
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
	// or we'll be timed out. Also, we want to be less than the JBoss
	// timeout for remoting sockets, which is 60 seconds, or we'll
	// keep on recreating new connections to the server.
	private static final long PING_INTERVAL = 30 * 1000; // 50 seconds
	
	// How long to wait before retrying if we need to reconnect to 
	// the application server
	private static final long RETRY_SLEEP = 10 * 1000; // 10 seconds

	private class NotifierThread extends Thread {
		private void sendNotification(MessengerGlueRemote glue, String serverIdentifier, Notification notification) throws NoSuchServerException {
			if (notification instanceof RoomNotification) {
				RoomNotification roomNotification = (RoomNotification) notification;
				if (roomNotification.status == RoomUserStatus.NONMEMBER) {
					Log.debug("Sending notification of user leaving room");
					glue.onRoomUserUnavailable(serverIdentifier, roomNotification.kind, roomNotification.room, notification.user);
				} else {
					Log.debug("Sending notification of user joining room");
					glue.onRoomUserAvailable(serverIdentifier, roomNotification.kind, roomNotification.room, notification.user, roomNotification.status == RoomUserStatus.PARTICIPANT);
				}
			} else if (notification instanceof UserNotification) {
				UserNotification userNotification = (UserNotification) notification;
				if (userNotification.available) {
					Log.debug("Sending user available notification");
					glue.onUserAvailable(serverIdentifier, notification.user);
				} else {
					Log.debug("Sending user unavailable notification");
					glue.onUserUnavailable(serverIdentifier, notification.user);
				}
			} else if (notification instanceof ResourceConnectedNotification) {
				ResourceConnectedNotification resNotification = (ResourceConnectedNotification) notification;
				glue.onResourceConnected(serverIdentifier, resNotification.user);
			}
		}
		
		@Override
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
			MessengerGlueRemote glue = null;			
		
			try {
				while (true) {
					try {
						if (glue == null)
							glue = getGlue();
	
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
									if (info.sessionCount > 0) {
										notifications.add(new UserNotification(entry.getKey(), true));
										notifications.add(new ResourceConnectedNotification(entry.getKey()));
									}
									if (info.rooms != null) {
										for (Entry<String, RoomEntry> roomEntry : info.rooms.entrySet())
											notifications.add(new RoomNotification(entry.getKey(), roomEntry.getValue().getKind(), 
													roomEntry.getKey(), roomEntry.getValue().getStatus()));
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
						glue = null;
						Thread.sleep(RETRY_SLEEP);
					} catch (EJBException e) {
						Log.debug("Got exception communicating with the application server, restarting presence tracking", e);
						serverIdentifier = null;
						glue = null;
						Thread.sleep(RETRY_SLEEP);
					} catch (UndeclaredThrowableException e) {
						// We can get this as JBoss is restarting; if you find you are adding more
						// similar exceptions this code should probably be switched to just 
						// catch Throwable. It's a bad thing if this thread dies.
						Log.debug("Server threw an undeclared exception - probably an app server bug; restarting presence tracking", e);
						serverIdentifier = null;
						glue = null;
						Thread.sleep(RETRY_SLEEP);
					} catch (NoSuchServerException e) {
						Log.debug("Application server doesn't recognize us, restarting presence tracking");
						serverIdentifier = null;
						glue = null;
						Thread.sleep(RETRY_SLEEP);
					}
				}
			} catch (InterruptedException e) {
				// all done
			}
		}
	}
	
	private static class RoomEntry {
		private ChatRoomKind kind;
		private String name;
		private RoomUserStatus status;
		
		RoomEntry(ChatRoomKind kind, String name, RoomUserStatus status) {
			this.kind = kind;
			this.name = name;
			this.status = status;
		}
		
		ChatRoomKind getKind() {
			return kind;
		}
		
		String getName() {
			return name;
		}
		
		RoomUserStatus getStatus() {
			return status;
		}
	}
	
	private static class UserInfo {
		private int sessionCount;
		private Map<String, RoomEntry> rooms;
		
		void addRoom(ChatRoomKind kind, String room, RoomUserStatus status) {
			if (rooms == null)
				rooms = new HashMap<String, RoomEntry>();
			rooms.put(room, new RoomEntry(kind, room, status));
		}
		
		void removeRoom(ChatRoomKind kind, String room) {
			rooms.remove(room);
		}
	}

	private static abstract class Notification {
		String user;

		Notification(String user) {
			this.user = user;
		}
	}
	
	private static abstract class PresenceNotification extends Notification {
		boolean available;
		
		PresenceNotification(String user, boolean available) {
			super(user);
			this.available = available;
		}
	}
	
	private static class UserNotification extends PresenceNotification {
		UserNotification(String userName, boolean available) {
			super(userName, available);
		}
	};
	
	private static class RoomNotification extends Notification {
		private ChatRoomKind kind;
		private String room;
		private RoomUserStatus status;
		
		RoomNotification(String user, ChatRoomKind kind, String room, RoomUserStatus status) {
			super(user);
			this.kind = kind;
			this.room = room;
			this.status = status;
		}
	}
	
	private static class ResourceConnectedNotification extends Notification {
		ResourceConnectedNotification(String userName) {
			super(userName);
		}		
	}
	
	private MessengerGlueRemote getGlue() throws NamingException {
		// don't cache this for now, so jive will be robust against jboss redeploy,
		// and so we're thread safe automatically (note that our notification queue
		// calls this from another thread)
		return EJBUtil.defaultLookupRemoteChecked(MessengerGlueRemote.class);
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
		
		// TODO remove this later when we don't have a special admin user
		if (user.equals("admin"))
			return;
		
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
				notificationQueue.add(new UserNotification(user, false));
			} else if (oldCount == 0 && newCount > 0) {
				notificationQueue.add(new UserNotification(user, true));
			}
			if (newCount > oldCount) {
				notificationQueue.add(new ResourceConnectedNotification(user));
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
	
	public void onRoomUserAvailable(ChatRoomKind kind, String room, String user, RoomUserStatus status) {
		synchronized (userInfo) {
			UserInfo info = userInfo.get(user);
			if (info == null) {
				Log.warn("Got chat room presence for unavailable user " + user);
				return;
			}
			
			info.addRoom(kind, room, status);
			
			notificationQueue.add(new RoomNotification(user, kind, room, status));
		}
	}
	
	public void onRoomUserUnavailable(ChatRoomKind kind, String room, String user) {
		synchronized (userInfo) {
			UserInfo info = userInfo.get(user);
			if (info == null)
				return;
			
			info.removeRoom(kind, room);
			
			notificationQueue.add(new RoomNotification(user, kind, room, RoomUserStatus.NONMEMBER));
		}
	}
}
