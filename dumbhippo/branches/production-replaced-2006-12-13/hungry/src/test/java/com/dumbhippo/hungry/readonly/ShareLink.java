package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.SignedInPageTestCase;

public class ShareLink extends SignedInPageTestCase {

	public ShareLink(WebTester t, String userId) {
		super(t, userId);
	}
		
	public ShareLink() {
		super();
	}
	
	@Override
	public void testPage() {
		// not much we can test since it's all javascript,
		// but we check that the jsp doesn't traceback
		t.beginAt("/sharelink");
		validatePage();
	}

	public void validatePage() {
		t.assertElementPresent("dhShareRecipientsContainer");
		t.assertElementPresent("dhRecipientList");	
		t.assertElementPresent("dhCreateGroupLink");
	}

}
