package com.dumbhippo.server;

import javax.ejb.Local;
import javax.mail.internet.MimeMessage;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.User;

@Local
public interface Mailer {

	enum SpecialSender {
		INVITATION("Dumb Hippo Invitation <invitations@dumbhippo.com>"),
		LOGIN("Dumb Hippo Login <logins@dumbhippo.com>"),
		NOBODY("nobody@dumbhippo.com");
		
		private String address;
		
		SpecialSender(String address) {
			this.address = address;
		}
		
		public String toString() {
			return this.address;
		}
	};
	
	public static class NoAddressKnownException extends Exception {
		private static final long serialVersionUID = 0L;

		public NoAddressKnownException(String message) {
			super(message);
		}
	}
	
	public MimeMessage createMessage(User from, Person to) throws NoAddressKnownException;
	public MimeMessage createMessage(SpecialSender from, String to);
	
	public void setMessageContent(MimeMessage message, String subject, String bodyText, String bodyHtml);
	
	public void sendMessage(MimeMessage message);
}
