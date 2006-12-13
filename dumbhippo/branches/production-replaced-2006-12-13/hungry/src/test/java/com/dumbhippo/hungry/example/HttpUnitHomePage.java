package com.dumbhippo.hungry.example;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.PageTestCase;

public class HttpUnitHomePage extends PageTestCase {
	
	public HttpUnitHomePage() {
		
	}
	
	HttpUnitHomePage(WebTester t) {
		super(t);
	}

	// only needed if the test case uses a special url
	@Override
	public String getBaseUrl() {
		return "http://httpunit.sourceforge.net/";
	}
	
	@Override
	public void testPage() {
		t.beginAt("/");
		validatePage();
	}
	
	public void validatePage() {
		t.assertTitleEquals("HttpUnit Home");
		t.assertLinkPresentWithText("User's Manual");
	}
}
