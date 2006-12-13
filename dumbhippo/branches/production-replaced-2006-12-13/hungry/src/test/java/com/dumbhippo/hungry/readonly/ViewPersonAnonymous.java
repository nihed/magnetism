package com.dumbhippo.hungry.readonly;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.PageTestCase;

public class ViewPersonAnonymous extends PageTestCase {

	private String viewedId;
	
	public ViewPersonAnonymous() {
		CheatSheet cs = CheatSheet.getReadOnly();
		viewedId = cs.getOneSampleUserId();
	}
	
	ViewPersonAnonymous(WebTester t, String viewedId) {
		super(t);
		this.viewedId = viewedId;
	}
	
	@Override
	public void testPage() {
		t.beginAt("/person?who=" + viewedId);
		validatePage();
	}

	public void validatePage() {
		// FIXME check for nonexistence of private items
	}
}
