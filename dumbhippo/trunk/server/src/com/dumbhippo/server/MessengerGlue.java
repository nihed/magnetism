package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

@Local
public interface MessengerGlue {

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
		 * @param email may be null
		 */
		public JabberUser(String username, String name, String email) {
			this.username = username;
			this.name = name;
			this.email = email;
		}

		/**
		 * email may be null if there is none
		 * @return email or null
		 */
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
	 * Called whenever a new resource connects associated with a user. Sends
	 * that user any messages that the might have missed or other notifications
	 * that are needed on connection. This is separate from updateLoginDate
	 * because we want to separate out a potentionally expensive read-only transaction
	 * from a short write transaction.  
	 * 
	 * @param user the username associated with resource
	 * @param wasAlreadyConnected true if the user was connected to the cluster
	 *   (this node or another) before this resource connected. Note that this
	 *   value is only approximate: if two resources connect simultaneously, they
	 *   both can end up with wasAlreadyConnected = false. 
	 */
	public void sendConnectedResourceNotifications(String user, boolean wasAlreadyConnected);	
	
	/**
	 * Called whenever a new resource connects associated with a user. Note that
	 * there is no ordering guarantee between this and updateLogoutDate if a user
	 * connects momentarily, though the timestamps passed in will be reliably
	 * ordered (or equal).
	 * 
	 * @param user the username associated with resource
	 * @param timestamp a timestamp for the user connecting
	 */
	public void updateLoginDate(String user, Date timestamp);	
	
	/**
	 * Called when the last resource for a user logged out; this information
	 * is used for statistical purposes and also to (not perfectly reliably)
	 * track what posts the user has seen. 
	 * 
	 * @param user the jabber username of the user
	 * @param date a timestamp for the user disconnecting.
	 */
	public void updateLogoutDate(String user, Date timestamp);

	/**
	 * Returns the information associated with the potential user of a chat room.
	 * 
	 * @param roomName the name of the chat room
	 * @param kind the kind of the chat room
	 * @param username name of the user for whom we look up information
	 * @return blob of information about the user
	 */
	public ChatRoomUser getChatRoomUser(String roomName, ChatRoomKind kind, String username);
	
	public Set<ChatRoomUser> getChatRoomRecipients(String roomName, ChatRoomKind kind);
	
	/**
	 * Get the information needed to manage a chatroom for a post.
	 * 
	 * @param roomName The GUID for the post that the chat is about,
	 *   encoded in jabber node form. 
	 * @return a blob of information about the chatroom. Will
	 *   be null if no such post exists or the user isn't allowed
	 *   to see it.
	 */
	public ChatRoomInfo getChatRoomInfo(String roomName);
	
	/**
	 * Get messages that have been sent to the chatroom since the specified serial
	 * 
	 * @param roomName the GUID of the group or post the chat is about, in jabber node form.
	 *        This must exist or an exception will be thrown
	 * @param kind the kind of chatroom (group or post)
	 * @param lastSeenSerial retrieve only messages with serials greater than this (use -1
	 *        to get all messages)
	 * @return a list of chat room messages.
	 */
	List<ChatRoomMessage> getChatRoomMessages(String roomName, ChatRoomKind kind, long lastSeenSerial);
	
	public boolean canJoinChat(String roomName, ChatRoomKind kind, String username);
	
	/**
	 * Adds a new message to a chatroom.
	 * 
	 * @param roomName the GUID of the group or post the chat is about, in jabber node form
	 * @param kind the kind of chatroom (group or post)
	 * @param userName the GUID of the user posting the message, in jabber node form
	 * @param text the text of the message
	 * @param timestamp the timestamp for the message
	 */
	public void addChatRoomMessage(String roomName, ChatRoomKind kind, String userName, String text, Date timestamp);
	
	/**
	 * Get current music info for a given user.
	 * @param username the username for the user 
	 * @return current music info for a given user
	 */
	public Map<String, String> getCurrentMusicInfo(String username);
	
	/**
	 * Return a blob of user prefs. If user doesn't exist it just returns
	 * empty prefs.
	 * 
	 * @param username the username 
	 * @return the blob o' prefs 
	 */
	public Map<String,String> getPrefs(String username);
	
	/**
	 * Retrieves information about post or posts in XML form suitable for passing
	 * back to the client over XMPP. A particular post can be specified by
	 * ID, or if none is specified, some number of recent posts will be
	 * returned. (Currently 4). One day we should have a "getPosts()" operation 
	 * that takes a flexible way of specifying what posts to get, and we should avoid
	 * having a big pile of slightly different variants.
	 * 
	 * @param username the user to get posts for
	 * @param id an optional id of a particular post to retrieve, or null
	 * @param elementName an optional outer element name, or null
	 * @return XML form of the recent posts or of the particular post.
	 *     if id is specified and is not the id of a post visible to the
	 *     user, returns null.
	 */
	public String getPostsXml(Guid userId, Guid id, String elementName);

	/**
	 * Signal whether or not the user wants to ignore future notifications about
	 * a particular post.
	 * 
	 * @param userId the guid of the user
	 * @param postId the guid of the post
	 * @throws ParseException 
	 * @throws NotFoundException 
	 */
	public void setPostIgnored(Guid userId, Guid postId, boolean ignore) throws NotFoundException, ParseException;

	/**
	 * Returns the viewpoint XML of a group for a person
	 * 
	 * @param username Guid of viewpoint person
	 * @param groupid Guid of group being dumped
	 * @return xml dump as string
	 * @throws NotFoundException when passed an invalid group id 
	 */
	public String getGroupXml(Guid username, Guid groupid) throws NotFoundException;

	public void addGroupMember(Guid user, Guid groupId, Guid invitee) throws NotFoundException;

	public boolean isServerTooBusy();
	
	public void handleMusicChanged(Guid userId, Map<String, String> properties);

	public void handleMusicPriming(Guid userId, List<Map<String, String>> primingTracks);
}
