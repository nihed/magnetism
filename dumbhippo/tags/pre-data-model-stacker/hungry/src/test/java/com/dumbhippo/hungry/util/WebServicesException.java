package com.dumbhippo.hungry.util;

public class WebServicesException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public WebServicesException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public WebServicesException(String message) {
		super(message);
	}
}
