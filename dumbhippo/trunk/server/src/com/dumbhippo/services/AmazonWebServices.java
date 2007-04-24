package com.dumbhippo.services;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

public class AmazonWebServices extends AbstractXmlRequest<AmazonSaxHandler> {

    // typical Amazon user id is 14 characters		
	public static final int MAX_AMAZON_USER_ID_LENGTH = 20;

    // typical Amazon product id is 10 characters		
	public static final int MAX_AMAZON_ITEM_ID_LENGTH = 20;

    // typical Amazon wish list id is 13 characters		
	public static final int MAX_AMAZON_WISH_LIST_ID_LENGTH = 20;
	
	public static final int MAX_AMAZON_REVIEW_PAGES_RETURNED = 10;
	
	static private final Logger logger = GlobalSetup.getLogger(AmazonWebServices.class);
	
	private String accessKeyId;
	private String associateTagId;
	
	static private String loadProperty(Configuration config, HippoProperty property) {
		String propertyValue;
		try {
			propertyValue = config.getPropertyNoDefault(property);
			if (propertyValue.trim().length() == 0)
				propertyValue = null;
		} catch (PropertyNotFoundException e) {
			propertyValue = null;
		}
		return propertyValue;
	}
	
	public AmazonWebServices(int timeoutMilliseconds, Configuration config) {
		super(timeoutMilliseconds);
		this.accessKeyId = loadProperty(config, HippoProperty.AMAZON_ACCESS_KEY_ID);
		this.associateTagId = loadProperty(config, HippoProperty.AMAZON_ASSOCIATE_TAG_ID);
		
		if (this.accessKeyId == null)
			logger.warn("Amazon access key id is not set, can't make Amazon web services calls.");		
		if (this.associateTagId == null)
			logger.warn("Amazon associate tag id is not set, will be making Amazon web services calls without it.");	
	}
	
	public AmazonReviews lookupReviews(String amazonUserId) {
		// We need to page through all the available review pages before returning all reviews.
		// We need to make sure we return most recent reviews first in the list when we page the reviews.
		int page = 1;
		int totalPages = 1;
		AmazonReviews amazonReviews = null; 
		// This helps us filter out duplicate reviews. Amazon sometimes has duplicate reviews by the same person
		// for the same item (usually they are identical, so possibly they used to have some problems with people
		// submitting the same review twice), in which case we want to keep the more recent review. This should also
		// help avoid duplicates if while we were making multiple requests to get all the pages, someone added a 
		// review, and all reviews have shifted over, resulting in us getting the same review on multiple pages.
		// If someone had deleted a review while we were getting the reviews, we will be missing some other review,
		// but we should recover it on the next iteration, unless there is a net change of 0 in the review count.
		// For the above two scenarios to affect us, the count also had to change before we request the first page.
		Set<String> itemIds = new HashSet<String>();
		
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
		    	    	if (!itemIds.contains(review.getItemId())) {
		    	    		amazonReviews.addReview(review, false);
		    	    		itemIds.add(review.getItemId());
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
		    	    	if (!itemIds.contains(review.getItemId())) {
		    	    		amazonReviews.addReview(review, false);
		    	    		itemIds.add(review.getItemId());
		    	    	}
		    	    }
		        }		    	
		    }
		    page++;
		}
		    	    
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
}
