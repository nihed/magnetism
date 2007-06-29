package com.dumbhippo.server;

import javax.ejb.Local;
import javax.mail.internet.MimeMessage;

import com.dumbhippo.email.MessageContent;
import com.dumbhippo.server.views.UserViewpoint;

@Local
public interface Mailer {

	enum SpecialSender {
		INVITATION("Mugshot Invitation <invitations@mugshot.org>"),
		LOGIN("Mugshot Login <logins@mugshot.org>"),
		VERIFIER("Mugshot Address Verifier <verifier@mugshot.org>"),
		MUGSHOT("Mugshot <mugshot@mugshot.org>"),
		NOBODY("nobody@mugshot.org");
		
		private String address;
		
		SpecialSender(String address) {
			this.address = address;
		}
		
		@Override
		public String toString() {
			return this.address;
		}
	};
	
	public MimeMessage createMessage(UserViewpoint from, SpecialSender viewpointFallbackAddress, String to);
	public MimeMessage createMessage(UserViewpoint from, String to);
	public MimeMessage createMessage(SpecialSender from, String to);
	public MimeMessage createMessage(SpecialSender from, UserViewpoint viewpointReplyTo, 
			                         SpecialSender viewpointFallbackAddress, String to);
	
	public void setMessageContent(MimeMessage message, String subject, String bodyText, String bodyHtml, boolean htmlUsesMugshotLogo);
	
	public void setMessageContent(MimeMessage message, MessageContent content);
	
	public void sendMessage(MimeMessage message);
}
