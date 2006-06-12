package com.dumbhippo.web.pages;

import com.dumbhippo.web.Browser;
import com.dumbhippo.web.BrowserBean;

public class WhoAreYouPage {

	@Browser
	private BrowserBean browser;
	
	public WhoAreYouPage() {
		
	}
	
	public BrowserBean getBrowser() {
		return browser;
	}
}
