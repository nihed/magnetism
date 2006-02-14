package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Remote;

import com.dumbhippo.live.Hotness;

@Remote
public interface MessengerGlueRemote {

	/** 
	 * Simple immutable data blob
	 * @author hp
	 *
	 */
	public class JabberUser implements Serializable {
	
		private static final long serialVersionUID = 0L;
		private String username;
		private String name;
		private String email;
		
		/**
		 * @param username
		 * @param name
		 * @param email
		 */
		public JabberUser(String username, String name, String email) {
			this.username = username;
			this.name = name;
			this.email = email;
		}

		public String getEmail() {
			return email;
		}

		public String getName() {
			return name;
		}

		public String getUsername() {
			return username;
		}
	}
	
	public class ChatRoomUser implements Serializable {
		private static final long serialVersionUID = 0L;
		
		private String username;
		private int version;
		private String name;
		
		public ChatRoomUser(String username, int version, String name) {
			this.username = username;
			this.version = version;
			this.name = name;
		}
		
		public String getUsername() {
			return username;
		}
		
		public int getVersion() {
			return version;
		}
		
		public String getName() {
			return name;
		}
	}

	public class ChatRoomMessage implements Serializable {
		private static final long serialVersionUID = 0L;
		
		private String fromUsername;
		private int serial;
		private Date timestamp;
		private String text;
		
		public ChatRoomMessage(String fromUsername, String text, Date timestamp, int serial) {
			this.fromUsername = fromUsername;
			this.timestamp = timestamp;
			this.text = text;
			this.serial = serial;
		}
		
		public String getFromUsername() {
			return fromUsername;
		}

		public int getSerial() {
			return serial;
		}

		public String getText() {
			return text;
		}

		public Date getTimestamp() {
			return timestamp;
		}
	}

	public class ChatRoomInfo implements Serializable {
		private static final long serialVersionUID = 0L;
		
		private String roomName;
		private String postTitle;
		private List<ChatRoomUser> allowedUsers;
		private List<ChatRoomMessage> history;
		
		public ChatRoomInfo(String roomName, String postTitle, List<ChatRoomUser> allowedUsers, List<ChatRoomMessage> history) {
			this.roomName = roomName;
			this.postTitle = postTitle;
			this.allowedUsers = allowedUsers; // Don't copy for efficiency, assume the caller won't
			                                  // subsequently modify
			this.history = history;
		}
		
		public String getPostId() {
			return roomName;
		}
		
		public String getPostTitle() {
			return postTitle;
		}
		
		public List<ChatRoomUser> getAllowedUsers() {
			return Collections.unmodifiableList(allowedUsers);
		}

		public List<ChatRoomMessage> getHistory() {
			return Collections.unmodifiableList(history);
		}
	}
	
	public class MySpaceBlogCommentInfo implements Serializable {
		private static final long serialVersionUID = 1L;

		private long commentId;
		private long posterId;
		
		public MySpaceBlogCommentInfo(long commentId, long posterId) {
			this.commentId = commentId;
			this.posterId = posterId;
		}
		public long getCommentId() {
			return commentId;
		}
		public long getPosterId() {
			return posterId;
		}
	}
	
	
	public class MySpaceContactInfo implements Serializable {
		private static final long serialVersionUID = 1L;		
		private String username;
		private String friendId;
		public MySpaceContactInfo(String username, String friendId) {
			this.username = username;
			this.friendId = friendId;
		}
		public String getFriendId() {
			return friendId;
		}
		public String getUsername() {
			return username;
		}
	}	

	/** 
	 * Do Jabber digest authentication
	 * @param username Jabber username (domain is not included, just username)
	 * @param token
	 * @param digest
	 * @return true if authorized
	 */
	public boolean authenticateJabberUser(String username, String token, String digest);

	/**
	 * Get the number of active Jabber users in the system. Obviously 
	 * this has to be represented by a 64-bit integer.
	 * 
	 * @return number of active jabber users
	 */
	public long getJabberUserCount();


	/** 
	 * Set the real-life name displayed for a Jabber user.
	 * @param username Jabber username
	 * @param name name
	 */
	public void setName(String username, String name)
		throws JabberUserNotFoundException;

	/** 
	 * Sets the Jabber user's email address
	 * 
	 * @param username Jabber username
	 * @param email email address the user says they have
	 */
	public void setEmail(String username, String email)
		throws JabberUserNotFoundException;
	
	public JabberUser loadUser(String username)
		throws JabberUserNotFoundException;
	
	/**
	 * Return the MySpace name associated with a user, or null if none 
	 * 
	 * @param userId
	 * @return MySpace name or null
	 */
	public String getMySpaceName(String username);
	
	public void addMySpaceBlogComment(String username, long commentId, long posterId);
	
	public List<MySpaceBlogCommentInfo> getMySpaceBlogComments(String username);	
	
	public void notifyNewMySpaceContactComment(String username, String contactName);
	
	/**
	 * Return a list of MySpace names associated with the passed user's DumbHippo
	 * contacts.
	 * 
	 * @param username user for which MySpace contacts are retrieved
	 * @return MySpace contacts
	 */
	public List<MySpaceContactInfo> getContactMySpaceNames(String username);
	
	/**
	 * Thrown when the server identifier passed to one of the functions
	 * for maintaining Jabber state is unknown. This could happen for
	 * one of two reasons; either the application server has been 
	 * restarted in between, or because the Jabber server did not
	 * ping frequently enough and the application server assumed
	 * that it had died. In either case, when you get this exception
	 * you must get a new server identifier and replay the current
	 * state.
	 */
	public class NoSuchServerException extends Exception {
		private static final long serialVersionUID = 1L;

		public NoSuchServerException(String message) {
			super(message);
		}
	}
	
	/**
	 * Called when Jabber server starts up. After calling this method you must
	 * call serverPing periodically or it will be assumed that the server
	 * has died and cached state for the server will be discarded. 
	 * (Once a minute is the recommended ping period.)
	 *  
	 * @param timestamp when the server is starting, from System.currentTimeMillis()
	 * @return a string that will be used to identify this server
	 *        instance in subsequent communication. Each time the server is
	 *        restarted, a new server identifier should be generated.
	 *        If an attempt to use this ID throws NoSuchServerException, then
	 *        the server must call serverStartup() again, then re-register
	 *        all present users by calling onUserAvailable().
	 */
	public String serverStartup(long timestamp);

	/**
	 * Keep resources associated with a Jabber server from timing out.
	 * 
	 * @param serverIdentifier identifying string for the server returned from serverStartup()
	 */
	public void serverPing(String serverIdentifier) throws NoSuchServerException;
	
	/**
	 * Called each time a user opens their first session.
	 * 
	 * @param serverIdentifier identifying string for the server returned from serverStartup()
	 * @param username the username that has a new session available
	 */
	public void onUserAvailable(String serverIdentifier, String username) throws NoSuchServerException;

	/**
	 * Called each time a user has zero sessions available.
	 * 
	 * @param serverIdentifier identifying string for the server returned from serverStartup()
	 * @param username the username that became unavailable
	 */
	public void onUserUnavailable(String serverIdentifier, String username) throws NoSuchServerException;
	
	/**
	 * Called when a user leaves the chatroom for a post.
	 * 
	 * @param serverIdentifier identifying string for the server returned from serverStartup()
	 * @param username the username that has a new session available
	 */
	public void onRoomUserAvailable(String serverIdentifier, String roomname, String username, boolean participant) throws NoSuchServerException;

	/**
	 * Called when a user joins the chatroom for a post.
	 * 
	 * @param serverIdentifier identifying string for the server returned from serverStartup()
	 * @param username the username that became unavailable
	 */
	public void onRoomUserUnavailable(String serverIdentifier, String roomname, String username) throws NoSuchServerException;


	/**
	 * Called whenever a new resource connects associated with a user.
	 * 
	 * @param serverIdentifier identifying string for the server returned from serverStartup()
	 * @param user the username associated with resource
	 * @throws NoSuchServerException 
	 */
	public void onResourceConnected(String serverIdentifier, String user) throws NoSuchServerException;	
	
	/**
	 * Get the information needed to manage a chatroom for a post.
	 * 
	 * @param roomName The GUID for the post that the chat is about,
	 *   encoded in jabber node form. 
	 * @param initialUsername The initial user requesting that the 
	 *   chatroom be created. If the user doesn't have access to
	 *   the post, null will be returned as if the post doesn't
	 *   exist.
	 *  @return a blob of information about the chatroom. Will
	 *   be null if no such post exists or the user isn't allowed
	 *   to see it.
	 */
	public ChatRoomInfo getChatRoomInfo(String roomName, String initialUsername);
	
	/**
	 * Return a blob of user prefs. If user doesn't exist it just returns
	 * empty prefs.
	 * 
	 * @param username the username 
	 * @return the blob o' prefs 
	 */
	public Map<String,String> getPrefs(String username);
	
	public Hotness getUserHotness(String username);
}
