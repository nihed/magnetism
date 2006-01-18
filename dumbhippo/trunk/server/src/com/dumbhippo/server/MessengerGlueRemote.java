package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.ejb.Remote;

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
		private Date timestamp;
		private String text;
		
		public ChatRoomMessage(String fromUsername, String text, Date timestamp) {
			this.fromUsername = fromUsername;
			this.timestamp = timestamp;
			this.text = text;
		}
		
		public String getFromUsername() {
			return fromUsername;
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
	
	/**
	 * Called when Jabber server starts up, so we can detect when 
	 * we have a new server and need to reset.
	 *  
	 * @param timestamp when the server is starting, from System.currentTimeMillis()
	 */
	public void serverStartup(long timestamp);
	
	/**
	 * Called each time a user opens their first session. Note that this 
	 * can only be used to take some action on user availability, unless we 
	 * add database fields to track state.
	 * 
	 * @param username the username that has a new session available
	 */
	public void onUserAvailable(String username);

	/**
	 * Called each time a user has zero sessions available.
	 * Not very reliably called since Jive could go down 
	 * and restart. To try to track user presence you'd 
	 * need to address this by e.g. having Jive reset
	 * our list of available users when it starts up.
	 * Also, to track user presence we'd need a database 
	 * representation of it.
	 * 
	 * @param username the username that became unavailable
	 */
	public void onUserUnavailable(String username);
	
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
}
