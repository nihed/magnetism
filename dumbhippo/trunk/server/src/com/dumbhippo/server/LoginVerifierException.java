package com.dumbhippo.server;

import javax.ejb.ApplicationException;

@ApplicationException
public class LoginVerifierException extends Exception {

	private static final long serialVersionUID = 1L;

	public LoginVerifierException(String message, Throwable cause) {
		super(message, cause);
	}
	public LoginVerifierException(String message) {
		super(message);
	}
}
