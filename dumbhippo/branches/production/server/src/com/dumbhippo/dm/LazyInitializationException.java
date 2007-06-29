package com.dumbhippo.dm;

import com.dumbhippo.server.NotFoundException;

/**
 * LazyInitializationException is thrown when a call to {@link DMObject.init()} as a
 * side-effect of method call to a property getter produces a NotFoundException.
 * This exception usually indicates a logic error: someone has called 
 * {@link DMSession.findUnchecked} on a key that was provided by a user or otherwise
 * might not exist. It might also indicate database corruption (a dangling reference
 * to a non-existent object in the database), or that an object was deleted from
 * the database between the point where the DMObject was created and the point where
 * init() was called.
 * 
 * @author otaylor
 */
public class LazyInitializationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public LazyInitializationException(String message, NotFoundException cause) {
		super(message, cause);
		
		if (cause == null)
			throw new RuntimeException("Cause must be specified");
	}
	
	@Override
	public NotFoundException getCause() {
		return (NotFoundException)super.getCause();
	}
}
