package com.dumbhippo.services.caches;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.server.util.EJBUtil;

/** 
 * A session bean implementing the ListCache interface, which returns a list of objects (as opposed to one object) as the 
 * ResultType. Subclass AbstractListCacheWithStorageBean is usually a better choice unless your cache needs to 
 * implement its own custom handling of persistence objects.
 */
public abstract class AbstractListCacheBean<KeyType, ResultType>
		extends AbstractCacheBean<KeyType, List<ResultType>, ListCache<KeyType, ResultType>> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractListCacheBean.class);	
	
	protected AbstractListCacheBean(Request defaultRequest, Class<? extends ListCache<KeyType,ResultType>> ejbIface, long expirationTime) {
		super(defaultRequest, ejbIface, expirationTime);
	}
	
	static private class AbstractListCacheTask<KeyType,ResultType> implements Callable<List<ResultType>> {

		private Class<? extends ListCache<KeyType,ResultType>> ejbIface;
		private KeyType key;
		
		public AbstractListCacheTask(KeyType key, Class<? extends ListCache<KeyType,ResultType>> ejbIface) {
			this.key = key;
			this.ejbIface = ejbIface;
		}
		
		public List<ResultType> call() {
			logger.debug("Entering AbstractListCacheTask thread for bean {} key {}", ejbIface.getName(), key);

			EJBUtil.assertNoTransaction();
			
			// we do this instead of an inner class to work right with threads
			ListCache<KeyType,ResultType> cache = EJBUtil.defaultLookup(ejbIface);
			
			// Check again in case another node stored the data first
			try {
				List<ResultType> results = cache.checkCache(key);
				if (results == null)
					throw new RuntimeException("ListCache.checkCache() isn't supposed to return null ever, it did for key: " + key);
				else
					return results;
			} catch (NotCachedException e) {
				List<ResultType> result = cache.fetchFromNet(key);
				
				return cache.saveInCache(key, result);
			}
		}
	}
	
	public List<ResultType> getSync(KeyType key) {
		return getFutureResultEmptyListOnException(getAsync(key));
	}

	public Future<List<ResultType>> getAsync(KeyType key) {
		if (key == null)
			throw new IllegalArgumentException("null key passed to AbstractListCacheWithStorageBean");
		
		try {
			List<ResultType> results = checkCache(key);

			if (results == null)
				throw new RuntimeException("ListCache.checkCache isn't supposed to return null ever, it did for key " + key);
		
			logger.debug("Using cached listing of {} items for {}", results.size(), key);
			return new KnownFuture<List<ResultType>>(results);
		} catch (NotCachedException e) {
			Callable<List<ResultType>> task = new AbstractListCacheTask<KeyType,ResultType>(key, getEjbIface());
			return getExecutor().execute(key, task);
		}
	}
}
