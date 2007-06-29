package com.dumbhippo.tx;

import javax.ejb.ApplicationException;

/**
 * RetryException is a checked exception that indicates that the enclosing
 * transaction should be retried.
 * 
 * It's a checked exception, even though calling code normally shouldn't
 * handle it, but just pass it back up, so that the retry is evident
 * in the calling code, and it will be apparent if the calling code is
 * doing something that can't be retried, like writing output.
 *  
 * The marking as ApplicationException(rollback=true) isn't really necesary
 * since we are going to catch the exception in our own transaction management
 * code (where we do the retry), and normally we are throwing this as a
 * result of a JDBC exception that will roll back the transaction anyways.
 * But the annotation does reflect our intent and should be harmless.
 *  
 * @author otaylor
 */
@ApplicationException(rollback=true)
public class RetryException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public RetryException(String message) {
		super(message);
	}
	
	public RetryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryException(Throwable cause) {
		super(cause);
	}
}
