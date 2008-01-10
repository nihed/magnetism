package com.dumbhippo.dm;

/**
 * Indicates that the string form of a resource ID could not be parsed. Typically
 * thrown by a {@link DMKey} constructor.
 *  
 * @author otaylor
 */
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