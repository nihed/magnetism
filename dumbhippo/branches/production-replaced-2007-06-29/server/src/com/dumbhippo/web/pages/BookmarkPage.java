package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.web.Browser;
import com.dumbhippo.web.BrowserBean;
import com.dumbhippo.web.WebEJBUtil;

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
