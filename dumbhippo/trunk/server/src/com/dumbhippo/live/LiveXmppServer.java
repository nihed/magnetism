package com.dumbhippo.live;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
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
public class LiveXmppServer {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveXmppServer.class);

	// This is the same as com.dumbhippo.jive.rooms.RoomUserStatus; we 
	// cut-and-paste rather than sharing to keep the build simple
	private enum RoomUserStatus {
		NONMEMBER,     // Not looking at the post
		VISITOR,       // Viewing the post
		PARTICIPANT    // Chatting in the post's chat room
	}

	private LiveState state;
	private String serverIdentifier;
	
	private Map<Guid, UserInfo> availableUsers;

	// We rely on the fact that all modifications to the state of a single
	// LiveXmppServer via calls to MessengerGlueRemote are serialized because
	// they are ultimately being made from a single thread in MessageProducer.
	// 
	// So the only thing we have to do to protect our internal data structures
	// is to make sure:
	//
	//  - We don't start discarding a timed-out LiveXmppServer while there
	//    are updates in progress
	//  - Once we've started discarding a timed-out LiveXmppServer, we don't
	//    begin any more updates
	//
	// The following variables take care of that. (Actually, since the calls
	// from MessengerGlueRemote are serialized, updateCount should never be
	// greater than 1; it's just a count for notational convenience.)
	private boolean discarded;
	private int updateCount;

	// Access to cacheAge needs to be synchronized because we reset it to 0
	// when we get called from MessengerGlueBean, but we increase it and check it from the
	// "cleaner" thread of LiveState
	private int cacheAge;

	private static class UserInfo {
		Guid userId;
		Map<Guid, RoomUserStatus> postRooms;
		
		UserInfo(Guid userId) {
			this.userId = userId;
		}
		
		void addPostRoom(Guid postId, boolean participant) {
			if (postRooms == null)
				postRooms = new HashMap<Guid, RoomUserStatus>();
			
			postRooms.put(postId, participant ? RoomUserStatus.PARTICIPANT : RoomUserStatus.VISITOR);
		}
		
		RoomUserStatus removePostRoom(Guid postId) {
			if (postRooms != null)
				return postRooms.remove(postId);
			else
				return RoomUserStatus.NONMEMBER;
		}
		
		RoomUserStatus getPostRoomStatus(Guid postId) {
			if (postRooms == null)
				return RoomUserStatus.NONMEMBER;
			RoomUserStatus result = postRooms.get(postId);
			if (result == null)
				return RoomUserStatus.NONMEMBER;
			else
				return result;
		}
		
		void sendUnavailable(LiveState state) {
			if (postRooms != null) {
				for (Entry<Guid,RoomUserStatus> entry : postRooms.entrySet())
					state.postRoomUserUnavailable(entry.getKey(), userId, entry.getValue() == RoomUserStatus.PARTICIPANT);
			}
			
			state.userUnavailable(userId);
		}
	}
	
	LiveXmppServer(LiveState state) {
		this.state = state;
		availableUsers = new HashMap<Guid, UserInfo>();		
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
		if (!startUpdate())
			return;
		
		try {
			if (!availableUsers.containsKey(userId)) {
				availableUsers.put(userId, new UserInfo(userId));
			}
			
			state.userAvailable(userId);
		} finally {
			stopUpdate();
		}
	}
	
	/**
	 * Mark a user as unavailable for this server. Called by the
	 * Jabber server glue code.
	 * 
	 * @param userId the user who is no longer present
	 */
	public void userUnavailable(Guid userId) {
		if (!startUpdate())
			return;
		
		try {
			UserInfo info = availableUsers.get(userId);
			if (info != null) {
				availableUsers.remove(userId);
				info.sendUnavailable(state);
			}
		} finally {
			stopUpdate();
		}
	}
	
	/**
	 * Mark that a user has joined the chat room for a post. Must
	 * be called after userAvailable for that user. Called by the Jabber 
	 * server glue code.
	 * 
	 * @param postId the post ID for the chat room 
	 * @param userId the user who has joined the room
	 * @param participant whether the user is a participant (chatting about the post)
	 *        as compared to a guest (just viewing the post). If a user switches
	 *        from participant to user or vice-versa, postRoomUserUnavailable.
	 *        must be called in between.
	 */
	public void postRoomUserAvailable(Guid postId, Guid userId, boolean participant) {
		if (!startUpdate())
			return;
		
		try {
			UserInfo info = availableUsers.get(userId);
			if (info == null) {
				logger.warn("onPostRoomUserAvailable called for an unavailable user");
				return;
			}
			if (info.getPostRoomStatus(postId) != RoomUserStatus.NONMEMBER) {
				logger.warn("onPostRoomUserAvailable called for user already in room");
				return;
			}
				
			info.addPostRoom(postId, participant);
			state.postRoomUserAvailable(postId, userId, participant);
		} finally {
			stopUpdate();
		}
	}

	/**
	 * Mark that a user has left the chat room for a post.
	 * Called by the Jabber server glue code.
	 * 
	 * @param postId the post ID for the chat room 
	 * @param userId the user who has joined the room
	 */
	public void postRoomUserUnavailable(Guid postId, Guid userId) {
		if (!startUpdate())
			return;
		
		try {
			UserInfo info = availableUsers.get(userId);
			if (info == null)
				return;
		
			RoomUserStatus oldStatus = info.removePostRoom(postId);
			if (oldStatus != RoomUserStatus.NONMEMBER)
				state.postRoomUserUnavailable(postId, userId, oldStatus == RoomUserStatus.PARTICIPANT);
		} finally {
			stopUpdate();
		}
	}
	
	/**
	 * Notify that a resource has connected.
	 * 
	 * @param guid Guid associated with a user
	 */
	public void resourceConnected(Guid guid) {
		// It's harmless to send extra notifications, so don't bother checking
		// whether we have already been discarded
		state.resendAllNotifications(guid);
	}	
	
	/**
	 * Keeps an LiveXmppServer object alive, cached, and referencing
	 * it's users. This must be called within 
	 * LiveState.CLEANER_INTERVAL * LiveState.MAX_XMPP_SERVER_CACHE_AGE 
	 * or the object will be discarded. (Even if otherwise referenced,
	 * this is to avoid leaked references causing problems.)
	 */
	public synchronized void ping() {
		cacheAge = 0;
	}	
	
	public synchronized int increaseCacheAge() {
		return ++cacheAge;
	}

	private synchronized boolean startUpdate() {
		if (discarded)
			return false;
		
		updateCount++;
		
		return true;
	}
	
	private synchronized void stopUpdate() {
		updateCount--;
		
		if (updateCount == 0 && discarded)
			notifyAll();
	}
	
	/**
	 * Called when this LiveXmppServer is timed out due to lack of pinging.
	 */
	public void discard() throws InterruptedException {
		synchronized (this) {
			// Signal not to begin any more updates
			discarded = true;
			
			// Wait for in-progress updates to finish
			while (updateCount > 0)
				wait();
		}
		
		for (UserInfo info : availableUsers.values()) {
			info.sendUnavailable(state);
		}
	}
}
