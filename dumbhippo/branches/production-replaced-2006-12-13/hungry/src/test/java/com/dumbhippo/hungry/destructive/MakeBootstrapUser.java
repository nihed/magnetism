package com.dumbhippo.hungry.destructive;

import org.jivesoftware.smack.packet.Packet;

import com.dumbhippo.hungry.readonly.Signin;
import com.dumbhippo.hungry.util.JabberClient;
import com.dumbhippo.hungry.util.OrderAfter;

@OrderAfter(VerifyInvitation.class)
public class MakeBootstrapUser extends Signin {

	@Override
	public void testPage() {
		super.testPage();
		
		t.setWorkingForm("dhLoginWithPasswordForm");
		t.setFormElement("address", "bootstrap@example.com");
		t.setFormElement("password", "blahblah");
		t.submit("checkpassword");
		
		String userId = assertSignedIn();
		System.out.println("Bootstrap user is " + userId);
		
		// now try our jabber connection
		JabberClient c = new JabberClient(userId);
		c.login();
		assertTrue(c.isConnected());
		Packet p = c.take();
		if (p == null) {
			throw new RuntimeException("Didn't receive xmpp packet after first user sign-on");
		}
		//System.out.println("Signing on post-invite, got packet: " + p.toXML());
		if (!JabberClient.packetContains(p, "/account")) {
			throw new RuntimeException("xmpp packet received after bootstrap user first sign-on didn't contain /account");
		}
		c.close();
	}
}
