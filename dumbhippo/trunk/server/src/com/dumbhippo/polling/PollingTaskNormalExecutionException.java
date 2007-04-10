package com.dumbhippo.polling;

/**
 * This exception should be thrown for executions which result in some
 * sort of (potentially) transient problem.  For example, an error opening a TCP
 * connection to a web service.  It should not be used to wrap just any exception
 * from the code because exceptions wrapped in this way will not have their stack
 * traces printed to the log.  Using this exception requires the task invoke
 * other methods or code which provides for this distinction.
 * 
 * This class intentionally omits the Exception(String) constructor because it
 * is assumed that it will be used solely to wrap an existing throwable from
 * an error condition as opposed to being a primary thrown exception.
 * 
 * @author walters
 */
public class PollingTaskNormalExecutionException extends Exception {
	private static final long serialVersionUID = 1L;

	public PollingTaskNormalExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	public PollingTaskNormalExecutionException(Throwable cause) {
		super(cause);
	}
}