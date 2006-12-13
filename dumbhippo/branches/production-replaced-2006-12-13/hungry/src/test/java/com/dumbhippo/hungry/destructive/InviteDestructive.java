package com.dumbhippo.hungry.destructive;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.readonly.Invite;
import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.OrderAfter;

@OrderAfter(MakeBootstrapUser.class)
public class InviteDestructive extends Invite {

	public InviteDestructive(WebTester t, String userId) {
		super(t, userId);
	}
	
	public InviteDestructive() {
		super();
	}
	
	@Override
	public void testPage() {
		// be sure we have some invitations
		CheatSheet cs = CheatSheet.getReadWrite();
		cs.setNumberOfInvitations(getUserId(), 10);

		// load and validate
		super.testPage();		
		
		// now let's invite someone
// // This is disabled for now until we investigate executing the JavaScript
//		Random random = new SecureRandom();
//		StringBuilder sb = new StringBuilder();
//		for (int i = 0; i < 10; ++i) {
//			sb.append((char) ('a' + random.nextInt(26)));
//		}
//		String name = sb.toString() + " Doe";
//		String email = sb.toString() + "@example.com";
//		
//		t.setWorkingForm("dhInvitationForm");
//		t.setFormElement("dhSubjectEntry", "Yo " + name + ", join up");
//		t.setFormElement("dhAddressEntry", email);
//		t.setFormElement("dhMessageEntry", "This is my message to you");
//		
//		t.submit();
	}
}
