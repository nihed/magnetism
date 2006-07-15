package com.dumbhippo.hungry.destructive;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.Packet;

import net.sourceforge.jwebunit.WebTester;

import com.dumbhippo.hungry.readonly.ShareLink;
import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.JabberClient;
import com.dumbhippo.hungry.util.OrderAfter;
import com.dumbhippo.hungry.util.SignedInPageTestCase;
import com.dumbhippo.hungry.util.WebServices;

@OrderAfter(AcceptAllInvitations.class)
public class ShareLinkDestructive extends SignedInPageTestCase {

	public ShareLinkDestructive(WebTester t, String userId) {
		super (t, userId);
	}
	
	public ShareLinkDestructive() {
		super();
	}
	
	private static final long PACKET_WAIT_TIME = 10000; // 10s
	
	private void shareLink(WebServices ws, String url, String title, String description, 
			boolean secret, String... recipientIds) {
		
		ArrayList<JabberClient> recipientClients = new ArrayList<JabberClient>(recipientIds.length);
		
		boolean invalidUrl = false;
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			invalidUrl = true;
		}
		
		StringBuilder sb = new StringBuilder();
		for (String r : recipientIds) {
			sb.append(r);
			sb.append(",");
			
			if (!invalidUrl) {
				JabberClient c = new JabberClient(r);
				c.login();
				recipientClients.add(c);
			}
		}
		if (sb.length() > 0)
			sb.delete(sb.length() - 1, sb.length());
		
		String commaRecipients = sb.toString();

		System.out.println(getUserId() + " sharing \"" + title + "\" with " + recipientIds.length + " recipients " + commaRecipients);
		
		ws.getXmlPOST("/sharelink",
				"url", url,
				"title", title, 
				"description", description,
				"recipients", commaRecipients,
				"isPublic", Boolean.toString(!secret));
		
		for (JabberClient c : recipientClients) {
			if (!c.isConnected())
				throw new RuntimeException("One of the recipients got kicked off jabber or something");
			
			boolean gotPacket = false;
			
			// We might, for example, receive a hotness-changed message after the
			// post before the link-share message so we just read packets until
			// we get no packets for 10 seconds, and see if any of the packets we
			// get is the one we were looking for. (This should be refactored
			// rather than cut-and-pasted. Something like 
			// for (Packet packet : c.getPacketsFor(PACKET_WAIT_TIME)) { ... }
			// which could be more sophisticated and not wait forever if a
			// non-matching packet comes in every 9 seconds.)
			while (!gotPacket) {
				//System.out.println("Waiting for recipient to be notified of post...");
				Packet p = c.poll(PACKET_WAIT_TIME);
				if (p == null)
					break;
				//System.out.println("After post share, got packet: " + p.toXML());
				gotPacket = JabberClient.packetContains(p, url);
			}
						
			if (!gotPacket) {
				throw new RuntimeException("Didn't receive packet after link share containing the link");
			}
			
			c.close();
		}
	}
	
	@Override
	public void testPage() {
		
		t.beginAt("/sharelink");
		ShareLink sl = new ShareLink(t, getUserId());
		sl.validatePage();
		
		// now call xmlhttprequests directly to 
		// simulate posting
		CheatSheet cs = CheatSheet.getReadOnly();
		
		WebServices ws = new WebServices(t);
		Set<String> userIds = cs.getSampleUserIds(12);
		
		for (int i = 0; !userIds.isEmpty(); ++i) {
			
			List<String> recipients = new ArrayList<String>();
			int nRecipients = userIds.size() / 3;
			if (nRecipients == 0)
				nRecipients = 1;
			
			while (nRecipients > 0) {
				String r = userIds.iterator().next();
				userIds.remove(r);
				if (!r.equals(getUserId()))
					recipients.add(r);
				--nRecipients;
			}
			
			switch (i) {
			case 0:
				shareLink(ws,
						"http://basicshare.example.com",
						"Basic Share",
						"This is an unremarkable link share. Lorem ipsum vita excolatur tres partes.",
						false,
						(String[])recipients.toArray(new String[recipients.size()]));
				break;
			case 1:
				shareLink(ws,
						"http://secretshare.example.com",
						"Secret!",
						"This share is s3kr3t. Lorem ipsum vita excolatur tres partes.",
						true,
						(String[])recipients.toArray(new String[recipients.size()]));
				break;
			case 2:
				shareLink(ws,
						"https://blah.example.com/blahblah/<b>bold</b><i>italic</i>/",
						"Invalid",
						"This share has an invalid URL with all kinds of HTML special chars in it. Lorem ipsum vita excolatur tres partes.",
						false,
						(String[])recipients.toArray(new String[recipients.size()]));
				break;
			case 3:
				shareLink(ws,
						"https://loooooooooooooooooong.example.com/blahblah/loooooooooooooooooooooooooooooooooooong/blaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaarrgh/",
						"Long link!",
						"This share has an incredibly long URL.",
						false,
						(String[])recipients.toArray(new String[recipients.size()]));
				break;
			case 4:
				shareLink(ws,
						"http://example.com/",
						"Empty description and secret",
						"",
						true,
						(String[])recipients.toArray(new String[recipients.size()]));
				break;
			default:
				// just don't do anything
				break;
			}
		}
	}

	public void validatePage() {
		t.assertElementPresent("dhShareLinkForm");
	}
}
