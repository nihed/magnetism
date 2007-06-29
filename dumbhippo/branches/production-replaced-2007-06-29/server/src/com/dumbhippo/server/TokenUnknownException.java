package com.dumbhippo.server;

public class TokenUnknownException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public TokenUnknownException(String key, Throwable cause) {
		super("No record of token " + key, cause);
	}
}
