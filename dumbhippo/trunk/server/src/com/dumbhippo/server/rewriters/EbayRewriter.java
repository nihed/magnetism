package com.dumbhippo.server.rewriters;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.PostRewriter;
import com.dumbhippo.server.Configuration;

public class EbayRewriter extends AbstractRewriter {

	static private final Log logger = GlobalSetup.getLog(EbayRewriter.class);
	
	static private final String[] domains = { "ebay.com" };
	
	static public String[] getDomains() {
		return domains;
	}
		
	static public PostRewriter newInstance(Configuration config) {
		return new EbayRewriter();
	}
	
	private EbayItemData itemData;
	
	private EbayRewriter() {
		
	} 
	
	private String extractItemId(URL url) {
		// example is:
		// http://cgi.ebay.com/ws/eBayISAPI.dll?ViewItem&item=5059205542&ru=http://search.ebay.com:80/5059205542_W0QQfromZR40QQfviZ1
		String params = url.getQuery();
		if (params != null) {
			Pattern p = Pattern.compile("item=([0-9]+)");
			Matcher m = p.matcher(params);
			if (m.find())
				return m.group(1);
		}
		
		// example is:
		// http://cgi.ebay.com/AUTHENTIC-ARMATURA-RB6H388A-AUTOMATIC-PILOT-MEN-WATCH_W0QQitemZ5059205542QQcategoryZ31387QQrdZ1QQcmdZViewItem
		Pattern p = Pattern.compile("QQitemZ([0-9]+)");
		Matcher m = p.matcher(url.getPath());
		if (m.find())
			return m.group(1);
		
		return null;
	}
	
	public void bind(Post post, URL url) {
		super.bind(post, url);
		final String itemId = extractItemId(url);
		if (itemId == null) {
			logger.debug("no item id in ebay url, not rewriting " + url);
			return;
		}
		setAsyncTask(new Runnable() {
			
			public void run() {
				try {
					logger.debug("launching ebay scraper for itemId " + itemId);
					EbayScreenScraper scraper = new EbayScreenScraper();
					itemData = scraper.getItem(itemId);
				} finally {
					notifyAsyncTask();
				}
			}
			
		});
	}

	public String getTextAsHtml() {
		waitForAsyncTask();
		
		if (itemData == null)
			return super.getTextAsHtml();
		
		String pictureUrl = itemData.getPictureUrl();
		if (pictureUrl == null)
			return super.getTextAsHtml();
		
		XmlBuilder xml = new XmlBuilder();
		xml.append("<div class=\"dh-ebay-item\">");
		xml.append("  <img class=\"dh-ebay-small-image\" style=\"float: left; max-width: 70; max-height: 70;\" src=\"");
		xml.appendEscaped(pictureUrl);
		xml.append("\"/> ");
		xml.append("</div>");
		xml.append("<p class=\"dh-ebay-description\">");
		xml.appendTextAsHtml(boundPost.getText());
		xml.append("</p>");
		return xml.toString();
	}

	public String getTitle() {
		return super.getTitle();
	}
}
