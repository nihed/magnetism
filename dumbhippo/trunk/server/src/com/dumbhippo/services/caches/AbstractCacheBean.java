package com.dumbhippo.services.caches;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.UniqueTaskExecutor;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.caches.AbstractCache;

/**
 * Base class used for beans that implement a cached web service lookup.
 *
 * Use one of the two subclasses (AbstractBasicCacheBean, AbstractListCacheBean) if possible, 
 * they have more stuff implemented by default (they add knowledge of the entity bean type used 
 * to store the cache).
 */
public abstract class AbstractCacheBean<KeyType,ResultType,EjbIfaceType> implements AbstractCache<KeyType,ResultType> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractCacheBean.class);
	
	// how long to wait on the search API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	// 2 days, shared by yahoo-related subclasses
	static protected final int YAHOO_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24 * 2;
	// hour timeout to retry on failure
	static protected final int FAILED_QUERY_TIMEOUT = 1000 * 60 * 60;

	protected enum Request {
		AMAZON_ALBUM,
		RHAPSODY_DOWNLOAD,
		YAHOO_ALBUM,
		YAHOO_ALBUM_SONGS,
		YAHOO_ARTIST_ALBUMS,
		YAHOO_ARTIST,
		YAHOO_ARTIST_BY_NAME,
		YAHOO_SONG,
		YAHOO_SONG_DOWNLOAD,
		FLICKR_USER_PHOTOS,
		FLICKR_PHOTOSET_PHOTOS,
		FLICKR_USER_PHOTOSETS
	}
	
	private static EnumMap<Request,UniqueTaskExecutor> executors;
	private static boolean shutdown = false;
	
	private Request defaultRequest;
	
	private Class<? extends EjbIfaceType> ejbIface;
	private long expirationTime; // in milliseconds until we expire the cache	
	
	private synchronized static UniqueTaskExecutor getExecutorInternal(Request request) {
		if (shutdown)
			throw new RuntimeException("getExecutor() called after shutdown");
		
		if (executors == null)
			executors = new EnumMap<Request,UniqueTaskExecutor>(Request.class);
		
		UniqueTaskExecutor executor = executors.get(request);
		if (executor == null) {
			executor = new CacheTaskExecutor(request.name().toLowerCase() + " pool");
			executors.put(request, executor);
		}
		return executor;
	}
	
	@SuppressWarnings("unchecked")
	public static void shutdown() {
		synchronized (AbstractCacheBean.class) {
			shutdown = true;
			
			// executors is null if we've never called getExecutorInternal
			if (executors != null) {
				for (UniqueTaskExecutor executor : executors.values()) {
					executor.shutdownAndAwaitTermination();
				}
				executors.clear();
				executors = null;
			}
		}
	}
	
	protected static <T> T getFutureResultNullOnException(Future<T> future) {
		return ThreadUtils.getFutureResultNullOnException(future);
	}
	
	protected static <T> List<T> getFutureResultEmptyListOnException(Future<List<T>> future) {
		return ThreadUtils.getFutureResultEmptyListOnException(future);
	}
	
	protected AbstractCacheBean(Request defaultRequest, Class<? extends EjbIfaceType> ejbIface, long expirationTime) {
		this.defaultRequest = defaultRequest;
		this.ejbIface = ejbIface;
		this.expirationTime = expirationTime;
	}
	
	@SuppressWarnings("unchecked")
	protected UniqueTaskExecutor<KeyType,ResultType> getExecutor() {
		return getExecutorInternal(defaultRequest);
	}
	
	@SuppressWarnings("unchecked")
	protected UniqueTaskExecutor<KeyType,ResultType> getExecutor(Request request) {
		return getExecutorInternal(request);
	}	

	protected Class<? extends EjbIfaceType> getEjbIface() {
		return ejbIface;
	}

	protected long getExpirationTime() {
		return expirationTime;
	}	
	
	public void expireCache(KeyType key) {
		throw new UnsupportedOperationException("This cache bean doesn't implement cache expiration");
	}
	
	protected abstract ResultType fetchFromNetImpl(KeyType key);	
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public ResultType fetchFromNet(KeyType key) {
		EJBUtil.assertNoTransaction();
		return fetchFromNetImpl(key);
	}
}
