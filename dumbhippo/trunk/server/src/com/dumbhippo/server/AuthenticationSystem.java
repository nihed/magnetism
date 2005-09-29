package com.dumbhippo.server;

public interface AuthenticationSystem {

	/**
	 * Access the server secret
	 * @see ServerSecret
	 * 
	 * @return the server secret
	 */
	public abstract ServerSecret getServerSecret();

}