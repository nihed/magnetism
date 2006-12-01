package com.dumbhippo.services.caches;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.server.util.EJBUtil;

/** 
 * A session bean implementing the ListCache interface, which returns a list of objects (as opposed to one object) as the 
 * ResultType. Subclass AbstractListCacheWithStorageBean is usually a better choice unless your cache needs to 
 * implement its own custom handling of persistence objects.
 */
public abstract class AbstractListCacheBean<KeyType, ResultType>
		extends AbstractCacheBean<KeyType, List<? extends ResultType>, ListCache<KeyType, ResultType>> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractListCacheBean.class);	
	
	private Class<ResultType> resultClass;
	
	protected AbstractListCacheBean(Request defaultRequest, Class<? extends ListCache<KeyType,ResultType>> ejbIface, long expirationTime, Class<ResultType> resultClass) {
		super(defaultRequest, ejbIface, expirationTime);
		this.resultClass = resultClass;
	}
	
	protected Class<ResultType> getResultClass() {
		return resultClass;
	}
	
	static private class AbstractListCacheTask<KeyType,ResultType> implements Callable<List<? extends ResultType>> {

		private Class<? extends ListCache<KeyType,ResultType>> ejbIface;
		private KeyType key;
		private boolean alwaysRefetch;
		
		public AbstractListCacheTask(KeyType key, boolean alwaysRefetch, Class<? extends ListCache<KeyType,ResultType>> ejbIface) {
			this.key = key;
			this.ejbIface = ejbIface;
			this.alwaysRefetch = alwaysRefetch;
		}
		
		public List<? extends ResultType> call() {
			logger.debug("Entering AbstractListCacheTask thread for bean {} key {}", ejbIface.getName(), key);

			EJBUtil.assertNoTransaction();
			
			// we do this instead of an inner class to work right with threads
			ListCache<KeyType,ResultType> cache = EJBUtil.defaultLookup(ejbIface);
			
			// Check again in case another node stored the data first
			try {
				if (alwaysRefetch)
					throw new NotCachedException("Forced refetch");
				
				List<? extends ResultType> results = cache.checkCache(key);
				if (results == null)
					throw new RuntimeException("ListCache.checkCache() isn't supposed to return null ever, it did for key: " + key);
				else
					return results;
			} catch (NotCachedException e) {
				List<? extends ResultType> result = cache.fetchFromNet(key);
				
				return cache.saveInCache(key, result, alwaysRefetch);
			}
		}
	}
	
	public List<? extends ResultType> getSync(KeyType key, boolean alwaysRefetchEvenIfCached) {
		return ThreadUtils.getFutureResultEmptyListOnException(getAsync(key, alwaysRefetchEvenIfCached), resultClass);
	}

	public Future<List<? extends ResultType>> getAsync(KeyType key, boolean alwaysRefetchEvenIfCached) {
		if (key == null)
			throw new IllegalArgumentException("null key passed to AbstractListCacheWithStorageBean");

		// you really don't want a transaction open unless you can assume on average we aren't doing a
		// remote request (i.e. assuming a cache hit is likely)
		if (alwaysRefetchEvenIfCached)
			EJBUtil.assertNoTransaction();		
		
		try {
			if (alwaysRefetchEvenIfCached)
				throw new NotCachedException("Forced refetch");
			
			List<? extends ResultType> results = checkCache(key);

			if (results == null)
				throw new RuntimeException("ListCache.checkCache isn't supposed to return null ever, it did for key " + key);
		
			logger.debug("Using cached listing of {} items for {}", results.size(), key);
			return new KnownFuture<List<? extends ResultType>>(results);
		} catch (NotCachedException e) {
			Callable<List<? extends ResultType>> task = new AbstractListCacheTask<KeyType,ResultType>(key, alwaysRefetchEvenIfCached, getEjbIface());
			return getExecutor().execute(key, task);
		}
	}
}
