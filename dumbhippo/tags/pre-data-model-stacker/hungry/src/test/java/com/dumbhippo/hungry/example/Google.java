package com.dumbhippo.hungry.example;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.PageTestCase;

public class Google extends PageTestCase {
	
	public Google() {
		
	}
	
	Google(WebTester t) {
		super(t);
	}
	
	// only needed if the test case uses a special url
	@Override
	public String getBaseUrl() {
		return "http://www.google.com";
	}
	
	@Override
	public void testPage() {
		t.beginAt("/");
		validatePage();
		doSearch("httpunit");
		t.clickLinkWithText("HttpUnit");
		HttpUnitHomePage thuhp = new HttpUnitHomePage(t);
		thuhp.validatePage();
	}

	// another test could call this
	public void doSearch(String query) {
		t.setFormElement("q", query);
		t.submit("btnG");		
	}
	
	// any test that goes to google.com can use this to check it
	public void validatePage() {
		t.assertFormElementPresent("btnG");
		t.assertFormElementPresent("q");
		t.assertTitleEquals("Google");
	}
}
