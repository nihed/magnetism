package com.dumbhippo.services;

import java.net.URL;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;

public class AmazonItemLookup extends AbstractXmlRequest<AmazonItemLookupSaxHandler> {
	
	static private final Logger logger = GlobalSetup.getLogger(AmazonItemLookup.class);
	
	public AmazonItemLookup(int timeoutMilliseconds) {
		super(timeoutMilliseconds);
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
	
	public AmazonItemData getItem(String amazonAccessKeyId, String itemId) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&Operation=ItemLookup"
				+ "&ResponseGroup=Images,OfferSummary&AWSAccessKeyId=");
		sb.append(amazonAccessKeyId);
		sb.append("&ItemId=");
		sb.append(StringUtils.urlEncode(itemId));		

		String wsUrl = sb.toString();
		logger.debug("Loading amazon web services url {}", wsUrl);
		
		AmazonItemLookupSaxHandler handler = parseUrl(new AmazonItemLookupSaxHandler(), wsUrl);
		
		return handler;
	}
	
	public AmazonItemData getItemForUrl(String amazonAccessKeyId, URL url) {
		String itemId = parseItemIdFromUrl(url);
		if (itemId == null) {
			// at .info so we can find new kinds of url to handle
			logger.info("amazon url we might want to handle: {}", url);
			return null;
		}
		
		AmazonItemData itemData = getItem(amazonAccessKeyId, itemId); 
		if (itemData == null) {
			logger.debug("failed to load amazon data, we parsed item ID '{}' from url {}", itemId, url);
		}
		return itemData;
	}
}
