package com.dumbhippo.server.util;

public class NotFoundUncheckedException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public NotFoundUncheckedException(String message, Exception e) {
		super(message, e);
	}
}
