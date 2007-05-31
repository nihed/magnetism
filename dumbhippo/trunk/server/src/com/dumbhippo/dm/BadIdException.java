package com.dumbhippo.dm;

public class BadIdException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public BadIdException(String message, Exception cause) {
		super(message, cause);
	}
	
	public BadIdException(String message) {
		super(message);
	}
	
	public BadIdException(Exception cause) {
		super(cause);
	}
}