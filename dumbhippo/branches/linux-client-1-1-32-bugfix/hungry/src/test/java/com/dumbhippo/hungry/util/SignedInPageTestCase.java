package com.dumbhippo.hungry.util;

import net.sourceforge.jwebunit.WebTester;

/**
 * Test case that logs on as a test user in setUp()
 * 
 * @author hp
 */
public abstract class SignedInPageTestCase extends PageTestCase {
	
	private String userId;
	
	// userId can be null
	protected SignedInPageTestCase(WebTester t, String userId) {
		super(t);
		this.userId = userId;
	}
	
	protected SignedInPageTestCase(String userId) {
		this(null, userId);
	}

	protected SignedInPageTestCase() {
		this(null, null);
	}
	
	public String getUserId() {
		return userId;
	}
	
	@Override
	public void setUp() {
		super.setUp();
		
		CheatSheet cs = CheatSheet.getReadOnly();
		
		if (userId == null) {
			userId = cs.getOneSampleUserId();
		}
		
		String authKey = cs.getUserAuthKey(userId);
		if (authKey == null) {
			throw new RuntimeException("userId " + userId + " appears to have no auth keys, can't sign in");
		}
		
		t.getTestContext().addCookie("auth", "name=" + userId + "&password=" + authKey);
	}	
}
