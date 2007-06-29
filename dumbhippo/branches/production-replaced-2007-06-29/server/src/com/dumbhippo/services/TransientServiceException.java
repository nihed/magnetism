package com.dumbhippo.services;


public class TransientServiceException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public TransientServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransientServiceException(String message) {
		super(message);
	}

	public TransientServiceException(Throwable cause) {
		super(cause);
	}
};
