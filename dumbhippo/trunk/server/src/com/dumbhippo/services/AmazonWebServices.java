package com.dumbhippo.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.util.ConfigurationUtil;

public class AmazonWebServices extends AbstractXmlRequest<AmazonSaxHandler> {

    // typical Amazon user id is 14 characters		
	public static final int MAX_AMAZON_USER_ID_LENGTH = 20;

    // typical Amazon product id is 10 characters		
	public static final int MAX_AMAZON_ITEM_ID_LENGTH = 20;

    // typical Amazon list id is 13 characters		
	public static final int MAX_AMAZON_LIST_ID_LENGTH = 20;
	
	// typical Amazon list item id is 14 characters
	public static final int MAX_AMAZON_LIST_ITEM_ID_LENGTH = 20;
	
	public static final int MAX_AMAZON_REVIEW_PAGES_RETURNED = 10;
	
	public static final int AMAZON_REVIEWS_PER_PAGE = 10;

	public static final int MAX_AMAZON_LIST_PAGES_RETURNED = 30;
	
	static private final Logger logger = GlobalSetup.getLogger(AmazonWebServices.class);
	
	private String accessKeyId;
	private String associateTagId;
	
	public AmazonWebServices(int timeoutMilliseconds, Configuration config) {
		super(timeoutMilliseconds);
		this.accessKeyId = ConfigurationUtil.loadProperty(config, HippoProperty.AMAZON_ACCESS_KEY_ID);
		this.associateTagId = ConfigurationUtil.loadProperty(config, HippoProperty.AMAZON_ASSOCIATE_TAG_ID);
		
		if (this.accessKeyId == null)
			logger.warn("Amazon access key id is not set, can't make Amazon web services calls.");		
		if (this.associateTagId == null)
			logger.warn("Amazon associate tag id is not set, will be making Amazon web services calls without it.");	
	}
	
	public AmazonReviews lookupReviews(String amazonUserId) {
		// We need to page through all the available review pages before returning all reviews.
		// We need to make sure we return most recent reviews first in the list when we page the reviews.
		
		// Amazon returns the oldest review first on the first page, so we need to insert the reviews to
		// the front of the list we are creating as we get them. There currently doesn't seem to be a way
		// to request for the reviews to be sorted by date, getting the newest ones first. This means
		// that we will not be able to pull in new reviews for users who have written more than 100 reviews
		// on Amazon.
		
		int page = 1;
		int totalPages = 1;
		AmazonReviews amazonReviews = null; 
		// Keeping a reviewMap helps us filter out duplicate reviews. Amazon sometimes has duplicate reviews by 
		// the same person for the same item (usually they are identical, so possibly they used to have some 
		// problems with people submitting the same review twice), in which case we want to keep the more recent review.
		// If someone had deleted a review while we were getting the reviews, we will be missing some other review,
		// but we should recover it on the next iteration, unless there is a net change of 0 in the review count.
		// For this scenario to affect us, the count had to change before we request the first page.	
		// LinkedHashMap did not allow inserting pairs to the front, which is what we needed. It also returned
		// a Collection for values() which could not be reversed. So we need to keep both a Map and a List here.
		Map<String, AmazonReviewView> reviewMap = new HashMap<String, AmazonReviewView>();
        List<AmazonReviewView> reviewList = new ArrayList<AmazonReviewView>();
		
		while (page <= totalPages) {
		    String wsUrl = createReviewsRequest(amazonUserId, page);		
		    AmazonSaxHandler handler = parseUrl(new AmazonSaxHandler(false), wsUrl);
		    if (amazonReviews == null) {
		    	if (page != 1) {
		    		logger.warn("amazonReviews were null when getting reviews for page {}", page);	
		    	}
		    	if (handler.getReviews() != null) {
		    		amazonReviews = new AmazonReviews(handler.getReviews().getTotal(), handler.getReviews().getTotalReviewPages());
		    	    for (AmazonReviewView review : handler.getReviews().getReviews()) {
		    	    	AmazonReviewView reviewInMap = reviewMap.get(review.getItemId());
		    	    	if (reviewInMap == null) {
		    	    		// insert it in the front
		    	    		reviewList.add(0, review);
		    	    		reviewMap.put(review.getItemId(), review);
		    	    	} else {
		    	    		// remove a duplicate review that must have been older
		    	    		reviewList.remove(reviewInMap);
		    	    		reviewList.add(0, review);		    	    		
		    	    	}
		    	    }
		    	    // the web services tell us how many pages total there are, but only return reviews
		    	    // from the first MAX_AMAZON_REVIEW_PAGES_RETURNED pages
		    	    totalPages = Math.min(amazonReviews.getTotalReviewPages(), MAX_AMAZON_REVIEW_PAGES_RETURNED);
		    	} else {
		    		// if a customer doesn't have any reviews, the response will not even have a CustomerReviews
		    		// tag, but if there was no error, we should just assume the customer didn't write any reviews
		    		amazonReviews = new AmazonReviews(0, 0);
		    	}
		    } else {
		        if (handler.getReviews() == null) {
		            logger.warn("Reviews are null when we were expecting page {} of Amazon reviews", page);
		        } else {
		    	    for (AmazonReviewView review : handler.getReviews().getReviews()) {
		    	    	AmazonReviewView reviewInMap = reviewMap.get(review.getItemId());
		    	    	if (reviewInMap == null) {
		    	    		// insert it in the front
		    	    		reviewList.add(0, review);
		    	    		reviewMap.put(review.getItemId(), review);
		    	    	} else {
		    	    		// remove a duplicate review that must have been older
		    	    		reviewList.remove(reviewInMap);
		    	    		reviewList.add(0, review);		    	    		
		    	    	}
		    	    }
		        }		    	
		    }
		    page++;
		}
		    	    
		amazonReviews.addReviews(reviewList, false);
		return amazonReviews;
	}
	
	public int getReviewsCount(String amazonUserId) {
		String wsUrl = createReviewsRequest(amazonUserId, 1);		
		AmazonSaxHandler handler = parseUrl(new AmazonSaxHandler(true), wsUrl);
		
		if (handler.getReviews() != null)
		    return handler.getReviews().getTotal();
		
		return 0;
	}
	
	private String createReviewsRequest(String amazonUserId, int page) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService"
				+ "&Operation=CustomerContentLookup&ResponseGroup=CustomerReviews&ReviewPage=");
		sb.append(page);
		sb.append("&CustomerId=");
		sb.append(amazonUserId);
		sb.append("&AWSAccessKeyId=");
		sb.append(this.accessKeyId);
		if (this.associateTagId != null) {
			sb.append("&AssociateTag=");
			sb.append(this.associateTagId);
		}
		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
		return wsUrl;
	}
	
	public AmazonLists lookupLists(String amazonUserId) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService"
				+ "&Operation=CustomerContentLookup&ResponseGroup=CustomerLists&CustomerId=");
		sb.append(amazonUserId);
		sb.append("&AWSAccessKeyId=");
		sb.append(this.accessKeyId);
		if (this.associateTagId != null) {
			sb.append("&AssociateTag=");
			sb.append(this.associateTagId);
		}
		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
	    AmazonSaxHandler handler = parseUrl(new AmazonSaxHandler(false), wsUrl);
	    return handler.getLists();
	}
	
	public AmazonList lookupListItems(String amazonUserId, String listId) {
		int page = 1;
		int totalPages = 1;
		AmazonList amazonList = null; 
		// we have this in case the results shift over while we are getting multiple pages
		// (see lookupReviews for a longer explanation)
		Set<String> itemIds = new HashSet<String>();
		
		while (page <= totalPages) {
		    String wsUrl = createListRequest(listId, true, page);		
		    AmazonSaxHandler handler = parseUrl(new AmazonSaxHandler(false), wsUrl);
		    if (amazonList == null) {
		    	if (page != 1) {
		    		logger.warn("amazonList was null when getting items for page {}", page);	
		    	}
		    	if (handler.getList() != null) {
		    		amazonList = new AmazonList(amazonUserId, listId, handler.getList().getListName(), handler.getList().getTotalItems(),
		    				                    handler.getList().getTotalPages(), handler.getList().getDateCreated(),
		    				                    new ArrayList<AmazonListItem>());
		    	    for (AmazonListItemView listItem : handler.getList().getListItems()) {
		    	    	// sometimes list items are returned without item information,
		    	    	// we can skip those
		    	    	if ((listItem.getItemId() != null) && !itemIds.contains(listItem.getItemId())) {
		    	    		amazonList.addListItem(listItem);
		    	    		itemIds.add(listItem.getItemId());
		    	    	}
		    	    }
		    	    // the web services tell us how many pages total there are, but only return items
		    	    // from the first MAX_AMAZON_LIST_PAGES_RETURNED pages
		    	    totalPages = Math.min(amazonList.getTotalPages(), MAX_AMAZON_LIST_PAGES_RETURNED);
		    	} else {
		    		// There must be no list with this id, nothing to do, we should return null
		    		// we will not overwrite the old data till it expires, and then we will stop 
		    		// looking up that list. We will also stop looking up that list if it's no
		    		// longer in the list of user's lists.
		    	}
		    } else {
		        if (handler.getList() == null) {
		            logger.warn("Amazon list is null when we were expecting page {} of Amazon list items", page);
		        } else {
		    	    for (AmazonListItemView listItem : handler.getList().getListItems()) {
		    	    	if ((listItem.getItemId() != null) && !itemIds.contains(listItem.getItemId())) {
		    	    		amazonList.addListItem(listItem);
		    	    		itemIds.add(listItem.getItemId());
		    	    	}
		    	    }
		        }		    	
		    }
		    page++;
		}
		    	    
		return amazonList;	
	}
	
	// This methos will return null of the list is no longer accessible through
	// ECS (perhaps it was made private).
	public AmazonList getListDetails(String listId) {
	    String wsUrl = createListRequest(listId, false, -1);	
	    AmazonSaxHandler handler = parseUrl(new AmazonSaxHandler(false), wsUrl);
	    return handler.getList();
	}
	
	private String createListRequest(String listId, boolean includeItems, int page) {
		StringBuilder sb = new StringBuilder();
		// sorting by last updated will ensure we always store updated information about
		// list items (like quantity desired, quantity received, and comment)
		// ListFull response group gives us the list of items, we could also include
		// Images, ItemAttributes, and EditorialReview response groups to get information
		// about each item, but we need to do item lookups more frequently than we get list
		// items anyway, so we do that in a separate cache update activity
		// ListFull response group has all elements from ListInfo and ListItems groups
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService"
				+ "&Operation=ListLookup&ListType=WishList&Sort=LastUpdated");
		if (includeItems) {
	        sb.append("&ResponseGroup=ListFull&ProductPage=");
		    sb.append(page);
		}
		sb.append("&ListId=");
		sb.append(listId);
		sb.append("&AWSAccessKeyId=");
		sb.append(this.accessKeyId);
		if (this.associateTagId != null) {
			sb.append("&AssociateTag=");
			sb.append(this.associateTagId);
		}
		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
		return wsUrl;
	}


	// associateTagId can be null for all of the following functions, but other
	// parameters are expected to be valid
	    
	public static String getItemLink(String itemId, String associateTagId) {
	    return "http://www.amazon.com/gp/product/" + itemId + createTagString(associateTagId, true);
	}
	    
	public static String getListLink(String listId, String associateTagId) {
	    return "http://www.amazon.com/gp/registry/" + listId + createTagString(associateTagId, true);
	}
	   
	// if an item is added to the cart after following this link, it is added to be purchased for someone's 
	// wish list
	public static String getListItemLink(String itemId, String listItemId, String listId, String associateTagId) {
	    return "http://www.amazon.com/gp/product/" + itemId +
	           "/?coliid=" + listItemId + "&colid=" + listId + createTagString(associateTagId, false);
	}
    
	public static String getMemberReviewsLink(String amazonUserId, String associateTagId) {
	    return "http://www.amazon.com/gp/cdp/member-reviews/" + amazonUserId + createTagString(associateTagId, true);     
	}
	    
	private static String createTagString(String associateTagId, boolean firstParam) {
        String tagString = "";
	    if (associateTagId != null && associateTagId.trim().length() > 0) {
	        if (firstParam)
	            tagString = "?tag=" + associateTagId.trim();
	        else
	            tagString = "&tag=" + associateTagId.trim();
	    }
	          
        return tagString;
	}
}
