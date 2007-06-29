package com.dumbhippo.storage;

public class TooBigException extends StorageException {
	private static final long serialVersionUID = 1L;
	
	public TooBigException(String message) {
		super(message);
	}
	
	public TooBigException(String message, Throwable cause) {
		super(message, cause);
	}
}
