package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.web.WebEJBUtil;

public class FeaturesPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FeaturesPage.class);

	private FacebookSystem facebookSystem;
	
	public FeaturesPage() {
		facebookSystem =  WebEJBUtil.defaultLookup(FacebookSystem.class);
	}
	
    public String getFacebookApiKey() {
        return facebookSystem.getApiKey();
    }
}