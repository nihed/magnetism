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
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostRewriter;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;

public class AmazonRewriter extends AbstractRewriter {
	static private final Log logger = GlobalSetup.getLog(AmazonRewriter.class);
	
	static private final String[] domains = { "amazon.com" };
	
	static private SAXParserFactory saxFactory;
	
	static public String[] getDomains() {
		return domains;
	}
		
	static public PostRewriter newInstance(Configuration config) {
		return new AmazonRewriter(config);
	}

	static private SAXParser newSAXParser() {
		synchronized (AmazonRewriter.class) {
			if (saxFactory == null)
				saxFactory = SAXParserFactory.newInstance();
		}
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

	static private AmazonSaxHandler parseUrl(String url) {
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
			
			parser.parse(connection.getInputStream(), handler);
		} catch (SAXException e) {
			logger.warn("parse error on amazon reply", e);
			return null;
		} catch (IOException e) {
			logger.warn("IO error talking to amazon", e);
			return null;
		}
		
		if (handler.isValid()) {
			return handler;
		} else {
			logger.warn("Did not successfully parse all fields from Amazon");
			return null;
		}
	}
	
	private String amazonAccessKeyId;
	private AmazonSaxHandler handler;
	
	private AmazonRewriter(Configuration config) {
		try {
			amazonAccessKeyId = config.getPropertyNoDefault(HippoProperty.AMAZON_ACCESS_KEY_ID);
			if (amazonAccessKeyId.trim().length() == 0)
				amazonAccessKeyId = null;
		} catch (PropertyNotFoundException e) {
			amazonAccessKeyId = null;
		}
		
		if (amazonAccessKeyId == null)
			logger.warn("Amazon web services access key is not set");
	}
	
	private String parseItemId(URL url) {
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
	
	// normally called from another thread, but synchronization
	// isn't needed since all our methods that touch the same 
	// stuff will waitForAsyncTask() before continuing
	private void loadAmazonData() {
		if (amazonAccessKeyId == null)
			return;
		
		//String itemId = "B000A0GP4K"; // for testing
		String itemId = parseItemId(boundUrl);
		
		if (itemId == null) {
			logger.debug("could not extract item ID from amazon url " + boundUrl);
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&Operation=ItemLookup"
				+ "&ResponseGroup=Images,OfferSummary&AWSAccessKeyId=");
		sb.append(amazonAccessKeyId);
		sb.append("&ItemId=");
		sb.append(itemId);

		String wsUrl = sb.toString();
		logger.debug("Starting AmazonRewriter async task " + wsUrl);
		
		handler = parseUrl(wsUrl);
		
		if (handler != null)
			logger.debug("successfully loaded amazon data");
		else {
			logger.debug("failed to load amazon data, we parsed item ID '" + itemId + "' from url " + boundUrl);
		}
	}
	
	@Override
	public void bind(Post post, URL url) {
		super.bind(post, url);
		if (this.amazonAccessKeyId != null) {
			setAsyncTask(new Runnable() {
	
				public void run() {
					try {
						loadAmazonData();
					} finally {
						notifyAsyncTask();
					}
				}
				
			});
		}
	}
	
	private void addPrice(XmlBuilder xml, String type, String value) {
		if (value == null)
			return;
		xml.appendTextNode("b", value, "class", "dh-amazon-price");
		xml.append(" " + type + "<br/>");
	}
	
	@Override
	public String getTextAsHtml() {
		waitForAsyncTask();
		if (handler == null)
			return super.getTextAsHtml();
		
		XmlBuilder xml = new XmlBuilder();
		xml.append("<div class=\"dh-amazon-item\">");
		xml.append("  <img class=\"dh-amazon-small-image\" style=\"float: left;\" src=\"");
		xml.appendEscaped(handler.getSmallImageUrl());
		xml.append("\" width=\"" + handler.getSmallImageWidth());
		xml.append("\" height=\"" + handler.getSmallImageHeight());
		xml.append("\"/> ");
		addPrice(xml, "New", handler.getNewPrice());
		addPrice(xml, "Used", handler.getUsedPrice());
		addPrice(xml, "Refurbished", handler.getRefurbishedPrice());
		addPrice(xml, "Collectible", handler.getCollectiblePrice());
		xml.append("<br/></div>");
		xml.appendTextNode("p", boundPost.getText(), "class", "dh-amazon-description");
		return xml.toString();
	}

	@Override
	public String getTitle() {
		return super.getTitle();
	}
}
