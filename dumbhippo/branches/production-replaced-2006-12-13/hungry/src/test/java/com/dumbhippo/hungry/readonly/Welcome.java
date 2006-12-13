package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.SignedInPageTestCase;

public class Welcome extends SignedInPageTestCase {

	public Welcome(WebTester t) {
		super(t, null);
	}
	
	public Welcome() {
		super();
	}
	
	@Override
	public void testPage() {
		t.beginAt("/download");
		validatePage();
	}

	public void validatePage() {
		t.assertLinkPresentWithText("Click here to download");
	}

}
