package com.dumbhippo.server.updaters;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.postinfo.EbayPostInfo;
import com.dumbhippo.postinfo.PostInfoType;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.services.EbayItemData;
import com.dumbhippo.services.EbayScreenScraper;

public class EbayUpdater extends AbstractUpdater<EbayPostInfo> {
	static private final Logger logger = GlobalSetup.getLogger(EbayUpdater.class);
	
	static private final String[] domains = { "ebay.com" };

	static public String[] getDomains() {
		return domains;
	}
		
	static public PostUpdater newInstance(Configuration config) {
		return new EbayUpdater(config);
	}

	
	
	public EbayUpdater(Configuration config) {
		super(EbayPostInfo.class);
	}

	@Override
	protected int getMaxAgeMilliseconds() {
		// keep ebay info for 1 hour
		return 1000 * 60 * 60;
	}
	
	@Override
	protected PostInfoType getType() {
		return PostInfoType.EBAY;
	}
	
	@Override
	protected void update(EbayPostInfo postInfo, URL url) {
		String itemId = extractItemId(url);
		if (itemId == null) {
			// log at info level so on production server we can see any urls we 
			// need to handle. Some won't be item urls though, so don't warn.
			logger.info("consider handling this ebay url: {}", url);
			return;
		}
		EbayScreenScraper scraper = new EbayScreenScraper(getUpdateTimeoutMilliseconds());
		EbayItemData itemData = scraper.getItem(itemId);
		
		// if we fail to load info, just stick with whatever old information we had
		if (itemData != null) {
			postInfo.merge(itemData);
		}
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
}
