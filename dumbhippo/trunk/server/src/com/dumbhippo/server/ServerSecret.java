package com.dumbhippo.server;

import com.dumbhippo.identity20.RandomToken;

/**
 * A singleton secret stored in the database that is used for
 * validating non-secret data which we want to pass to the client and back,
 * for example encoding the data as 
 * SHA1(ServerSecret + data) + data
 * 
 * @author walters
 *
 */
public class ServerSecret {
	private byte[] secret;
	
	public ServerSecret() {
		secret = RandomToken.createNew().getBytes();
	}
	
	public byte[] getSecret() {
		return secret;
	}
}
