package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratorType;
import javax.persistence.Id;

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
@Entity
public class ServerSecret {
	private byte[] secret;
	
	public ServerSecret() {
		secret = RandomToken.createNew().getBytes();
	}
	
	@Id(generate = GeneratorType.NONE)
	public byte[] getSecret() {
		return secret;
	}

	protected void setSecret(byte[] secret) {
		this.secret = secret;
	}
}
