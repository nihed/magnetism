package com.dumbhippo.services;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.util.ConfigurationUtil;

public class AmazonItemLookupWebServices extends AbstractXmlRequest<AmazonItemLookupSaxHandler> {
	
	static private final Logger logger = GlobalSetup.getLogger(AmazonItemLookupWebServices.class);
	
	// 10 items is the maximum number of comma separated item ids we can supply to ItemLookup
	public static final int MAX_AMAZON_ITEMS_FOR_LOOKUP = 10;
	
	private String accessKeyId;
	private String associateTagId;
	
	public AmazonItemLookupWebServices(int timeoutMilliseconds, Configuration config) {
		super(timeoutMilliseconds);
		this.accessKeyId = ConfigurationUtil.loadProperty(config, HippoProperty.AMAZON_ACCESS_KEY_ID);
		this.associateTagId = ConfigurationUtil.loadProperty(config, HippoProperty.AMAZON_ASSOCIATE_TAG_ID);
		
		if (this.accessKeyId == null)
			logger.warn("Amazon access key id is not set, can't make Amazon item lookup web services calls.");		
		if (this.associateTagId == null)
			logger.warn("Amazon associate tag id is not set, will be making Amazon item lookup web services calls without it.");	
	}
	
	private String parseItemIdFromUrl(URL url) {
		// some possible formats are:
		// http://www.amazon.com/gp/product/B000A0GP4K/...
		// http://www.amazon.com/gp/product/product-description/B000A0GP4K/...
		// http://www.amazon.com/gp/product/customer-reviews/B000A0GP4K/...
		// http://www.amazon.com/gp/product/cast-crew/B000A0GP4K/...
		// http://www.amazon.com/gp/product/fun-facts/B000A0GP4K/...
		// http://www.amazon.com/exec/obidos/tg/detail/-/B000BJS4OY/...
		// http://www.amazon.com/exec/obidos/ASIN/B00083HIL8
		// there are probably others...
		String path = url.getPath();
		String[] components = path.split("\\/");
		for (String s : components) {
			if (s.equals("gp") ||
					s.equals("product") ||
					s.equals("product-description") ||
					s.equals("customer-reviews") ||
					s.equals("cast-crew") ||
					s.equals("fun-facts") ||
					s.equals("exec") ||
					s.equals("obidos") ||
					s.equals("tg") ||
					s.equals("detail") ||
					s.equals("ASIN") ||
					s.equals("-") ||
					s.equals("")) {
				continue;
			} else {
				return s;
			}
		}
		return null;
	}

	public List<AmazonItem> getItems(List<String> itemIds) {
		return getItems(StringUtils.concatenateUsingSeparator(itemIds, ","));
	}
	
	public List<AmazonItem> getItems(String itemIds) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&Operation=ItemLookup"
				+ "&ResponseGroup=Small,Images,OfferSummary,EditorialReview&AWSAccessKeyId=");
		sb.append(accessKeyId);
		if (associateTagId != null) {
			sb.append("&AssociateTag=");
			sb.append(associateTagId);
		}
		sb.append("&ItemId=");
		sb.append(StringUtils.urlEncode(itemIds));		

		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
		
		AmazonItemLookupSaxHandler handler = parseUrl(new AmazonItemLookupSaxHandler(), wsUrl);
		
		return handler.getItems();		
	}
	
	public AmazonItem getItem(String itemId) {
		List<String> itemIds = new ArrayList<String>();
		itemIds.add(itemId);
		List<AmazonItem> items = getItems(itemIds);
		if (items != null && items.size() > 0) {
			return items.get(0);
		}
		return null;
	}
	
	public AmazonItem getItemForUrl(URL url) {
		String itemId = parseItemIdFromUrl(url);
		if (itemId == null) {
			// at .info so we can find new kinds of url to handle
			logger.info("amazon url we might want to handle: {}", url);
			return null;
		}
		
		AmazonItem itemData = getItem(itemId); 
		if (itemData == null) {
			logger.debug("failed to load amazon data, we parsed item ID '{}' from url {}", itemId, url);
		}
		return itemData;
	}
}
