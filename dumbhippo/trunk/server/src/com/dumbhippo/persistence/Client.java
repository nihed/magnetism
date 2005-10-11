/**
 * 
 */
package com.dumbhippo.persistence;

import java.io.Serializable;

import javax.persistence.Entity;

import com.dumbhippo.identity20.RandomToken;

/**
 * 
 * A client is a computer or other device or application that someone uses to
 * access their account. The main purpose of this class is to store an auth cookie 
 * for the client so the user doesn't have to log in again, but it 
 * could also be used for random per-client state.
 * 
 * @author hp
 * 
 */
@Entity
public class Client extends DBUnique implements Serializable {

	private static final long serialVersionUID = 0L;

	private String authKey;

	private String name;

	private long lastUsed;

	private void init(String authKey, String name, long lastUsed) {
		this.authKey = authKey;
		if (this.authKey == null)
			this.authKey = RandomToken.createNew().toString();
		this.name = name;
		this.lastUsed = lastUsed;
	}

	public Client() {
		init(null, "Anonymous", // TODO localize
				System.currentTimeMillis());
	}

	public Client(String name) {
		init(null, name, System.currentTimeMillis());
	}
	
	public Client(Client client) {
		init(client.authKey, client.name, client.lastUsed);
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{Client name = " + name + " authKey " + "???" + " lastUsed " + lastUsed + "}");
		return builder.toString();
	}
	
	public String getAuthKey() {
		return authKey;
	}

	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	public long getLastUsed() {
		return lastUsed;
	}

	public void setLastUsed(long lastUsed) {
		this.lastUsed = lastUsed;
	}

	/**
	 * A string that might give some hint about the sort of client.
	 * Might be the IP address of the client, 
	 * or the name of the chat app, or "home" or whatever.
	 * 
	 * Probably users can set this, similar to a jabber resource?
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
