package com.dumbhippo.server;

import javax.ejb.ApplicationException;

@ApplicationException
public class TokenExpiredException extends Exception {
	
	private static final long serialVersionUID = 1L;
	private Class<?> tokenClass;
	
	public TokenExpiredException(Class<?> tokenClass) {
		super(tokenClass.getName() + " has expired");
		this.tokenClass = tokenClass;
	}
	
	public Class<?> getTokenClass() {
		return tokenClass;
	}
}
