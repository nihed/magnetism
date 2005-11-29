package com.dumbhippo.server.rewriters;

import java.net.URL;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostRewriter;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.services.AmazonItemData;
import com.dumbhippo.services.AmazonWebServices;

public class AmazonRewriter extends AbstractRewriter {
	static private final Log logger = GlobalSetup.getLog(AmazonRewriter.class);
	
	static private final String[] domains = { "amazon.com" };
	
	static public String[] getDomains() {
		return domains;
	}
		
	static public PostRewriter newInstance(Configuration config) {
		return new AmazonRewriter(config);
	}
	
	private String amazonAccessKeyId;
	private AmazonItemData itemData;
	
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
	
	@Override
	public void bind(Post post, URL url) {
		super.bind(post, url);
		if (amazonAccessKeyId != null) {
			setAsyncTask(new Runnable() {
	
				public void run() {
					try {
						AmazonWebServices webServices = new AmazonWebServices(amazonAccessKeyId);

						itemData = webServices.getItemForUrl(boundUrl);
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
		if (itemData == null)
			return super.getTextAsHtml();
		
		XmlBuilder xml = new XmlBuilder();
		xml.append("<div class=\"dh-amazon-item\">");
		xml.append("  <img class=\"dh-amazon-small-image\" style=\"float: left;\" src=\"");
		xml.appendEscaped(itemData.getSmallImageUrl());
		xml.append("\" width=\"" + itemData.getSmallImageWidth());
		xml.append("\" height=\"" + itemData.getSmallImageHeight());
		xml.append("\"/> ");
		addPrice(xml, "New", itemData.getNewPrice());
		addPrice(xml, "Used", itemData.getUsedPrice());
		addPrice(xml, "Refurbished", itemData.getRefurbishedPrice());
		addPrice(xml, "Collectible", itemData.getCollectiblePrice());
		xml.append("<br/></div>");
		xml.append("<p class=\"dh-amazon-description\">");
		xml.appendTextAsHtml(boundPost.getText());
		xml.append("</p>");
		return xml.toString();
	}

	@Override
	public String getTitle() {
		return super.getTitle();
	}
}
