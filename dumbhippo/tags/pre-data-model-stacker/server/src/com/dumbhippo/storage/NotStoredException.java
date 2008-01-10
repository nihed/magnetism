package com.dumbhippo.storage;

/**
 * This exception is thrown if a given item is not stored in the 
 * storage backend, i.e. ENOENT / File-not-found
 * 
 * @author Havoc Pennington
 *
 */
public class NotStoredException extends StorageException {
	private static final long serialVersionUID = 1L;
	
	public NotStoredException(String message) {
		super(message);
	}
	
	public NotStoredException(String message, Throwable cause) {
		super(message, cause);
	}
}
