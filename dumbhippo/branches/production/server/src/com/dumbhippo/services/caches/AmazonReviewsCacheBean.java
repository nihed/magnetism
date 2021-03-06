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
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.CachedAmazonReview;
import com.dumbhippo.persistence.CachedAmazonReviews;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.AmazonReviewView;
import com.dumbhippo.services.AmazonReviews;
import com.dumbhippo.services.AmazonReviewsView;
import com.dumbhippo.services.AmazonWebServices;
import com.dumbhippo.tx.TxUtils;

//@Stateless // for now, these cache beans are our own special kind of bean and not EJBs due to a jboss bug
public class AmazonReviewsCacheBean extends AbstractBasicCacheBean<String,AmazonReviewsView> implements
		AmazonReviewsCache {

	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonReviewsCacheBean.class);

	// we explicitly drop the cache if we think there is a change, but we also need to update
	// amazon review information every 24 hours
	static private final long AMAZON_REVIEWS_EXPIRATION = 1000 * 60 * 60 * 24;
	
	private BasicCacheStorage<String,AmazonReviewsView,CachedAmazonReviews> summaryStorage;
	private ListCacheStorage<String,AmazonReviewView,CachedAmazonReview> reviewListStorage;
	
	public AmazonReviewsCacheBean() {
		super(Request.AMAZON_REVIEWS, AmazonReviewsCache.class, AMAZON_REVIEWS_EXPIRATION);
	}

	@PostConstruct
	public void init() {
		BasicCacheStorageMapper<String,AmazonReviewsView,CachedAmazonReviews> summaryMapper =
			new BasicCacheStorageMapper<String,AmazonReviewsView,CachedAmazonReviews>() {

				public CachedAmazonReviews newNoResultsMarker(String key) {
					TxUtils.assertHaveTransaction();
					
					return CachedAmazonReviews.newNoResultsMarker(key);
				}

				public CachedAmazonReviews queryExisting(String key) {
					TxUtils.assertHaveTransaction();
					
					Query q = em.createQuery("SELECT reviews FROM CachedAmazonReviews reviews WHERE reviews.amazonUserId = :amazonUserId");
					q.setParameter("amazonUserId", key);
					
					try {
						return (CachedAmazonReviews) q.getSingleResult();
					} catch (NoResultException e) {
						return null;
					}
				}

				public AmazonReviewsView resultFromEntity(CachedAmazonReviews entity) {
					return entity.toAmazonReviews();
				}

				public CachedAmazonReviews entityFromResult(String key, AmazonReviewsView result) {
					return new CachedAmazonReviews(key, result);
				}

				public void updateEntityFromResult(String key, AmazonReviewsView result, CachedAmazonReviews entity) {
					entity.update(result);
				}
			
		};
		
		ListCacheStorageMapper<String,AmazonReviewView,CachedAmazonReview> reviewListMapper =
			new ListCacheStorageMapper<String,AmazonReviewView,CachedAmazonReview>() {
			
			public List<CachedAmazonReview> queryExisting(String key) {
				Query q = em.createQuery("SELECT review FROM CachedAmazonReview review WHERE review.amazonUserId = :amazonUserId");
				q.setParameter("amazonUserId", key);
				
				List<CachedAmazonReview> results = TypeUtils.castList(CachedAmazonReview.class, q.getResultList());
				return results;
			}
			
			public void setAllLastUpdatedToZero(String key) {
				EJBUtil.prepareUpdate(em, CachedAmazonReview.class);
				
				Query q = em.createQuery("UPDATE CachedAmazonReview c" + 
						" SET c.lastUpdated = '1970-01-01 00:00:00' " + 
						" WHERE c.amazonUserId = :amazonUserId");
				q.setParameter("amazonUserId", key);
				int updated = q.executeUpdate();
				logger.debug("{} cached items expired", updated);
			}				

			public AmazonReviewView resultFromEntity(CachedAmazonReview entity) {
				return entity.toAmazonReview();
			}

			public CachedAmazonReview entityFromResult(String key, AmazonReviewView result) {
				return new CachedAmazonReview(key, result);
			}
			
			public CachedAmazonReview newNoResultsMarker(String key) {
				return CachedAmazonReview.newNoResultsMarker(key);
			}
		};
		
		summaryStorage = new BasicCacheStorage<String,AmazonReviewsView,CachedAmazonReviews>(em, getExpirationTime(), summaryMapper);
		reviewListStorage = new ListCacheStorage<String,AmazonReviewView,CachedAmazonReview>(em, getExpirationTime(), AmazonReviewView.class, reviewListMapper);
	}	
	

	@Override
	protected AmazonReviewsView fetchFromNetImpl(String key) {
		AmazonWebServices ws = new AmazonWebServices(REQUEST_TIMEOUT, config);
		AmazonReviews reviews = ws.lookupReviews(key);
		return reviews;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public AmazonReviewsView checkCache(String key) throws NotCachedException {
		AmazonReviewsView summary = summaryStorage.checkCache(key);
		List<? extends AmazonReviewView> reviewList = reviewListStorage.checkCache(key);
		summary.setReviews(reviewList, false);
		return summary;
	}

	public AmazonReviewView queryExisting(String key, String itemId) {
		Query q = em.createQuery("SELECT review FROM CachedAmazonReview review " +
                "WHERE review.amazonUserId = :amazonUserId " +
                "AND review.itemId = :itemId");
        q.setParameter("amazonUserId", key);	
        q.setParameter("itemId", itemId);

        try {
            return ((CachedAmazonReview)q.getSingleResult()).toAmazonReview();
        } catch (NoResultException e) {
            return null;
        }
	}
	
	@TransactionAttribute(TransactionAttributeType.MANDATORY)
	public AmazonReviewsView saveInCacheInsideExistingTransaction(String key, AmazonReviewsView data, Date now, boolean refetchedWithoutCheckingCache) {
		if (refetchedWithoutCheckingCache) {
			try {
				// Try to avoid saving to the cache if the review count hasn't changed. (Yes, this could lead to not 
				// getting new info if someone adds and deletes reviews for a net change of 0; but after the expiration of 
				// the cache, checkCache here will return NotCachedException and we'll recover. Or as soon as someone 
				// adds or removes another review we'll recover.)
				AmazonReviewsView old = checkCache(key);
				// chechCache for BasicCacheStorage will return null if we have a NoResultsMarker cached
				if (old == null && data == null) {
				    // data will be null if there was an error, old will be null if we have a no results marker stored
					return new AmazonReviews();					
				} else if (data == null || (data != null && old != null && old.getTotal() == data.getTotal())) {
					// If data is null, but old is not null, we should keep around the old reviews until they expire.
					// This is possible if the reviews were deleted, but since there are no updates we'd want to make
				    // in that situation, we shouldn't rush to delete them.
					return old;
			    }
			} catch (NotCachedException e) {
				// This could happen if we never cached anything for this key, or if
				// the cache expired. We need to use the new results, or save a new 
				// no-results marker.
			}
		}
		
		AmazonReviewsView summary = summaryStorage.saveInCacheInsideExistingTransaction(key, data, now, refetchedWithoutCheckingCache);
		// if there are no results, we want to save a NoResultsMarker for the list as well
  	    List<? extends AmazonReviewView> reviewList = reviewListStorage.saveInCacheInsideExistingTransaction(key, data != null ? data.getReviews() : null, now, refetchedWithoutCheckingCache);
		if (summary != null) {
			summary.setReviews(reviewList, false);
		} else {
			summary = new AmazonReviews();
		}
		
		return summary;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Override
	public void expireCache(String key) {
		TxUtils.assertHaveTransaction();
		
		summaryStorage.expireCache(key);
		reviewListStorage.expireCache(key);
	}
}