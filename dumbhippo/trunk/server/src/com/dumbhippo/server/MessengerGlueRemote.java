package com.dumbhippo.server;

import java.io.Serializable;

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
	 * Called when Jabber server starts up, so we can detect when 
	 * we have a new server and need to reset.
	 * timestamp parameter fixes an extremely hypothetical race condition
	 * 
	 * Without a singleton-across-all-EJB-servers stateful bean this method
	 * more or less has to be a no-op unfortunately 
	 * 
	 * @param timestamp when the server is starting, from System.currentTimeMillis()
	 */
	public void serverStartup(long timestamp);
	
	/**
	 * Called each time a user opens their first session. Note that this 
	 * can only be used to take some action on user availability, it can't
	 * be used to track state since this is a stateless bean.
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
	 * Also, to track user presence we'd need a stateful bean.
	 * 
	 * @param username the username that became unavailable
	 */
	public void onUserUnavailable(String username);
}
