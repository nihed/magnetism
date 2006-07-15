package com.dumbhippo.hungry.destructive;

import java.util.Set;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.OrderAfter;

import junit.framework.TestCase;

@OrderAfter(InviteMany.class)
public class AcceptAllInvitations extends TestCase {

	
	public void testAcceptAllInvitations() {
		CheatSheet cs = CheatSheet.getReadOnly();
		
		Set<String> keys = cs.getUnacceptedInvitationAuthKeys();
		
		if (keys.size() == 0) {
			throw new RuntimeException("Should have been some invitations to accept");
		}
		
		for (String k : keys) {
			System.out.println("Accepting invitation " + k);
			VerifyInvitation verify = new VerifyInvitation(k);
			verify.setUp();
			verify.testPage();
		}
		
		keys = cs.getUnacceptedInvitationAuthKeys();
		if (keys.size() > 0) {
			System.err.println("Still have " + keys.size() + " unaccepted invites");
			// this will fail
			assertEquals(keys.size(), 0);
		} else {
			System.out.println("Accepted all invitations successfully");
		}
	}
}
