package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.persistence.CachedListItem;
import com.dumbhippo.server.util.EJBUtil;

public abstract class AbstractListCacheBean<KeyType,ResultType,EntityType extends CachedListItem>
	extends AbstractCacheBean<KeyType,List<ResultType>,AbstractListCache<KeyType,ResultType>>
	implements AbstractListCache<KeyType, ResultType>, ListCacheStorageMapper<KeyType,ResultType,EntityType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractListCacheBean.class);
	
	private ListCacheStorage<KeyType,ResultType,EntityType> storage;
	
	protected AbstractListCacheBean(Request defaultRequest, Class<? extends AbstractListCache<KeyType,ResultType>> ejbIface, long expirationTime) {
		super(defaultRequest, ejbIface, expirationTime);
	}
	
	@PostConstruct
	public void init() {
		storage = new ListCacheStorage<KeyType,ResultType,EntityType>(em, getExpirationTime(), this);
	}
	
	static private class AbstractListCacheTask<KeyType,ResultType,EntityType> implements Callable<List<ResultType>> {

		private Class<? extends AbstractListCache<KeyType,ResultType>> ejbIface;
		private KeyType key;
		
		public AbstractListCacheTask(KeyType key, Class<? extends AbstractListCache<KeyType,ResultType>> ejbIface) {
			this.key = key;
			this.ejbIface = ejbIface;
		}
		
		public List<ResultType> call() {
			logger.debug("Entering AbstractListCacheTask thread for bean {} key {}", ejbIface.getName(), key);

			EJBUtil.assertNoTransaction();
			
			// we do this instead of an inner class to work right with threads
			AbstractListCache<KeyType,ResultType> cache = EJBUtil.defaultLookup(ejbIface);
			
			// Check again in case another node stored the data first
			try {
				List<ResultType> results = cache.checkCache(key);
				if (results == null)
					throw new RuntimeException("AbstractListCache.checkCache() isn't supposed to return null ever, it did for key: " + key);
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
			throw new IllegalArgumentException("null key passed to AbstractListCacheBean");
		
		try {
			List<ResultType> results = checkCache(key);

			if (results == null)
				throw new RuntimeException("AbstractListCache.checkCache isn't supposed to return null ever, it did for key " + key);
		
			logger.debug("Using cached listing of {} items for {}", results.size(), key);
			return new KnownFuture<List<ResultType>>(results);
		} catch (NotCachedException e) {
			Callable<List<ResultType>> task = new AbstractListCacheTask<KeyType,ResultType,EntityType>(key, getEjbIface());
			return getExecutor().execute(key, task);
		}
	}

	public abstract List<EntityType> queryExisting(KeyType key);
	
	public abstract ResultType resultFromEntity(EntityType entity);
	
	public abstract EntityType entityFromResult(KeyType key, ResultType result);
	
	public List<ResultType> checkCache(KeyType key) throws NotCachedException {
		return storage.checkCache(key);
	}
	
	public abstract EntityType newNoResultsMarker(KeyType key);

	public void setAllLastUpdatedToZero(KeyType key) {
		throw new UnsupportedOperationException("Cache doesn't support manual expiration: " + getEjbIface().getName());
	}
	
	@Override
	public void expireCache(KeyType key) {
		storage.expireCache(key);
	}
	
	public void deleteCache(KeyType key) {
		storage.deleteCache(key);
	}
	
	// null means that we could not get the updated results, so leave the old results
	// empty list results means that we should save a no results marker
	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public List<ResultType> saveInCacheInsideExistingTransaction(KeyType key, List<ResultType> newItems, Date now) {
		return storage.saveInCacheInsideExistingTransaction(key, newItems, now);
	}
}
