package com.dumbhippo.server;

/**
 * Thrown when an operation could not be performed because of an authorization
 * failure; our general strategy is that there is no difference between information
 * you can't see and information doesn't exist, but this is useful, for example,
 * when checking an authorization token during sign in. 
 * 
 * @author otaylor
 */
public class UnauthorizedException extends Exception {
	private static final long serialVersionUID = 1L;

	public UnauthorizedException(String string) {
		super(string);
	}
}
