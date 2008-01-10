package com.dumbhippo.server;

import javax.ejb.ApplicationException;

/**
 * Thrown when a "lookupFoo" or "getFoo" method doesn't successfully lookup 
 * or get the Foo for whatever reason. Should be used instead of
 * a "return null" kind of convention.
 * 
 * Putting rollback=true on this could be right but broke stuff for me; 
 * we often catch this and handle it inside the EJB layer, and rollback=true 
 * seems to roll back even if you don't throw out of the outermost EJB frame.
 * Needs investigation.
 * 
 * See: http://today.java.net/pub/a/today/2006/04/06/exception-handling-antipatterns.html#ejb3
 * 
 * @author Havoc Pennington
 */
@ApplicationException
public class NotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public NotFoundException(String message) {
		super(message);
	}
}
