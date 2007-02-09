package com.dumbhippo.hungry.destructive;

import org.jivesoftware.smack.packet.Packet;

import com.dumbhippo.hungry.readonly.Welcome;
import com.dumbhippo.hungry.util.JabberClient;
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
		
		String userId = assertSignedIn();
		
		// now try our jabber connection
		JabberClient c = new JabberClient(userId);
		c.login();
		assertTrue(c.isConnected());
		Packet p = c.take();
		if (p == null) {
			throw new RuntimeException("Didn't receive xmpp packet after first sign-on");
		}
		//System.out.println("Signing on post-invite, got packet: " + p.toXML());
		if (!JabberClient.packetContains(p, "/account")) {
			throw new RuntimeException("xmpp packet received after first sign-on didn't contain /account");
		}
		c.close();
	}
}
