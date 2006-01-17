package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;

public class BookmarkPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(BookmarkPage.class);

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
