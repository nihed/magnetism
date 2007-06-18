package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedAmazonList;
import com.dumbhippo.persistence.CachedAmazonListItem;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.AmazonList;
import com.dumbhippo.services.AmazonListItemView;
import com.dumbhippo.services.AmazonListView;
import com.dumbhippo.services.AmazonWebServices;
import com.dumbhippo.tx.TxUtils;

//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class AmazonListItemsCacheBean extends AbstractBasicCacheBean<Pair<String, String>,AmazonListView> implements
		AmazonListItemsCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonListItemsCacheBean.class);

	// we explicitly drop the cache if we think there is a change, but we also need to update
	// amazon list information every 24 hours
	static private final long AMAZON_LIST_ITEMS_EXPIRATION = 1000 * 60 * 60 * 24;
	
	private BasicCacheStorage<Pair<String, String>,AmazonListView,CachedAmazonList> summaryStorage;
	private ListCacheStorage<String,AmazonListItemView,CachedAmazonListItem> itemListStorage;
	
	public AmazonListItemsCacheBean() {
		super(Request.AMAZON_LIST_ITEMS, AmazonListItemsCache.class, AMAZON_LIST_ITEMS_EXPIRATION);
	}

	@PostConstruct
	public void init() {
		BasicCacheStorageMapper<Pair<String, String>,AmazonListView,CachedAmazonList> summaryMapper =
			new BasicCacheStorageMapper<Pair<String, String>,AmazonListView,CachedAmazonList>() {

				public CachedAmazonList newNoResultsMarker(Pair<String, String> key) {
					TxUtils.assertHaveTransaction();
					
					return CachedAmazonList.newNoResultsMarker(key.getFirst(), key.getSecond());
				}

				public CachedAmazonList queryExisting(Pair<String, String> key) {
					TxUtils.assertHaveTransaction();
					
					Query q = em.createQuery("SELECT list FROM CachedAmazonList list WHERE list.amazonUserId = :amazonUserId" +
							                 " AND list.listId = :listId");
					q.setParameter("amazonUserId", key.getFirst());
					q.setParameter("listId", key.getSecond());
					
					try {
						return (CachedAmazonList) q.getSingleResult();
					} catch (NoResultException e) {
						return null;
					}
				}

				public AmazonListView resultFromEntity(CachedAmazonList entity) {
					return entity.toAmazonList();
				}

				public CachedAmazonList entityFromResult(Pair<String, String> key, AmazonListView result) {
					return new CachedAmazonList(key.getFirst(), result);
				}

				public void updateEntityFromResult(Pair<String, String> key, AmazonListView result, CachedAmazonList entity) {
					entity.update(result);
				}			
		};
		
		ListCacheStorageMapper<String,AmazonListItemView,CachedAmazonListItem> itemListMapper =
			new ListCacheStorageMapper<String,AmazonListItemView,CachedAmazonListItem>() {
			
			public List<CachedAmazonListItem> queryExisting(String key) {
				Query q = em.createQuery("SELECT listItem FROM CachedAmazonListItem listItem WHERE listItem.listId = :listId");
				q.setParameter("listId", key);
				
				List<CachedAmazonListItem> results = TypeUtils.castList(CachedAmazonListItem.class, q.getResultList());
				return results;
			}
			
			public void setAllLastUpdatedToZero(String key) {
				EJBUtil.prepareUpdate(em, CachedAmazonList.class);
				
				Query q = em.createQuery("UPDATE CachedAmazonListItem c" + 
						" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
						" WHERE c.listId = :listId");
				q.setParameter("listId", key);
				int updated = q.executeUpdate();
				logger.debug("{} cached items expired", updated);
			}				

			public AmazonListItemView resultFromEntity(CachedAmazonListItem entity) {
				return entity.toAmazonListItem();
			}

			public CachedAmazonListItem entityFromResult(String key, AmazonListItemView result) {
				return new CachedAmazonListItem(key, result);
			}
			
			public CachedAmazonListItem newNoResultsMarker(String key) {
				return CachedAmazonListItem.newNoResultsMarker(key);
			}
		};
		
		summaryStorage = new BasicCacheStorage<Pair<String, String>,AmazonListView,CachedAmazonList>(em, getExpirationTime(), summaryMapper);
		itemListStorage = new ListCacheStorage<String,AmazonListItemView,CachedAmazonListItem>(em, getExpirationTime(), AmazonListItemView.class, itemListMapper);
	}	
	

	@Override
	protected AmazonListView fetchFromNetImpl(Pair<String, String> key) {
		AmazonWebServices ws = new AmazonWebServices(REQUEST_TIMEOUT, config);
		// this one should get a list with list details and as many items
		// as Amazon web services return per list (300)
		AmazonList list = ws.lookupListItems(key.getFirst(), key.getSecond());
		return list;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public AmazonListView checkCache(Pair<String, String> key) throws NotCachedException {
		AmazonListView summary = summaryStorage.checkCache(key);
		List<? extends AmazonListItemView> itemList = itemListStorage.checkCache(key.getSecond());
		summary.setListItems(itemList);
		return summary;
	}

	public AmazonListItemView queryExisting(Pair<String, String> key, String itemId) {
		Query q = em.createQuery("SELECT listItem FROM CachedAmazonListItem listItem " +
                "WHERE listItem.listId = :listId " +
                "AND listItem.itemId = :itemId");
        q.setParameter("listId", key.getSecond());	
        q.setParameter("itemId", itemId);

        try {
            return ((CachedAmazonListItem)q.getSingleResult()).toAmazonListItem();
        } catch (NoResultException e) {
            return null;
        }
	}
	
	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AmazonListView saveInCacheInsideExistingTransaction(Pair<String, String> key, AmazonListView data, Date now, boolean refetchedWithoutCheckingCache) {
		if (refetchedWithoutCheckingCache) {
			try {
				// Try to avoid saving to the cache if the list items count hasn't changed. (Yes, this could lead to not 
				// getting new info if someone adds and deletes items for a net change of 0; but after the expiration of 
				// the cache, checkCache here will return NotCachedException and we'll recover. Or as soon as someone 
				// adds or removes another item we'll recover.)
				AmazonListView old = checkCache(key);
				// chechCache for BasicCacheStorage will return null if we have a NoResultsMarker cached
				if (old == null && data == null) {
				    // data will be null if there was an error, old will be null if we have a no results marker stored
					return new AmazonList();					
				} else if (data == null || 
						  (data != null && old != null && old.getTotalItems() == data.getTotalItems() && old.getListName().equals(data.getListName()))) {
					// If data is null, but old is not null, we should keep around the old list items until they expire.
					// This is possible if the list was deleted, but since there are no updates we'd want to make
				    // in that situation, we shouldn't rush to delete what we have.
					return old;
			    }
			} catch (NotCachedException e) {
				// This could happen if we never cached anything for this key, or if
				// the cache expired. We need to use the new results, or save a new 
				// no-results marker.
			}
		}
		
		AmazonListView summary = summaryStorage.saveInCacheInsideExistingTransaction(key, data, now, refetchedWithoutCheckingCache);
		// if there are no results, we want to save a NoResultsMarker for the list as well
  	    List<? extends AmazonListItemView> itemList = itemListStorage.saveInCacheInsideExistingTransaction(key.getSecond(), data != null ? data.getListItems() : null, now, refetchedWithoutCheckingCache);
		if (summary != null) {
			summary.setListItems(itemList);
		} else {
			summary = new AmazonList();
		}
		
		return summary;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public void expireCache(Pair<String, String> key) {
		TxUtils.assertHaveTransaction();
		
		summaryStorage.expireCache(key);
		itemListStorage.expireCache(key.getSecond());
	}
}