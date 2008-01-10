/**
 * 
 */
package com.dumbhippo.web;

public class NotLoggedInException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotLoggedInException(String string) {
		super(string);
	}
	
	public NotLoggedInException(String message, Throwable cause) {
		super(message, cause);
	}
}
