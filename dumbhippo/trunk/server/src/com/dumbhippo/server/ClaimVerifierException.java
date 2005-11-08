package com.dumbhippo.server;

import javax.ejb.ApplicationException;

@ApplicationException
public class ClaimVerifierException extends Exception {

	private static final long serialVersionUID = 1L;

	ClaimVerifierException(String message, Throwable cause) {
		super(message, cause);
	}
	ClaimVerifierException(String message) {
		super(message);
	}
}
