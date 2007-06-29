package com.dumbhippo.services.caches;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedAmazonList;
import com.dumbhippo.persistence.CachedAmazonLists;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.AmazonListView;
import com.dumbhippo.services.AmazonLists;
import com.dumbhippo.services.AmazonListsView;
import com.dumbhippo.services.AmazonWebServices;
import com.dumbhippo.tx.TxUtils;

//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class AmazonListsCacheBean extends AbstractBasicCacheBean<String,AmazonListsView> implements
		AmazonListsCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonListsCacheBean.class);

	// we explicitly drop the cache if we think there is a change, but we also need to update
	// amazon list information every 24 hours
	static private final long AMAZON_LISTS_EXPIRATION = 1000 * 60 * 60 * 24;
	
	private BasicCacheStorage<String,AmazonListsView,CachedAmazonLists> summaryStorage;
	private ListCacheStorage<String,AmazonListView,CachedAmazonList> listListStorage; // a list of amazon wish lists
	
	public AmazonListsCacheBean() {
		super(Request.AMAZON_LISTS, AmazonListsCache.class, AMAZON_LISTS_EXPIRATION);
	}

	@PostConstruct
	public void init() {
		BasicCacheStorageMapper<String,AmazonListsView,CachedAmazonLists> summaryMapper =
			new BasicCacheStorageMapper<String,AmazonListsView,CachedAmazonLists>() {

				public CachedAmazonLists newNoResultsMarker(String key) {
					TxUtils.assertHaveTransaction();
					
					return CachedAmazonLists.newNoResultsMarker(key);
				}

				public CachedAmazonLists queryExisting(String key) {
					TxUtils.assertHaveTransaction();
					
					Query q = em.createQuery("SELECT lists FROM CachedAmazonLists lists WHERE lists.amazonUserId = :amazonUserId");
					q.setParameter("amazonUserId", key);
					
					try {
						return (CachedAmazonLists) q.getSingleResult();
					} catch (NoResultException e) {
						return null;
					}
				}

				public AmazonListsView resultFromEntity(CachedAmazonLists entity) {
					return entity.toAmazonLists();
				}

				public CachedAmazonLists entityFromResult(String key, AmazonListsView result) {
					return new CachedAmazonLists(key, result);
				}

				public void updateEntityFromResult(String key, AmazonListsView result, CachedAmazonLists entity) {
					entity.update(result);
				}			
		};
		
		ListCacheStorageMapper<String,AmazonListView,CachedAmazonList> listListMapper =
			new ListCacheStorageMapper<String,AmazonListView,CachedAmazonList>() {
			
			public List<CachedAmazonList> queryExisting(String key) {
				Query q = em.createQuery("SELECT list FROM CachedAmazonList list WHERE list.amazonUserId = :amazonUserId");
				q.setParameter("amazonUserId", key);
				
				List<CachedAmazonList> results = TypeUtils.castList(CachedAmazonList.class, q.getResultList());
				return results;
			}
			
			public void setAllLastUpdatedToZero(String key) {
				EJBUtil.prepareUpdate(em, CachedAmazonList.class);
				
				Query q = em.createQuery("UPDATE CachedAmazonList c" + 
						" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
						" WHERE c.amazonUserId = :amazonUserId");
				q.setParameter("amazonUserId", key);
				int updated = q.executeUpdate();
				logger.debug("{} cached items expired", updated);
			}				

			public AmazonListView resultFromEntity(CachedAmazonList entity) {
				return entity.toAmazonList();
			}

			public CachedAmazonList entityFromResult(String key, AmazonListView result) {
				return new CachedAmazonList(key, result);
			}
			
			public CachedAmazonList newNoResultsMarker(String key) {
				return CachedAmazonList.newNoResultsMarker(key);
			}
		};
		
		summaryStorage = new BasicCacheStorage<String,AmazonListsView,CachedAmazonLists>(em, getExpirationTime(), summaryMapper);
		listListStorage = new ListCacheStorage<String,AmazonListView,CachedAmazonList>(em, getExpirationTime(), AmazonListView.class, listListMapper);
	}	
	

	@Override
	protected AmazonListsView fetchFromNetImpl(String key) {
		AmazonWebServices ws = new AmazonWebServices(REQUEST_TIMEOUT, config);
		// this one only gets bare bones list ids
		AmazonLists lists = ws.lookupLists(key);
		return lists;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public AmazonListsView checkCache(String key) throws NotCachedException {
		AmazonListsView summary = summaryStorage.checkCache(key);
		List<? extends AmazonListView> listList = listListStorage.checkCache(key);
		summary.setLists(listList, false);
		return summary;
	}

	public AmazonListView queryExisting(String key, String listId) {
		Query q = em.createQuery("SELECT list FROM CachedAmazonList list " +
                "WHERE list.amazonUserId = :amazonUserId " +
                "AND list.listId = :listId");
        q.setParameter("amazonUserId", key);	
        q.setParameter("listId", listId);

        try {
            return ((CachedAmazonList)q.getSingleResult()).toAmazonList();
        } catch (NoResultException e) {
            return null;
        }
	}
	
	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AmazonListsView saveInCacheInsideExistingTransaction(String key, AmazonListsView data, Date now, boolean refetchedWithoutCheckingCache) {
		AmazonListsView refreshedLists = data;
        // AmazonListsView we are getting here will only contain list ids
		if (refetchedWithoutCheckingCache) {
			try {
				AmazonListsView old = checkCache(key);
				if (old == null && data == null) {
					return new AmazonLists();
				} else if (data == null) {
					return old;
				}
				
				// we want to get the actual lists to compare their ids
				// this should not be costly, because people are only likely to have a few public
				// wish lists, on the other hand it is likely that someone would remove one list
				// and add another in the same period of time
				List<? extends AmazonListView> oldLists = listListStorage.checkCache(key);
		        
				Map<String,AmazonListView> oldListsByListId = new HashMap<String,AmazonListView>();
		        for (AmazonListView oldList : oldLists) {
			        oldListsByListId.put(oldList.getListId(), oldList);
		        }
		        
		        boolean allMatch = true;
		        // even if some lists have changed, we don't want to loose the information we are storing
		        // about other lists, so we keep the refreshedLists that would only have a set of new
		        // lists, but would retain the information we have about old lists if they are still around
		        refreshedLists = new AmazonLists();
				for (AmazonListView newList : data.getLists()) {
					AmazonListView matchingOldList = oldListsByListId.get(newList.getListId());
					// we do not get names as part of this lookup, otherwise we would have check if 
					// the name changed too
					if (matchingOldList == null)  {
						allMatch = false;
						// we don't break here because we want to save over all old lists that
						// are still around
					} else {
						refreshedLists.addList(matchingOldList, true);
						oldListsByListId.remove(newList.getListId());
					}
				}
				if (oldListsByListId.isEmpty() && allMatch)
					return old;
				else
					refreshedLists.addLists(oldListsByListId.values(), true);
			} catch (NotCachedException e) {
				// This could happen if we never cached anything for this key, or if
				// the cache expired. We need to use the new results, or save a new 
				// no-results marker.
			}
		}
		
		AmazonListsView summary = summaryStorage.saveInCacheInsideExistingTransaction(key, refreshedLists, now, refetchedWithoutCheckingCache);
		// if there are no results, we want to save a NoResultsMarker for the list as well
  	    List<? extends AmazonListView> listList = listListStorage.saveInCacheInsideExistingTransaction(key, refreshedLists != null ? refreshedLists.getLists() : null, now, refetchedWithoutCheckingCache);
		if (summary != null) {
			summary.setLists(listList, false);
		} else {
			summary = new AmazonLists();
		}
		
		return summary;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public void expireCache(String key) {
		TxUtils.assertHaveTransaction();
		
		// we need to implement expireCache for BasicCacheBean
		// summaryStorage.expireCache(key);
		listListStorage.expireCache(key);
	}
}