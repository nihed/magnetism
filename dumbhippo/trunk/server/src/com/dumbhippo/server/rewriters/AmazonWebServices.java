package com.dumbhippo.server.rewriters;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.xml.sax.SAXException;

import com.dumbhippo.GlobalSetup;

class AmazonWebServices {

	static private final Log logger = GlobalSetup.getLog(AmazonWebServices.class);
	
	private SAXParserFactory saxFactory;
	private String amazonAccessKeyId;
	
	AmazonWebServices(String amazonAccessKeyId) {
		this.amazonAccessKeyId = amazonAccessKeyId;
	}
	
	String parseItemIdFromUrl(URL url) {
		// some possible formats are:
		// http://www.amazon.com/gp/product/B000A0GP4K/...
		// http://www.amazon.com/gp/product/product-description/B000A0GP4K/...
		// http://www.amazon.com/gp/product/customer-reviews/B000A0GP4K/...
		// http://www.amazon.com/gp/product/cast-crew/B000A0GP4K/...
		// http://www.amazon.com/gp/product/fun-facts/B000A0GP4K/...
		// http://www.amazon.com/exec/obidos/tg/detail/-/B000BJS4OY/...
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
					s.equals("-") ||
					s.equals("")) {
				continue;
			} else {
				return s;
			}
		}
		return null;
	}
	
	AmazonItemData getItem(String itemId) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&Operation=ItemLookup"
				+ "&ResponseGroup=Images,OfferSummary&AWSAccessKeyId=");
		sb.append(amazonAccessKeyId);
		sb.append("&ItemId=");
		sb.append(itemId);

		String wsUrl = sb.toString();
		logger.debug("Starting AmazonRewriter async task " + wsUrl);
		
		AmazonSaxHandler handler = parseUrl(wsUrl);
		
		return handler;
	}
	
	AmazonItemData getItemForUrl(URL url) {
		String itemId = parseItemIdFromUrl(url);
		if (itemId == null) {
			logger.debug("could not extract item ID from amazon url " + url);
			return null;
		}
		
		AmazonItemData itemData = getItem(itemId); 
		if (itemData != null)
			logger.debug("successfully loaded amazon data");
		else {
			logger.debug("failed to load amazon data, we parsed item ID '" + itemId + "' from url " + url);
		}
		return itemData;
	}

	private SAXParser newSAXParser() {
		if (saxFactory == null)
			saxFactory = SAXParserFactory.newInstance();
		try {
			return saxFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		} catch (SAXException e) {
			logger.trace(e);
			throw new RuntimeException(e);
		}
	}

	private AmazonSaxHandler parseUrl(String url) {
		AmazonSaxHandler handler = new AmazonSaxHandler();
		SAXParser parser = newSAXParser();
		try {
			URL u = new URL(url);
			URLConnection connection = u.openConnection();
		
			// amazon when working usually replies in under 100 milliseconds,
			// so this is a very generous timeout. If the timeout expires 
			// then we give up and just don't display the post in a 
			// special amazon-specific way
			connection.setReadTimeout(1000 * 6);
			connection.setAllowUserInteraction(false);
			
			parser.parse(connection.getInputStream(), handler);
		} catch (SAXException e) {
			logger.warn("parse error on amazon reply", e);
			return null;
		} catch (IOException e) {
			logger.warn("IO error talking to amazon", e);
			return null;
		}
		
		return handler;
	}
}
