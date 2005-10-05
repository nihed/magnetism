package com.dumbhippo.server;

import com.dumbhippo.persistence.ServerSecret;

public interface AuthenticationSystem {

	/**
	 * Access the server secret
	 * @see ServerSecret
	 * 
	 * @return the server secret
	 */
	public abstract ServerSecret getServerSecret();

}