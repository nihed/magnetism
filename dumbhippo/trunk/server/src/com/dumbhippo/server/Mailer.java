package com.dumbhippo.server;

import javax.ejb.Local;
import javax.mail.internet.MimeMessage;

@Local
public interface Mailer {

	enum SpecialSender {
		INVITATION("Dumb Hippo Invitation <invitations@dumbhippo.com>"),
		LOGIN("Dumb Hippo Login <logins@dumbhippo.com>"),
		VERIFIER("Dumb Hippo Address Verifier <verifier@dumbhippo.com>"),
		NOBODY("nobody@dumbhippo.com");
		
		private String address;
		
		SpecialSender(String address) {
			this.address = address;
		}
		
		@Override
		public String toString() {
			return this.address;
		}
	};
	
	public MimeMessage createMessage(UserViewpoint from, String to);
	public MimeMessage createMessage(SpecialSender from, String to);
	
	public void setMessageContent(MimeMessage message, String subject, String bodyText, String bodyHtml);
	
	public void sendMessage(MimeMessage message);
}
