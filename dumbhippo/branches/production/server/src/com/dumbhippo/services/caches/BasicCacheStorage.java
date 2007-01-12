package com.dumbhippo.services.caches;

import java.util.Date;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.CachedItem;
import com.dumbhippo.server.util.EJBUtil;

public class BasicCacheStorage<KeyType, ResultType, EntityType extends CachedItem>
		extends AbstractCacheStorage<KeyType, ResultType, EntityType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(BasicCacheStorage.class);
	
	private BasicCacheStorageMapper<KeyType,ResultType,EntityType> mapper;
	
	protected BasicCacheStorage(EntityManager em, long expirationTime, BasicCacheStorageMapper<KeyType,ResultType,EntityType> mapper) {
		super(em, expirationTime);
		this.mapper = mapper;
	}
		
	public ResultType checkCache(KeyType key) throws NotCachedException {
		EJBUtil.assertHaveTransaction();
		
		EntityType result = mapper.queryExisting(key);

		if (result == null) {
			throw new NotCachedException();
		} else {
			long now = System.currentTimeMillis();
			Date lastUpdated = result.getLastUpdated();
			if (lastUpdated == null || ((lastUpdated.getTime() + getExpirationTime()) < now)) {
				throw new ExpiredCacheException();
			}
		
			logger.debug("Have cached result for key {}: {}", result, key);
			
			if (result.isNoResultsMarker()) {
				return null;
			} else {	
				return mapper.resultFromEntity(result);
			}
		}
	}
	
	// null data means to save a negative result
	public ResultType saveInCacheInsideExistingTransaction(KeyType key, ResultType data, Date now, boolean refetchedWithoutCheckingCache) {
		EJBUtil.assertHaveTransaction();
		
		EntityType e = mapper.queryExisting(key);
		if (e == null) {
			if (data == null) {
				e = mapper.newNoResultsMarker(key);
				if (!e.isNoResultsMarker()) {
					throw new RuntimeException("Newly-returned no results marker isn't: " + e);
				}
			} else {
				e = mapper.entityFromResult(key, data);
			}
			// the setLastUpdated has to be before the persist() since lastUpdated isn't nullable,
			// or hibernate gets upset
			e.setLastUpdated(now);
			em.persist(e);
		} else {
			// TODO: what if the data is really no longer there?!
			// in any case, we probably should not set last updated time to now if data == null
			e.setLastUpdated(now);
			// don't ever save a negative result once we have data at some point
			if (data != null) {
				mapper.updateEntityFromResult(key, data, e);
			}
		}
		
		logger.debug("Saved new cached item under {}: {}", key, e);
		
		if (e.isNoResultsMarker())
			return null;
		else
			return mapper.resultFromEntity(e);
	}
}
