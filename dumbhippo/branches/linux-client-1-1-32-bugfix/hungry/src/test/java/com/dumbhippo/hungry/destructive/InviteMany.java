package com.dumbhippo.hungry.destructive;

import com.dumbhippo.hungry.util.OrderAfter;

import junit.framework.TestCase;

@OrderAfter(InviteDestructive.class)
public class InviteMany extends TestCase {

	public void testInviteMany() {
		for (int i = 0; i < 7; ++i) {
			InviteDestructive invite = new InviteDestructive();
			invite.setUp();
			invite.testPage();
		}
	}
}
