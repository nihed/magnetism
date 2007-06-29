package com.dumbhippo.server.updaters;

import java.net.URL;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.postinfo.AmazonPostInfo;
import com.dumbhippo.postinfo.PostInfoType;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.services.AmazonItemView;
import com.dumbhippo.services.AmazonItemLookupWebServices;

public class AmazonUpdater extends AbstractUpdater<AmazonPostInfo> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AmazonUpdater.class);
	
	static private final String[] domains = { "amazon.com" };

	static public String[] getDomains() {
		return domains;
	}
		
	static public PostUpdater newInstance(Configuration config) {
		return new AmazonUpdater(config);
	}

	private Configuration config;
	
	public AmazonUpdater(Configuration config) {
		super(AmazonPostInfo.class);
		this.config = config;
	}

	@Override
	protected int getMaxAgeMilliseconds() {
		// keep amazon info for 1 hour
		return 1000 * 60 * 60;
	}
	
	@Override
	protected PostInfoType getType() {
		return PostInfoType.AMAZON;
	}
	
	@Override
	protected void update(AmazonPostInfo postInfo, URL url) {
		AmazonItemLookupWebServices itemLookup = new AmazonItemLookupWebServices(getUpdateTimeoutMilliseconds(), config);
		AmazonItemView itemData = itemLookup.getItemForUrl(url);
		
		// if the update fails, just stick with whatever old information we had
		if (itemData != null) {
			postInfo.merge(itemData);
		}
	}
}
