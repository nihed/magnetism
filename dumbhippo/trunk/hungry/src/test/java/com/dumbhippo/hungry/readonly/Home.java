package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.OrderAfter;
import com.dumbhippo.hungry.util.SignedInPageTestCase;

@OrderAfter(Signin.class)
public class Home extends SignedInPageTestCase {

	public Home(WebTester t, String userId) {
		super(t, userId);
	}
		
	public Home() {
		super();
	}
	
	@Override
	public void testPage() {
		t.beginAt("/");  // should be "home" not "main" for a logged in user
		validatePage();
	}

	public void validatePage() {
		//t.dumpResponse();
		t.assertLinkPresentWithText("Edit account");
	}
}
