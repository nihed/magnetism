package com.dumbhippo.services.caches;

/**
 * Exception thrown if we don't have something cached; allows return value 
 * to use null for "negative result cached" (otherwise null would be ambiguous 
 * for "cached no result" vs. "nothing cached")
 * 
 * @author Havoc Pennington
 *
 */
public class NotCachedException extends Exception {
	private static final long serialVersionUID = 1L;

	public NotCachedException() {
		super("Value was not in the cache");
	}
}
