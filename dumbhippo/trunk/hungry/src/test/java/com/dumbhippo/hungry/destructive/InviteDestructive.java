package com.dumbhippo.hungry.destructive;

import java.security.SecureRandom;
import java.util.Random;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.readonly.Invite;

public class InviteDestructive extends Invite {

	public InviteDestructive(WebTester t, String userId) {
		super(t, userId);
	}
	
	public InviteDestructive() {
		super();
	}
	
	@Override
	public void testPage() {
		// load and validate
		super.testPage();
		
		// now let's invite someone
		Random random = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; ++i) {
			sb.append((char) ('a' + random.nextInt(26)));
		}
		String name = sb.toString() + " Doe";
		String email = sb.toString() + "@example.com";
		
		t.setFormElement("fullName", name);
		t.setFormElement("email", email);
		
		t.submit();
	}
}
