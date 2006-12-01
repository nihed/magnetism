package com.dumbhippo.services.caches;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedListItem;
import com.dumbhippo.server.util.EJBUtil;

public class ListCacheStorage<KeyType, ResultType, EntityType extends CachedListItem>
		extends AbstractCacheStorage<KeyType, List<? extends ResultType>, EntityType> {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ListCacheStorage.class);
	
	private ListCacheStorageMapper<KeyType,ResultType,EntityType> mapper;
	private Class<ResultType> resultClass;
	
	protected ListCacheStorage(EntityManager em, long expirationTime, Class<ResultType> resultClass, ListCacheStorageMapper<KeyType,ResultType,EntityType> mapper) {
		super(em, expirationTime);
		this.mapper = mapper;
		this.resultClass = resultClass;
	}
	
	public List<? extends ResultType> checkCache(KeyType key) throws NotCachedException {
		List<EntityType> oldItems = mapper.queryExisting(key);

		if (oldItems.isEmpty())
			throw new NotCachedException();
		
		long now = System.currentTimeMillis();
		boolean outdated = false;
		boolean haveNoResultsMarker = false;
		for (EntityType d : oldItems) {
			if ((d.getLastUpdated().getTime() + getExpirationTime()) < now) {
				outdated = true;
			}
			if (d.isNoResultsMarker()) {
				haveNoResultsMarker = true;
			}
		}
		
		if (outdated) {
			logger.debug("Cache appears outdated for key {}", key);
			throw new ExpiredCacheException();
		}
		
		if (haveNoResultsMarker) {
			logger.debug("Negative result cached for key {}", key);
			return TypeUtils.emptyList(resultClass);
		}
		
		return formResultTypeList(oldItems);		
	}

	private List<ResultType> formResultTypeList(List<EntityType> list) {
		List<ResultType> results = new ArrayList<ResultType>();
		for (EntityType e : list) {
			results.add(mapper.resultFromEntity(e));
		}		
		return results;			
	}

	@Override
	public void expireCache(KeyType key) {
		mapper.setAllLastUpdatedToZero(key);
	}
	
	// null means that we could not get the updated results, so leave the old results
	// empty list results means that we should save a no results marker
	public List<? extends ResultType> saveInCacheInsideExistingTransaction(KeyType key, List<? extends ResultType> newItems, Date now, boolean refetchedWithoutCheckingCache) {
		EJBUtil.assertHaveTransaction();
	
		logger.debug("Saving new results in cache");

		List<EntityType> oldItems = mapper.queryExisting(key);		
		if (newItems == null)
		 	return formResultTypeList(oldItems);	
		
		if (!oldItems.isEmpty())
		    deleteCache(key);
		
		// This is perhaps superstitious, but we do have an ordering constraint that we must 
		// remove the old items then insert the new, or it will cause a constraint violation
		em.flush();
		
		// save new results
		if (newItems.isEmpty()) {
			EntityType e = mapper.newNoResultsMarker(key);
			if (!e.isNoResultsMarker())
				throw new RuntimeException("new no results marker isn't: " + e);
			e.setLastUpdated(now);
			em.persist(e);
			logger.debug("cached a no results marker for key {}", key);			
		} else {
			for (ResultType r : newItems) {							
				EntityType e = mapper.entityFromResult(key, r);
				e.setLastUpdated(now);
				em.persist(e);
			}
			logger.debug("cached {} items for key {}", newItems.size(), key);
		}
		
		return newItems;
	}
	
	public void deleteCache(KeyType key) {
		List<EntityType> oldItems = mapper.queryExisting(key);
		for (EntityType d : oldItems) {
			em.remove(d);
		}		
	}
}