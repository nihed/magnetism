package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.ServerSecret;

@Local
public interface AuthenticationSystem {

	/**
	 * Access the server secret
	 * @see ServerSecret
	 * 
	 * @return the server secret
	 */
	public abstract ServerSecret getServerSecret();

}