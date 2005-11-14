package com.dumbhippo.hungry.destructive;

import com.dumbhippo.hungry.readonly.Welcome;
import com.dumbhippo.hungry.util.PageTestCase;

public class VerifyInvitation extends PageTestCase {

	private String authKey;
	
	// don't make this public or junit will call it with the test name
	VerifyInvitation(String authKey) {
		this.authKey = authKey;
	}
	
	public VerifyInvitation() {
		
	}
	
	@Override
	public void testPage() {
		if (authKey == null) {
			// This does nothing since we can only implement this 
			// test in the context of AcceptAllInvitations
		} else {
			t.beginAt("/verify?authKey=" + authKey);
			validatePage();
		}
	}
	
	public void validatePage() {
		// should have been redirected to the welcome page
		Welcome w = new Welcome(t);
		w.validatePage();
		
		assertSignedIn();
	}
}
