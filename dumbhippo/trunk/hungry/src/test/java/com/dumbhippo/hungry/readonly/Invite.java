package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.SignedInPageTestCase;

public class Invite extends SignedInPageTestCase {

	public Invite(WebTester t, String userId) {
		super(t, userId);
	}
	
	public Invite() {
		super();
	}
	
	@Override
	public void testPage() {
		t.beginAt("/invite");
		validatePage();
	}

	public void validatePage() {
		t.assertFormElementPresent("email");
		t.assertFormElementPresent("fullName");
	}

}
