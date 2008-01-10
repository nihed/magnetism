package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;

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
	 * Called whenever a new resource connects associated with a user. Sends
	 * that user any messages that the might have missed or other notifications
	 * that are needed on connection. This is separate from updateLoginDate
	 * because we want to separate out a potentionally expensive read-only transaction
	 * from a short write transaction.  
	 * 
	 * @param userId the ID associated with resource
	 * @param wasAlreadyConnected true if the user was connected to the cluster
	 *   (this node or another) before this resource connected. Note that this
	 *   value is only approximate: if two resources connect simultaneously, they
	 *   both can end up with wasAlreadyConnected = false. 
	 * @throws RetryException 
	 */
	public void sendConnectedResourceNotifications(Guid userId, boolean wasAlreadyConnected) throws RetryException;	
	
	/**
	 * Called whenever a new resource connects associated with a user. Note that
	 * there is no ordering guarantee between this and updateLogoutDate if a user
	 * connects momentarily, though the timestamps passed in will be reliably
	 * ordered (or equal).
	 * 
	 * @param userId the ID associated with resource
	 * @param timestamp a timestamp for the user connecting
	 */
	public void updateLoginDate(Guid userId, Date timestamp);	
	
	/**
	 * Called when the last resource for a user logged out; this information
	 * is used for statistical purposes and also to (not perfectly reliably)
	 * track what posts the user has seen. 
	 * 
	 * @param userId the ID of the user
	 * @param date a timestamp for the user disconnecting.
	 */
	public void updateLogoutDate(Guid userId, Date timestamp);

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
	 * Signal whether or not the user wants to ignore future notifications about
	 * a particular post.
	 * 
	 * @param userId the guid of the user
	 * @param postId the guid of the post
	 * @throws ParseException 
	 * @throws NotFoundException 
	 */
	public void setPostIgnored(Guid userId, Guid postId, boolean ignore) throws NotFoundException, ParseException;

	public void addGroupMember(Guid user, Guid groupId, Guid invitee) throws NotFoundException;

	public boolean isServerTooBusy();
	
	public void handleMusicChanged(UserViewpoint viewpoint, Map<String, String> properties) throws RetryException;

	public void handleMusicPriming(UserViewpoint viewpoint, List<Map<String, String>> primingTracks) throws RetryException;
	
	public void handlePresence(String localJid, String remoteJid, String type) throws RetryException;
}
