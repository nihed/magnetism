package com.dumbhippo.hungry.performance;

import com.dumbhippo.hungry.util.SignedInPageTestCase;
import com.dumbhippo.hungry.util.SkipTest;

/**
 * "Test page" that is used to retrieve a session cookie for a logged
 * in user; we do this before running a set of tests for a user to
 * avoid counting session-creation and authentication overhead. 
 * 
 * @author otaylor
 */
@SkipTest
public class GetSession extends SignedInPageTestCase {

	public GetSession(String userId) {
		super(null, userId);
	}
	
	public String getSessionCookie() {
		t.beginAt("/"); // should be "home" not "main" for a logged in user
		return t.getTestContext().getWebClient().getCookieValue("JSESSIONID");
	}
		
	@Override
	public void testPage() {
		// Not used
	}

	public void validatePage() {
		// Not used
	}
}
