package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

public class BookmarkPage {
	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(BookmarkPage.class);

	@Browser
	private BrowserBean browser;
	
	private Configuration config;
	
	public BookmarkPage() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}
	
	public BrowserBean getBrowser() {
		return browser;
	}
	
	public String getBaseUrl() {
		return config.getPropertyFatalIfUnset(HippoProperty.BASEURL);
	}
}
