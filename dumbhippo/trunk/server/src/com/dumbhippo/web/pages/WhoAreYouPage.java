package com.dumbhippo.web.pages;

import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.web.Browser;
import com.dumbhippo.web.BrowserBean;
import com.dumbhippo.web.WebEJBUtil;

public class WhoAreYouPage {

	private Configuration config;
	
	@Browser
	private BrowserBean browser;
	
	public WhoAreYouPage() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}
	
	public BrowserBean getBrowser() {
		return browser;
	}
	
	public String getAimBotScreenName() {
		try {
			return config.getPropertyNoDefault(HippoProperty.AIMBOT_NAME);
		} catch (PropertyNotFoundException pnfe) {
			return null;
		}
	}
	
	public String getAimPresenceKey() {
		try {
			return config.getPropertyNoDefault(HippoProperty.AIM_PRESENCE_KEY);
		} catch (PropertyNotFoundException pnfe) {
			return null;
		}
	}
	
	public String getFacebookApplicationName() {
        String applicationName = "mugshot";
        if (config.getBaseUrlMugshot().toExternalForm().contains("dogfood"))
        	applicationName = "mugshot-test";
        return applicationName;
	}
}
