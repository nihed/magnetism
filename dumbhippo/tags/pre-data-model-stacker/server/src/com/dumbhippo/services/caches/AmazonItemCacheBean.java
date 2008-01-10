package com.dumbhippo.services.caches;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.caches.CachedAmazonItem;
import com.dumbhippo.server.BanFromWebTier;
import com.dumbhippo.services.AmazonItem;
import com.dumbhippo.services.AmazonItemLookupWebServices;
import com.dumbhippo.services.AmazonItemView;

@BanFromWebTier
// @Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class AmazonItemCacheBean extends AbstractBasicCacheWithStorageBean<String,AmazonItemView,CachedAmazonItem> implements AmazonItemCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonItemCacheBean.class);
	
	// 24 hours because we are not using the price for now, if we ever decide to use it,
	// we should make this 1 hour, so that we can display the price in a simpler format
	static private final int AMAZON_ITEM_EXPIRATION_TIMEOUT = 1000 * 60 * 60 * 24;	
	
	public AmazonItemCacheBean() {
		super(Request.AMAZON_ITEM, AmazonItemCache.class, AMAZON_ITEM_EXPIRATION_TIMEOUT);
	}	
	
	@Override
	public CachedAmazonItem queryExisting(String itemId) {
		Query q;
		
		q = em.createQuery("FROM CachedAmazonItem item WHERE item.itemId = :itemId");
		q.setParameter("itemId", itemId);
		
		try {
			return (CachedAmazonItem) q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}	
		
	@Override
	protected AmazonItemView fetchFromNetImpl(String itemId) {
		AmazonItemLookupWebServices lookup = new AmazonItemLookupWebServices(REQUEST_TIMEOUT, config);
		final AmazonItem data = lookup.getItem(itemId);		
		return data;
	}

	@Override
	public AmazonItemView resultFromEntity(CachedAmazonItem entity) {
		return entity.toAmazonItem();
	}

	@Override
	public void updateEntityFromResult(String key, AmazonItemView result, CachedAmazonItem entity) {
		entity.update(result);
	}

	@Override
	public CachedAmazonItem entityFromResult(String key, AmazonItemView result) {
		return new CachedAmazonItem(key, result);
	}

	@Override
	public CachedAmazonItem newNoResultsMarker(String key) {
		return CachedAmazonItem.newNoResultsMarker(key);
	}
}
