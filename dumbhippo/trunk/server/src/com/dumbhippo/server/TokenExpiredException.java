package com.dumbhippo.server;

import javax.ejb.ApplicationException;

@ApplicationException
public class TokenExpiredException extends Exception {
	
	private static final long serialVersionUID = 1L;
	private Class<?> tokenClass;
	private boolean viewed;
	
	public TokenExpiredException(Class<?> tokenClass, boolean viewed) {
		super(tokenClass.getName() + " has expired");
		this.tokenClass = tokenClass;
		this.viewed = viewed;
	}
	
	public Class<?> getTokenClass() {
		return tokenClass;
	}
	
	public boolean isViewed() {
		return viewed;
	}
}
