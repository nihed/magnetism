package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.CheatSheet;
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
		t.beginAt("/invitation");
		validatePage();
	}

	public void validatePage() {
		CheatSheet cs = CheatSheet.getReadOnly();
		int invites = cs.getNumberOfInvitations(getUserId());
		if (invites > 0) {
			t.assertTextNotPresent("NO INVITATIONS REMAINING");
			t.assertFormElementPresent("dhAddressEntry");
			t.assertFormElementPresent("dhSubjectEntry");
			t.assertFormElementPresent("dhMessageEntry");
		} else {
			t.assertFormElementNotPresent("dhAddressEntry");				
			t.assertTextPresent("NO INVITATIONS REMAINING");
		}
	}
}
