package com.dumbhippo.server.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.AmazonActivityStatus;
import com.dumbhippo.persistence.AmazonActivityType;
import com.dumbhippo.persistence.AmazonUpdateStatus;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.polling.PollResult;
import com.dumbhippo.polling.PollingTask;
import com.dumbhippo.polling.PollingTaskFamily;
import com.dumbhippo.server.AmazonUpdater;
import com.dumbhippo.server.CachedExternalUpdater;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.services.AmazonReviewView;
import com.dumbhippo.services.AmazonReviewsView;
import com.dumbhippo.services.AmazonWebServices;
import com.dumbhippo.services.caches.AmazonReviewsCache;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.CacheFactoryBean;
import com.dumbhippo.services.caches.NotCachedException;

@Stateless
public class AmazonUpdaterBean extends CachedExternalUpdaterBean<AmazonUpdateStatus> implements AmazonUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(AmazonUpdaterBean.class);		
	
	// how long to wait on the Amazon API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	@SuppressWarnings("unused")
	@EJB
	private Notifier notifier;
	
	// do away without AmazonWishListItemsCache for now, just use AmazonActivityStatus table to
	// keep what's on people's wish lists

	@EJB
	private CacheFactory cacheFactory;	
	
	@PostConstruct
	public void init() {
		cacheFactory.injectCaches(this);
	}
	
	@Override
	public Query getCachedStatusQuery(String amazonUserId) {
		Query q = em.createQuery("SELECT updateStatus FROM AmazonUpdateStatus updateStatus " +
				"WHERE updateStatus.amazonUserId = :amazonUserId");
		q.setParameter("amazonUserId", amazonUserId);
		return q;
	}
	
	// TODO: will be used in AmazonReviewBlockHandlerBean::onExternalAccountLovedAndEnabledMaybeChanged()
	public Collection<AmazonActivityStatus> getActivityStatusesForAmazonAccount(String amazonUserId) {
		Query q = em.createQuery("SELECT activityStatus FROM AmazonActivityStatus activityStatus WHERE " + 
				" activityStatus.amazonUserId = :amazonUserId");
		q.setParameter("amazonUserId", amazonUserId);
		return TypeUtils.castList(AmazonActivityStatus.class, q.getResultList());
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.NEVER)	
	public void doPeriodicUpdate(String amazonUserId) {
	    logger.warn("Not doing anything for Amazon periodic update for {}, a polling task is expected to be used", amazonUserId);	
	}
	
	// compute a "hash" (relies on the most recent reviews being first, 
	// so it always changes if there are new reviews)
	private String computeReviewsHash(List<? extends AmazonReviewView> reviewViews) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= reviewViews.size())
				break;
			sb.append(reviewViews.get(i).getItemId());
			sb.append("-"); // just leave it trailing on the last one, doesn't matter
		}
		return sb.toString();
	}
	
	public boolean saveUpdatedStatus(String amazonUserId, AmazonReviewsView reviewsView) { 
		if (reviewsView == null)
			throw new IllegalArgumentException("null reviewsView");

		logger.debug("Saving new amazon status for " + amazonUserId + ": reviews {}", reviewsView.getTotal());
		
		AmazonUpdateStatus updateStatus;
		boolean updateStatusFound = false;
		try {
			updateStatus = getCachedStatus(amazonUserId);
			updateStatusFound = true;
		} catch (NotFoundException e) {
			updateStatus = new AmazonUpdateStatus(amazonUserId);
			em.persist(updateStatus);
		}
		
		String reviewsHash = computeReviewsHash(reviewsView.getReviews());

		boolean reviewsChanged = false;
		
		if (!updateStatus.getReviewsHash().equals(reviewsHash)) {
			logger.debug("Most recent reviews changed '{}' -> '{}'",
					updateStatus.getReviewsHash(), reviewsHash);
			reviewsChanged = true;
			updateStatus.setReviewsHash(reviewsHash);
		}

		// Push the updateStatus object to the database. The theory here is that 
		// it keeps Hibernate from reordering saving the new UpdateStatus vs. 
		// the other work we're about to do below, which might help avoid 
		// deadlock situations with the 2nd level cache. 
		// (If two threads both do A,B that's safer than having one do A,B and
		// one B,A.) Since we're done with the updateStatus
		// at this point, it shouldn't hurt efficiency too much to do this.
		em.flush();
		
		
		// Go over all AmazonActivityStatuses for reviews, and figure out which
		// ones we don't have.
		// We don't do anything if a review is no longer around, we keep the
		// status around, but we will not be able to find review details the cache,
		// so we'll just not display any details in the block about the review.
		// We might also loose the details for very old reviews, because it appears
		// that Amazon web services would only return 100 most recent reviews, in theory
		// we should still be getting the count updated past 100, but we should test it on some
		// active customer from Amazon. (TODO)
		Query q = em.createQuery("SELECT activityStatus FROM AmazonActivityStatus activityStatus WHERE " + 
		                         "activityStatus.amazonUserId = :amazonUserId AND " +
				                 "activityStatus.activityType = " + AmazonActivityType.REVIEWED.ordinal());
        q.setParameter("amazonUserId", amazonUserId);
        List<AmazonActivityStatus> statuses = TypeUtils.castList(AmazonActivityStatus.class, q.getResultList());
    
        Map<String,AmazonReviewView> reviewsByItemId = new HashMap<String,AmazonReviewView>();
        for (AmazonReviewView review : reviewsView.getReviews()) {
	        reviewsByItemId.put(review.getItemId(), review);
        }

        // this will leave only reviews for which we don't have statuses in reviewsByItemId
		for (AmazonActivityStatus status : statuses) {            
			reviewsByItemId.remove(status.getItemId());
		}
		
		// remaining reviews require creating a new status object
		for (AmazonReviewView review : reviewsByItemId.values()) {
			AmazonActivityStatus status = new AmazonActivityStatus(amazonUserId, review.getItemId(), "", AmazonActivityType.REVIEWED);
			em.persist(status);
			
			logger.debug("created new Amazon review status {}", status);
	
			// If we did not have an updateStatus before, it means that we just initialized
			// this account, so we shouldn't create blocks for pre-existing reviews!
			if (updateStatusFound) {
			    // TODO: implement!
			    //notifier.onAmazonReviewCreated(status);
		    }
		}
		return reviewsChanged;
	}
	
	@Override
	protected ExternalAccountType getAccountType() {
		return ExternalAccountType.AMAZON;
	}

	@Override
	protected Class<? extends CachedExternalUpdater<AmazonUpdateStatus>> getUpdater() {
		return AmazonUpdater.class;
	}

	@Override
	protected PollingTaskFamilyType getTaskFamily() {
		return PollingTaskFamilyType.AMAZON;
	}

	private static class AmazonTaskFamily implements PollingTaskFamily {

		public long getDefaultPeriodicitySeconds() {
			return 20 * 60; // 20 minutes
		}

		public String getName() {
			return PollingTaskFamilyType.AMAZON.name();
		}

		public long rescheduleSeconds(long suggestedSeconds) {
			return suggestedSeconds;
		}
	}
	
	private static PollingTaskFamily family = new AmazonTaskFamily();
	
	private static class AmazonTask extends PollingTask {
		private String amazonUserId;
		
		public AmazonTask(String amazonUserId) {
			this.amazonUserId = amazonUserId;
		}

		@Override
		protected PollResult execute() throws Exception {
			boolean changed = false;
			AmazonUpdater proxy = EJBUtil.defaultLookup(AmazonUpdater.class);
			Configuration config = EJBUtil.defaultLookup(Configuration.class);
			AmazonReviewsCache reviewsCache = CacheFactoryBean.defaultLookup(AmazonReviewsCache.class);
			
			// refetching all reviews each time through cache would require paging through Amazon
			// results and making multiple requests for that, we are better off making one request here,
			// and getting the results only if the review count changed or the results in cache expired
			AmazonWebServices ws = new AmazonWebServices(REQUEST_TIMEOUT, config);
			
			int newReviewCount = ws.getReviewsCount(amazonUserId);
			
			boolean needCacheUpdate = false;       	
			int cachedReviewCount = -1;
			try {
				AmazonReviewsView cachedReviews = reviewsCache.checkCache(amazonUserId);
				cachedReviewCount = cachedReviews.getTotal();
			} catch (NotCachedException e) {
				needCacheUpdate = true;
			}
			
			if (needCacheUpdate || (newReviewCount != cachedReviewCount)) {
				AmazonReviewsView reviewsView = reviewsCache.getSync(amazonUserId, true);
				
				// changed might be false here if we were saving updated status because
				// the previous set of results has expired, but nothing really changed
				changed = proxy.saveUpdatedStatus(amazonUserId, reviewsView);
			}	
			
			return new PollResult(changed, false);
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}

		@Override
		public String getIdentifier() {
			return amazonUserId;
		}
	}

	@Override
	protected PollingTask createPollingTask(String handle) {
		return new AmazonTask(handle);
	}		
	
}
	
	
	