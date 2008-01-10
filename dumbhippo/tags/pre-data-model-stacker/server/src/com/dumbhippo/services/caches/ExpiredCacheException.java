package com.dumbhippo.services.caches;

public class ExpiredCacheException extends NotCachedException {
	private static final long serialVersionUID = 1L;

	public ExpiredCacheException() {
		super("The data in cache is expired");
	}
}
