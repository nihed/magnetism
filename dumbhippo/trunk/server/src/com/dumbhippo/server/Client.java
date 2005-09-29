/**
 * 
 */
package com.dumbhippo.server;

import com.dumbhippo.identity20.RandomToken;
import com.dumbhippo.persistence.DBUnique;

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
class Client extends DBUnique {

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
