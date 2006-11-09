package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import com.dumbhippo.KnownFuture;
import com.dumbhippo.persistence.CachedListItem;
import com.dumbhippo.server.AbstractListCache;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;

public abstract class AbstractListCacheBean<KeyType,ResultType,EntityType extends CachedListItem> extends AbstractCacheBean<KeyType,List<ResultType>> implements AbstractListCache<KeyType, ResultType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AbstractListCacheBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	protected EntityManager em;
	
	@EJB
	protected TransactionRunner runner;
	
	@EJB
	protected Configuration config;		
	
	private Class<? extends AbstractListCache<KeyType,ResultType>> ejbIface;
	private long expirationTime; // in milliseconds until we expire the cache
	
	protected AbstractListCacheBean(Request defaultRequest, Class<? extends AbstractListCache<KeyType,ResultType>> ejbIface, long expirationTime) {
		super(defaultRequest);
		this.ejbIface = ejbIface;
		this.expirationTime = expirationTime;
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

			// we do this instead of an inner class to work right with threads
			AbstractListCache<KeyType,ResultType> cache = EJBUtil.defaultLookup(ejbIface);
			
			// Check again in case another node stored the data first
			List<ResultType> alreadyStored = cache.checkCache(key);
			if (alreadyStored != null)
				return alreadyStored;
			
			List<ResultType> result = cache.fetchFromNet(key);
			
			return cache.saveInCache(key, result);
		}
	}	
	
	public List<ResultType> getSync(KeyType key) {
		return getFutureResultEmptyListOnException(getAsync(key));
	}

	public Future<List<ResultType>> getAsync(KeyType key) {
		if (key == null)
			throw new IllegalArgumentException("null key passed to AbstractListCacheBean");
		
		List<ResultType> results = checkCache(key);
		if (results != null) {
			logger.debug("Using cached listing of {} items for {}", results.size(), key);
			return new KnownFuture<List<ResultType>>(results);
		}
		Callable<List<ResultType>> task = new AbstractListCacheTask<KeyType,ResultType,EntityType>(key, ejbIface);
		return getExecutor().execute(key, task);
	}

	protected abstract List<EntityType> queryExisting(KeyType key);
	
	protected abstract ResultType resultFromEntity(EntityType entity);
	
	protected abstract EntityType entityFromResult(KeyType key, ResultType result);
	
	public List<ResultType> checkCache(KeyType key) {
		List<EntityType> old = queryExisting(key);

		if (old.isEmpty())
			return null;
		
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (EntityType d : old) {
			if ((d.getLastUpdated().getTime() + expirationTime) < now) {
				outdated = true;
			}
			if (d.isNoResultsMarker()) {
				haveNoResultsMarker = true;
			}
		}
		
		if (outdated) {
			logger.debug("Cache appears outdated for bean {} key {}", ejbIface.getName(), key);
			return null;
		}
		
		if (haveNoResultsMarker) {
			logger.debug("Negative result cached for bean {} key {}", ejbIface.getName(), key);
			return Collections.emptyList();
		}
		
		List<ResultType> results = new ArrayList<ResultType>();
		for (EntityType e : old) {
			results.add(resultFromEntity(e));
		}
		
		return results;		
	}

	protected abstract List<ResultType> fetchFromNetImpl(KeyType key);
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<ResultType> fetchFromNet(KeyType key) {
		return fetchFromNetImpl(key);
	}

	protected abstract EntityType newNoResultsMarker(KeyType key);

	protected abstract void setAllLastUpdatedToZero(KeyType key);
	
	@Override
	public void expireCache(KeyType key) {
		setAllLastUpdatedToZero(key);
	}
	
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public List<ResultType> saveInCache(final KeyType key, List<ResultType> newResults) {
		// null results doesn't happen right now but if it did would be the same as empty list
		if (newResults == null)
			newResults = Collections.emptyList(); 
		
		final List<ResultType> newItems = newResults; // the method param is reassigned above
		
		try {
			return runner.runTaskInNewTransaction(new Callable<List<ResultType>>() {
				public List<ResultType> call() {
					
					logger.debug("Saving new results in cache for bean {}", ejbIface.getName());

					List<EntityType> old = queryExisting(key);
					for (EntityType d : old) {
						em.remove(d);
					}					
					
					Date now = new Date();
					
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
				
			});
		} catch (Exception e) {
			if (EJBUtil.isDatabaseException(e)) {
				logger.warn("Ignoring database exception saving in cache for " + ejbIface.getName() + " {}: {}", e.getClass().getName(), e.getMessage());
				return newItems;
			} else {
				ExceptionUtils.throwAsRuntimeException(e);
				throw new RuntimeException(e); // not reached
			}
		}
	}
}
