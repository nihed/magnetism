package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.SignedInPageTestCase;

public class ViewPerson extends SignedInPageTestCase {

	private String viewedId;
	
	public ViewPerson() {
		CheatSheet cs = CheatSheet.getReadOnly();
		viewedId = cs.getOneSampleUserId();
	}
	
	ViewPerson(WebTester t, String viewedId) {
		super(t, null);
		this.viewedId = viewedId;
	}
	
	@Override
	public void testPage() {
		t.beginAt("/person?who=" + viewedId);
		validatePage();
	}

	public void validatePage() {
		t.assertLinkPresentWithText("Home");
		t.assertTextPresent("SHARED BY");
	}
}
