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
 * access their account. This class is mostly used for authentication and should
 * be kept internal probably; if exported, you'd want to copy into a value class
 * that didn't have the auth key. The auth key is basically a cookie that allows
 * a particular computer or application to log in without a password.
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
		builder.append("{Client name = " + name + " authKey " + authKey + " lastUsed " + lastUsed + "}");
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
