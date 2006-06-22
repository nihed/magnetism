package com.dumbhippo.web.pages;

import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
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
	
	public String getSigninAimLink() {
		return "aim:GoIM?screenname=" + config.getPropertyFatalIfUnset(HippoProperty.AIMBOT_NAME) 
		+ "&message=Hey+Bot!+Send+me+a+login+link!";
	}
}
