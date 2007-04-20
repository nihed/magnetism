package com.dumbhippo.services;

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
		// TODO: this will need to page through all the available review pages before returning 
		// all reviews
		// make sure we return most recent reviews first in the list when we page the reviews
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService"
				+ "&Operation=CustomerContentLookup&ResponseGroup=CustomerReviews&CustomerId=");
		sb.append(amazonUserId);
		sb.append("&AWSAccessKeyId=");
		sb.append(this.accessKeyId);
		if (this.associateTagId != null) {
			sb.append("&AssociateTag=");
			sb.append(this.associateTagId);
		}
		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
		
		AmazonSaxHandler handler = parseUrl(new AmazonSaxHandler(true), wsUrl);
		
		return handler.getReviews();
	}
	
	public int getReviewsCount(String amazonUserId) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService"
				+ "&Operation=CustomerContentLookup&ResponseGroup=CustomerReviews&CustomerId=");
		sb.append(amazonUserId);
		sb.append("&AWSAccessKeyId=");
		sb.append(this.accessKeyId);
		if (this.associateTagId != null) {
			sb.append("&AssociateTag=");
			sb.append(this.associateTagId);
		}
		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
		
		AmazonSaxHandler handler = parseUrl(new AmazonSaxHandler(true), wsUrl);
		
		return handler.getReviews().getTotal();
	}
}
