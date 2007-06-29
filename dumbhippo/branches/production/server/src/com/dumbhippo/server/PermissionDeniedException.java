package com.dumbhippo.server;

public class PermissionDeniedException extends Exception {
	private static final long serialVersionUID = 1L;

	public PermissionDeniedException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public PermissionDeniedException(String message) {
		super(message);
	}
}
