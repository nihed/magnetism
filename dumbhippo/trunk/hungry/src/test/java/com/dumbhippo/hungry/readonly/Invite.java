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
		t.beginAt("/invite");
		validatePage();
	}

	public void validatePage() {
		CheatSheet cs = CheatSheet.getReadOnly();
		int invites = cs.getNumberOfInvitations(getUserId());
		if (invites > 0) {
			t.assertFormElementPresent("email");
			t.assertFormElementPresent("subject");
			t.assertFormElementPresent("message");
		} else {
			t.assertTextPresent("Out Of Invites");
		}
	}
}
