package com.dumbhippo.services.caches;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;


public abstract class AbstractCacheStorage<KeyType,ResultType,EntityType> implements
		CacheStorage<KeyType,ResultType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractCacheStorage.class);
	
	protected EntityManager em;
	
	private long expirationTime; // in milliseconds until we expire the cache
	
	protected AbstractCacheStorage(EntityManager em, long expirationTime) {
		this.em = em;
		this.expirationTime = expirationTime;
	}
	
	public void expireCache(KeyType key) {
		throw new UnsupportedOperationException("This cache storage bean doesn't implement cache expiration");
	}
	
	protected long getExpirationTime() {
		return expirationTime;
	}
}
