package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.persistence.CachedItem;
import com.dumbhippo.server.util.EJBUtil;

/** 
 * Base class for cache beans that store each result in one database row 
 * (vs. AbstractListCacheBean for beans that store a list of result rows).
 * 
 * @author Havoc Pennington
 *
 * @param <KeyType>
 * @param <ResultType>
 * @param <EntityType>
 */
public abstract class AbstractBasicCacheBean<KeyType,ResultType,EntityType extends CachedItem>
	extends AbstractCacheBean<KeyType,ResultType,AbstractCache<KeyType,ResultType>> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractBasicCacheBean.class);
	
	protected AbstractBasicCacheBean(Request defaultRequest, Class<? extends AbstractCache<KeyType,ResultType>> ejbIface, long expirationTime) {
		super(defaultRequest, ejbIface, expirationTime);
	}
	
	static private class AbstractBasicCacheTask<KeyType,ResultType> implements Callable<ResultType> {
		
		private Class<? extends AbstractCache<KeyType,ResultType>> ejbIface;
		private KeyType key;

		public AbstractBasicCacheTask(KeyType key, Class<? extends AbstractCache<KeyType,ResultType>> ejbIface) {
			this.key = key;
			this.ejbIface = ejbIface;
		}
		
		public ResultType call() {
			logger.debug("Entering AbstractBasicCacheTask thread for bean {} key {}", ejbIface.getName(), key);
		
			AbstractCache<KeyType,ResultType> cache = EJBUtil.defaultLookup(ejbIface);					

			try {
				// Check again in case another node stored the data first				
				return cache.checkCache(key);
			} catch (NotCachedException e) {
				ResultType data = cache.fetchFromNet(key);

				return cache.saveInCache(key, data);				
			}
		}
	}
	
	public ResultType getSync(KeyType key) {
		return getFutureResultNullOnException(getAsync(key));
	}

	public Future<ResultType> getAsync(KeyType key) {
		if (key == null) {
			throw new IllegalArgumentException("null key passed to " + getEjbIface().getName());
		}
		
		try {
			ResultType result = checkCache(key);
			// result may be null, but in that case we cached null
			return new KnownFuture<ResultType>(result);
		} catch (NotCachedException e) {
			return getExecutor().execute(key, new AbstractBasicCacheTask<KeyType,ResultType>(key, getEjbIface()));	
		}
	}

	protected abstract EntityType queryExisting(KeyType key);
	
	protected abstract ResultType resultFromEntity(EntityType entity);
	
	protected abstract EntityType entityFromResult(KeyType key, ResultType result);
	
	protected abstract void updateEntityFromResult(KeyType key, ResultType result, EntityType entity);
	
	public ResultType checkCache(KeyType key) throws NotCachedException {
		EJBUtil.assertHaveTransaction();
		
		EntityType result = queryExisting(key);

		if (result == null) {
			throw new NotCachedException();
		} else {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + getExpirationTime()) < now)) {
				throw new NotCachedException();
			}
		
			logger.debug("Have cached result for key {}: {}", result, key);
			
			if (result.isNoResultsMarker()) {
				return null;
			} else {	
				return resultFromEntity(result);
			}
		}
	}

	protected abstract EntityType newNoResultsMarker(KeyType key);
	
	// null data means to save a negative result
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public ResultType saveInCache(final KeyType key, final ResultType data) {
		EJBUtil.assertNoTransaction();
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<ResultType>() {
				public ResultType call() {
					EntityType e = queryExisting(key);
					if (e == null) {
						if (data == null) {
							e = newNoResultsMarker(key);
							if (!e.isNoResultsMarker()) {
								throw new RuntimeException("Newly-returned no results marker isn't: " + e);
							}
						} else {
							e = entityFromResult(key, data);
						}
						em.persist(e);
					} else {
						// don't ever save a negative result once we have data at some point
						if (data != null) {
							updateEntityFromResult(key, data, e);
						}
					}
					e.setLastUpdated(new Date());
					
					logger.debug("Saved new cached item under {}: {}", 
						     key, e);
					
					if (e.isNoResultsMarker())
						return null;
					else
						return resultFromEntity(e);
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
}
