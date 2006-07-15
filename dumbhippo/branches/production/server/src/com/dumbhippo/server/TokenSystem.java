package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.Token;

@Local
public interface TokenSystem {
	
	/**
	 * Look up a token by authentication key
	 * @param authKey potential authentication key
	 * @return the corresponding token, or null if none
	 */
	public Token getTokenByKey(String authKey) throws TokenExpiredException, TokenUnknownException;
	
}
