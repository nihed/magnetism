package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
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
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.services.AmazonItem;
import com.dumbhippo.services.AmazonItemLookupWebServices;
import com.dumbhippo.services.AmazonList;
import com.dumbhippo.services.AmazonListItemView;
import com.dumbhippo.services.AmazonListView;
import com.dumbhippo.services.AmazonLists;
import com.dumbhippo.services.AmazonListsView;
import com.dumbhippo.services.AmazonReviewView;
import com.dumbhippo.services.AmazonReviewsView;
import com.dumbhippo.services.AmazonWebServices;
import com.dumbhippo.services.caches.AmazonItemCache;
import com.dumbhippo.services.caches.AmazonListItemsCache;
import com.dumbhippo.services.caches.AmazonListsCache;
import com.dumbhippo.services.caches.AmazonReviewsCache;
import com.dumbhippo.services.caches.CacheFactory;
import com.dumbhippo.services.caches.CacheFactoryBean;
import com.dumbhippo.services.caches.NotCachedException;
import com.dumbhippo.services.caches.WebServiceCache;
import com.dumbhippo.tx.TxUtils;

@Stateless
public class AmazonUpdaterBean extends CachedExternalUpdaterBean<AmazonUpdateStatus> implements AmazonUpdater {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(AmazonUpdaterBean.class);		
	
	// how long to wait on the Amazon API call
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	// do away without AmazonWishListItemsCache for now, just use AmazonActivityStatus table to
	// keep what's on people's wish lists

	@EJB
	private CacheFactory cacheFactory;	
	
	@EJB
	private Configuration config;
	
	@WebServiceCache
	private AmazonItemCache itemCache;
	
	@WebServiceCache
	private AmazonListsCache listsCache;

	@WebServiceCache
	private AmazonListItemsCache listItemsCache;
	
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
	
	public Collection<AmazonActivityStatus> getActivityStatusesForAmazonAccount(String amazonUserId) {
		Query q = em.createQuery("SELECT activityStatus FROM AmazonActivityStatus activityStatus WHERE " + 
				" activityStatus.amazonUserId = :amazonUserId");
		q.setParameter("amazonUserId", amazonUserId);
		return TypeUtils.castList(AmazonActivityStatus.class, q.getResultList());
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

	private String computeListHash(AmazonListView listView) {
		List<? extends AmazonListItemView> listItems = listView.getListItems();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; ++i) {
			if (i >= listItems.size())
				break;
			sb.append(listItems.get(i).getItemId());
			sb.append("-"); // just leave it trailing on the last one, doesn't matter
		}
		return sb.toString();
	}
	
	private List<AmazonActivityStatus> updateReviews(String amazonUserId, AmazonReviewsView reviewsView, boolean initializingAmazonAccount) {
		// Go over all AmazonActivityStatuses for reviews, and figure out which
		// ones we don't have.
		// We don't do anything if a review is no longer around, we keep the
		// status around, but we will not be able to find review details the cache,
		// so we'll just not display any details in the block about the review.
		// We might also loose the details for very old reviews, because Amazon web 
		// services only return 100 most recent reviews.
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
		
		List<AmazonActivityStatus> newStatusesToNotifyAbout = new ArrayList<AmazonActivityStatus>();
		// remaining reviews require creating a new status object
		for (AmazonReviewView review : reviewsByItemId.values()) {
			AmazonActivityStatus status = new AmazonActivityStatus(amazonUserId, review.getItemId(), "", AmazonActivityType.REVIEWED);
			em.persist(status);
			
			logger.debug("created new Amazon review status {}", status);
	
			// don't create blocks for pre-existing reviews
			if (!initializingAmazonAccount) {
				 newStatusesToNotifyAbout.add(status);
		    }
		}		
		
		return newStatusesToNotifyAbout;		
	}
	
	private List<AmazonActivityStatus> updateListItems(String amazonUserId, AmazonListView listView, boolean initializingAmazonAccount) {
		// Go over all AmazonActivityStatuses for the list, and figure out which
		// ones we don't have.
		// We don't do anything if a list item is no longer around, we keep the
		// status around, but we will not be able to find list item details in the cache,
		// so we'll just not display any details in the block about the list item.
		// We might also loose the details for very old list items, because Amazon web 
		// services only return 300 list items per list.
		Query q = em.createQuery("SELECT activityStatus FROM AmazonActivityStatus activityStatus WHERE " + 
		                         "activityStatus.amazonUserId = :amazonUserId AND " +
		                         "activityStatus.listId = :listId AND " +
				                 "activityStatus.activityType = " + AmazonActivityType.WISH_LISTED.ordinal());
        q.setParameter("amazonUserId", amazonUserId);
        q.setParameter("listId", listView.getListId());
        List<AmazonActivityStatus> statuses = TypeUtils.castList(AmazonActivityStatus.class, q.getResultList());
    
        Map<String,AmazonListItemView> listItemsByItemId = new HashMap<String,AmazonListItemView>();
        for (AmazonListItemView listItem : listView.getListItems()) {
	        listItemsByItemId.put(listItem.getItemId(), listItem);
        }

        // this will leave only list items for which we don't have statuses in listItemsByItemId
		for (AmazonActivityStatus status : statuses) {            
			listItemsByItemId.remove(status.getItemId());
		}
		
		List<AmazonActivityStatus> newStatusesToNotifyAbout = new ArrayList<AmazonActivityStatus>();
		// remaining list items require creating a new status object
		for (AmazonListItemView listItem : listItemsByItemId.values()) {
			AmazonActivityStatus status = new AmazonActivityStatus(amazonUserId, listItem.getItemId(), listView.getListId(), AmazonActivityType.WISH_LISTED);
			em.persist(status);
			
			logger.debug("created new Amazon list item status {}", status);
	
			// don't create blocks for pre-existing list items
			if (!initializingAmazonAccount) {
			   newStatusesToNotifyAbout.add(status);
		    }
		}	
		return newStatusesToNotifyAbout;		
	}
	
	
	public List<AmazonActivityStatus> saveUpdatedStatus(String amazonUserId, AmazonReviewsView reviewsView, AmazonListsView listsView) { 
		String reviewsStatus = "were not changed";
		if (reviewsView != null) {
			reviewsStatus = Integer.toString(reviewsView.getTotal());
		}
		
		logger.debug("Saving new amazon status for {} : reviews {} lists {}", 
				     new String[]{amazonUserId, reviewsStatus, Integer.toString(listsView.getTotal())});
		
		AmazonUpdateStatus updateStatus;
		boolean updateStatusFound = false;
		try {
			updateStatus = getCachedStatus(amazonUserId);
			updateStatusFound = true;
		} catch (NotFoundException e) {
			updateStatus = new AmazonUpdateStatus(amazonUserId);
			em.persist(updateStatus);
		}
		
		boolean reviewsChanged = false;     
		
		if (reviewsView != null) {
			String reviewsHash = computeReviewsHash(reviewsView.getReviews());
			
			if (!updateStatus.getReviewsHash().equals(reviewsHash)) {
				logger.debug("Most recent reviews changed '{}' -> '{}'",
						updateStatus.getReviewsHash(), reviewsHash);
				reviewsChanged = true;
				updateStatus.setReviewsHash(reviewsHash);
			}
		}
		
		AmazonLists listsThatHaveChanged = new AmazonLists();
		// we want to make a copy of the map because we'll be removing
		// the lists we find from it
		Map<String, String> listHashesMap = new HashMap<String, String>();
		listHashesMap.putAll(updateStatus.getListHashes());
		
		for (AmazonListView listView : listsView.getLists()) {
			// if a listView doesn't have any items, it most likely means we didn't set them
			// because we already believe they haven't changed
			// if a list really does not have any items, it's ok not to have a listHash for it
			if (listView.hasSomeItems()) {
			    String newListHash = computeListHash(listView);
			    if (!listHashesMap.containsKey(listView.getListId()) ||
			        !listHashesMap.get(listView.getListId()).equals(newListHash)) {
			    	// this might be null if the key was not previously there
			    	String oldListHash = listHashesMap.get(listView.getListId());
			    	logger.debug("Most recent items changed for list {} : '{}' -> '{}'",
			    			     new String[]{listView.getListId(), oldListHash, newListHash});
			    	listsThatHaveChanged.addList(listView, true);
			    	// this will overwrite oldListHash if it was there
			    	updateStatus.putListHash(listView.getListId(), newListHash);
			    }
			}	
			// remove just won't do anything if the hash for the list was not previously there
			listHashesMap.remove(listView.getListId());
		}
		
		// the lists that remain in the map are no longer present 
		for (String key : listHashesMap.keySet()) {
		    updateStatus.removeListHash(key);	
		    // Calling updateListItems on a list with no items will not do anything
		    // now, because if we ever decide to remove or mark as removed corresponding
		    // AmazonActivityStatuses, this call will be useful. It is better to only
		    // mark things as removed to avoid duplicate notifications in case the 
		    // there was a glitch in getting the data from the web services.
		    AmazonList removedList = new AmazonList();
		    removedList.setListId(key);
		    listsThatHaveChanged.addList(removedList, true);
		}
		
		// Push the updateStatus object to the database. The theory here is that 
		// it keeps Hibernate from reordering saving the new UpdateStatus vs. 
		// the other work we're about to do below, which might help avoid 
		// deadlock situations with the 2nd level cache. 
		// (If two threads both do A,B that's safer than having one do A,B and
		// one B,A.) Since we're done with the updateStatus
		// at this point, it shouldn't hurt efficiency too much to do this.
		em.flush();
		
		List<AmazonActivityStatus> newStatusesToNotifyAbout = new ArrayList<AmazonActivityStatus>();
        if (reviewsChanged) {
			// If we did not have an updateStatus before, it means that we just initialized
			// this account.
			newStatusesToNotifyAbout.addAll(updateReviews(amazonUserId, reviewsView, !updateStatusFound));
        }
        
        for (AmazonListView listView : listsThatHaveChanged.getLists()) {
        	newStatusesToNotifyAbout.addAll(updateListItems(amazonUserId, listView, !updateStatusFound));
        }

		if (!reviewsChanged && (listsThatHaveChanged.getTotal() == 0)) {
			// this means that there was no change
			return null;
		} else {
			// when we are initializing somone's Amazon account, this will be empty,
			// but we will count it as a "changed" result, so that we don't start polling
			// less frequently right away
			return newStatusesToNotifyAbout;
		}
	}
	
	public void saveItemsInCache(List<AmazonItem> items) {
	    for (AmazonItem item : items) {
	        itemCache.saveInCacheInsideExistingTransaction(item.getItemId(), item, new Date(), true);	
	    }
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
			AmazonUpdater proxy = EJBUtil.defaultLookup(AmazonUpdater.class);
			Configuration config = EJBUtil.defaultLookup(Configuration.class);
			AmazonReviewsCache reviewsCache = CacheFactoryBean.defaultLookup(AmazonReviewsCache.class);
			AmazonListsCache listsCache = CacheFactoryBean.defaultLookup(AmazonListsCache.class);
			AmazonListItemsCache listItemsCache = CacheFactoryBean.defaultLookup(AmazonListItemsCache.class);
			
			// refetching all reviews each time through cache would require paging through Amazon
			// results and making multiple requests for that, we are better off making one request here,
			// and getting the results only if the review count changed or the results in cache expired
			AmazonWebServices ws = new AmazonWebServices(REQUEST_TIMEOUT, config);
			
			int newReviewCount = ws.getReviewsCount(amazonUserId);
			
			boolean needReviewsCacheUpdate = false;       	
			int cachedReviewCount = -1;
			try {
				AmazonReviewsView cachedReviews = reviewsCache.checkCache(amazonUserId);			
				cachedReviewCount = cachedReviews.getTotal();
			} catch (NotCachedException e) {
				needReviewsCacheUpdate = true;
			}
			
			AmazonReviewsView reviewsView = null;
			if (needReviewsCacheUpdate || (newReviewCount != cachedReviewCount)) {
				reviewsView = reviewsCache.getSync(amazonUserId, true);
			}	

			// For the wish list items, we first get the wish lists, we can
			// update the wish lists list only once every 24 hours, but because people
			// might wonder if Mugshot picked up their new wish list right away,
			// we should actually check it on every iteration. Because it's not
			// unlikely for someone to remove one wish list and add another at the
			// same time and people generally have few wish lists, we just always 
			// check if wish list ids and names match what we have.
			AmazonListsView listsView = listsCache.getSync(amazonUserId, true);
			
			AmazonListsView refreshedListsView = new AmazonLists();
			
			for (AmazonListView list : listsView.getLists()) {
				// we would not have list details if it is new or has expired in our database
				boolean needListCacheUpdate = !list.hasListDetails();
				
				if (!needListCacheUpdate) {
	                // refetching all wish list items each time through cache would require paging through Amazon
					// results and making multiple requests for that, we are better off making one request here,
					// and getting the results only if the wish list items count changed or the results in cache expired	
					// we also get the results if the list name changed
				    AmazonListView newList = ws.getListDetails(list.getListId());
                    if (newList.getTotalItems() != list.getTotalItems() ||
                        !newList.getListName().equals(list.getListName())) {
                    	needListCacheUpdate = true;
                    }
				}
				
				if (needListCacheUpdate) {
					// AmazonListView also serves as what could have been called AmazonListItemsView
					AmazonListView listWithItems = 
						listItemsCache.getSync(new Pair<String, String>(amazonUserId, list.getListId()), true);
					refreshedListsView.addList(listWithItems, true);
				} else {
					// we will know that this list has not changed because hasSomeItems() will be false 
					// for it; we want to have all lists in the refreshedListsView so that saveUpdatedStatus
					// can figure out if some lists were deleted
					refreshedListsView.addList(list, true);
				}
			}
			
			// the returned list will be null if we were saving something because it 
			// expired, but nothing really changed
			final List<AmazonActivityStatus> statuses = proxy.saveUpdatedStatus(amazonUserId, reviewsView, refreshedListsView);
			
			if (statuses == null) 
				return new PollResult(false, false);
			
			// we first want to get information about all Amazon items we have new statuses about, and
			// then we can create notifications
			
			StringBuilder sb = new StringBuilder();
			int count = 0;
			AmazonItemLookupWebServices ailws = new AmazonItemLookupWebServices(REQUEST_TIMEOUT, config);
			
			for (AmazonActivityStatus status : statuses) {
				sb.append(status.getItemId());
				sb.append(",");
				count++;
				// we don't expect many items to be added on each iteration, but we should
				// limit our calls to Amazon web services to MAX_AMAZON_ITEMS_FOR_LOOKUP at a time
				if (count == AmazonItemLookupWebServices.MAX_AMAZON_ITEMS_FOR_LOOKUP) {
				    sb.setLength(sb.length() - 1);
				    List<AmazonItem> items = ailws.getItems(sb.toString());
				    proxy.saveItemsInCache(items);
				    count = 0;
				    sb.setLength(0);
				}
			}
			// get the remainder of new items
			if (sb.length() > 0) {
			    sb.setLength(sb.length() - 1);
			    List<AmazonItem> items = ailws.getItems(sb.toString());
			    proxy.saveItemsInCache(items);				
			}
			
			TxUtils.runInTransaction(new Runnable() {
				public void run() {
					DataService.getModel().initializeReadWriteSession(SystemViewpoint.getInstance());
					
					Notifier notifier = EJBUtil.defaultLookup(Notifier.class);
					for (AmazonActivityStatus status : statuses) {
					    notifier.onAmazonActivityCreated(status);
					}
				}
			});
			
			return new PollResult(true, false);
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
	
	public List<Pair<String, String>> getAmazonLinks(String amazonUserId, boolean expireCache) {
		List<Pair<String, String>> amazonLinks = new ArrayList<Pair<String, String>>();
		
		Pair<String, String> reviewLink = 
			new Pair<String, String>("My Reviews", AmazonWebServices.getMemberReviewsLink(amazonUserId, null));
		amazonLinks.add(reviewLink);
	
	    AmazonListsView lists = listsCache.getSync(amazonUserId, true);
	    if (lists != null) {	
			AmazonWebServices ws = new AmazonWebServices(REQUEST_TIMEOUT, config);
		    for (AmazonListView list : lists.getLists()) {	
		        String listName = list.getListName();	
		    	// Just getting the list for the first time, doesn't get its name.
		    	// So we should get list details.
		    	if (expireCache || (listName == null) || (listName.trim().length() == 0)) {
			        AmazonListView newList = ws.getListDetails(list.getListId());
                    if (newList != null && (newList.getTotalItems() != list.getTotalItems() 
                    	|| !newList.getListName().equals(listName))) {
                    	listName = newList.getListName();    
    		    	    listItemsCache.expireCache(new Pair<String, String>(amazonUserId, list.getListId()));                	
                    }                       
                }
		    	if ((listName != null) && (listName.trim().length() != 0)) {
			        Pair<String, String> listLink = 
				        new Pair<String, String>(listName, AmazonWebServices.getListLink(list.getListId(), null));
			        amazonLinks.add(listLink);		
		    	}    
		    }
	    }
		
		return amazonLinks;
	}
}
	
	
	