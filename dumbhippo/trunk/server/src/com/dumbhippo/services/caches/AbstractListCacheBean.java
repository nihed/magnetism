package com.dumbhippo.services.caches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.KnownFuture;
import com.dumbhippo.persistence.CachedListItem;
import com.dumbhippo.server.util.EJBUtil;

public abstract class AbstractListCacheBean<KeyType,ResultType,EntityType extends CachedListItem>
	extends AbstractCacheBean<KeyType,List<ResultType>,AbstractListCache<KeyType,ResultType>>
	implements AbstractListCache<KeyType, ResultType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractListCacheBean.class);
	
	protected AbstractListCacheBean(Request defaultRequest, Class<? extends AbstractListCache<KeyType,ResultType>> ejbIface, long expirationTime) {
		super(defaultRequest, ejbIface, expirationTime);
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

	protected abstract List<EntityType> queryExisting(KeyType key);
	
	protected abstract ResultType resultFromEntity(EntityType entity);
	
	protected abstract EntityType entityFromResult(KeyType key, ResultType result);
	
	public List<ResultType> checkCache(KeyType key) throws NotCachedException {
		List<EntityType> old = queryExisting(key);

		if (old.isEmpty())
			throw new NotCachedException();
		
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (EntityType d : old) {
			if ((d.getLastUpdated().getTime() + getExpirationTime()) < now) {
				outdated = true;
			}
			if (d.isNoResultsMarker()) {
				haveNoResultsMarker = true;
			}
		}
		
		if (outdated) {
			logger.debug("Cache appears outdated for bean {} key {}", getEjbIface().getName(), key);
			throw new NotCachedException();
		}
		
		if (haveNoResultsMarker) {
			logger.debug("Negative result cached for bean {} key {}", getEjbIface().getName(), key);
			return Collections.emptyList();
		}
		
		List<ResultType> results = new ArrayList<ResultType>();
		for (EntityType e : old) {
			results.add(resultFromEntity(e));
		}
		
		return results;		
	}

	protected abstract EntityType newNoResultsMarker(KeyType key);

	protected void setAllLastUpdatedToZero(KeyType key) {
		throw new UnsupportedOperationException("Cache doesn't support manual expiration: " + getEjbIface().getName());
	}
	
	@Override
	public void expireCache(KeyType key) {
		setAllLastUpdatedToZero(key);
	}
	
	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public List<ResultType> saveInCacheInsideExistingTransaction(KeyType key, List<ResultType> newItems, Date now) {
		EJBUtil.assertHaveTransaction();
		
		// null results doesn't happen right now but if it did would be the same as empty list
		if (newItems == null)
			newItems = Collections.emptyList(); 
	
		logger.debug("Saving new results in cache for bean {}", getEjbIface().getName());

		List<EntityType> old = queryExisting(key);
		for (EntityType d : old) {
			em.remove(d);
		}

		// This is perhaps superstitious, but we do have an ordering constraint that we must 
		// remove the old items then insert the new, or it will cause a constraint violation
		em.flush();
		
		// save new results
		if (newItems.isEmpty()) {
			EntityType e = newNoResultsMarker(key);
			if (!e.isNoResultsMarker())
				throw new RuntimeException("new no results marker isn't: " + e);
			e.setLastUpdated(now);
			em.persist(e);
		} else {
			for (ResultType r : newItems) {							
				EntityType e = entityFromResult(key, r);
				e.setLastUpdated(now);
				em.persist(e);
			}
		}
		
		return newItems;
	}
}
