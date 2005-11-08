package com.dumbhippo.server;

import javax.ejb.ApplicationException;

@ApplicationException
public class LoginVerifierException extends Exception {

	private static final long serialVersionUID = 1L;

	LoginVerifierException(String message, Throwable cause) {
		super(message, cause);
	}
	LoginVerifierException(String message) {
		super(message);
	}
}
