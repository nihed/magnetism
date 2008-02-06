package com.dumbhippo.server;

public class ContactConflictException extends Exception {
	private static final long serialVersionUID = 260770317520710823L;

	public ContactConflictException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ContactConflictException(String message) {
		super(message);
	}
}
