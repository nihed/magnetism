package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.PageTestCase;
import com.meterware.httpunit.HttpUnitOptions;

public class Signin extends PageTestCase {

	public Signin() {
		
	}
	
	public Signin(WebTester t) {
		super(t);
	}
	
	public void setUp() {
		super.setUp();
		HttpUnitOptions.setScriptingEnabled(false);
	}
	
	@Override
	public void testPage() {
		t.beginAt("/signin");
		validatePage();
	}
	
	public void validatePage() {
		t.assertElementPresent("dhSendLinkButton");
		t.assertElementPresent("dhSendLinkAddress");
	}
	
}
