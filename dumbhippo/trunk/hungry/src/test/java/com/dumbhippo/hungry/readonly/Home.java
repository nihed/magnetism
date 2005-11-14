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
		t.beginAt("/home");
		validatePage();
		t.clickLinkWithText("(your public page)");
		ViewPerson vp = new ViewPerson(t, getUserId());
		vp.validatePage();
	}

	public void validatePage() {
		//t.dumpResponse();
		t.assertLinkPresentWithText("(your public page)");
	}
}
