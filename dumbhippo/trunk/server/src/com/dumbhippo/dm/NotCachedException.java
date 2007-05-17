package com.dumbhippo.dm;

/**
 * Indicates that a request for data from the cache didn't find anything. (Needed
 *   so we can cache null values) 
 */
public class NotCachedException extends Exception {
	private static final long serialVersionUID = 1L;
}
