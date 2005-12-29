package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.PageTestCase;

public class ViewPerson extends PageTestCase {

	private String userId;
	
	public ViewPerson() {
		CheatSheet cs = CheatSheet.getReadOnly();
		userId = cs.getOneSampleUserId();
	}
	
	ViewPerson(WebTester t) {
		super(t);
		this.userId = null;
	}
	
	ViewPerson(WebTester t, String userId) {
		super(t);
		this.userId = userId;
	}
	
	@Override
	public void testPage() {
		t.beginAt("/person?who=" + userId);
		validatePage();
	}

	public void validatePage() {
		t.assertLinkPresentWithText("Home");
		t.assertLinkPresentWithText("Your Account");
	}
}
