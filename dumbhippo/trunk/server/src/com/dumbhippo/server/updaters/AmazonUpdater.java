package com.dumbhippo.server.updaters;

import java.net.URL;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.postinfo.AmazonPostInfo;
import com.dumbhippo.postinfo.PostInfoType;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.services.AmazonItemData;
import com.dumbhippo.services.AmazonWebServices;

public class AmazonUpdater extends AbstractUpdater<AmazonPostInfo> {
	static private final Logger logger = GlobalSetup.getLogger(AmazonUpdater.class);
	
	static private final String[] domains = { "amazon.com" };

	static public String[] getDomains() {
		return domains;
	}
		
	static public PostUpdater newInstance(Configuration config) {
		return new AmazonUpdater(config);
	}

	private String amazonAccessKeyId;
	
	public AmazonUpdater(Configuration config) {
		super(AmazonPostInfo.class);
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
	protected int getMaxAgeMilliseconds() {
		// keep amazon info for 1 day
		return 1000 * 60 * 60 * 24;
	}
	
	@Override
	protected PostInfoType getType() {
		return PostInfoType.AMAZON;
	}
	
	@Override
	protected void update(AmazonPostInfo postInfo, URL url) {
		if (amazonAccessKeyId == null)
			return;
		AmazonWebServices webServices = new AmazonWebServices(amazonAccessKeyId, getUpdateTimeoutMilliseconds());
		AmazonItemData itemData = webServices.getItemForUrl(url);
		
		// if the update fails, just stick with whatever old information we had
		if (itemData != null) {
			postInfo.merge(itemData);
		}
	}
}
