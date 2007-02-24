package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.EnumMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.UniqueTaskExecutor;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

/**
 * Base class used for beans that implement a cached web service lookup.
 *
 * Use one of the subclasses (AbstractBasicCacheWithStorageBean, AbstractListCacheWithStorageBean, AbstractBasicCacheBean, AbstractListCacheBean)
 * if possible, they have more stuff implemented by default.
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS) // hackaround for bug with method tx attr on generic methods
public abstract class AbstractCacheBean<KeyType,ResultType,EjbIfaceType> implements Cache<KeyType,ResultType> {
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
		FLICKR_USER_PHOTOSETS,
		FACEBOOK_PHOTO_DATA,
		YOUTUBE_VIDEOS,
		NETFLIX_QUEUE_MOVIES
	}
	
	private static EnumMap<Request,UniqueTaskExecutor> executors;
	private static boolean shutdown = false;
	
	private Request defaultRequest;
	
	private Class<? extends EjbIfaceType> ejbIface;
	private long expirationTime; // in milliseconds until we expire the cache	
	
	@PersistenceContext(unitName = "dumbhippo")
	protected EntityManager em;
	
	@EJB
	protected TransactionRunner runner;
	
	@EJB
	protected Configuration config;			
	
	@EJB
	protected CacheFactory cacheFactory;
	
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
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED) // would be the default, but we changed the class default
	public void expireCache(KeyType key) {
		throw new UnsupportedOperationException("This cache bean doesn't implement cache expiration");
	}
	
	protected abstract ResultType fetchFromNetImpl(KeyType key);	
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public ResultType fetchFromNet(KeyType key) {
		EJBUtil.assertNoTransaction();
		return fetchFromNetImpl(key);
	}

	/** This is final since you should override saveInCacheInsideExistingTransaction instead, generally */
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public final ResultType saveInCache(final KeyType key, final ResultType data, final boolean refetchedWithoutCheckingCache) {
		EJBUtil.assertNoTransaction();
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<ResultType>() {
				public ResultType call() {
					return saveInCacheInsideExistingTransaction(key, data, new Date(), refetchedWithoutCheckingCache);
				}
			});
		} catch (Exception e) {
			if (EJBUtil.isDatabaseException(e)) {
				logger.warn("Ignoring database exception {}: {}", e.getClass().getName(), e.getMessage());
				return data;
			} else {
				ExceptionUtils.throwAsRuntimeException(e);
				throw new RuntimeException(e); // not reached
			}
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ResultType getSync(KeyType key) {
		return getSync(key, false);
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)	
	public Future<ResultType> getAsync(KeyType key) {
		return getAsync(key, false);
	}
}
