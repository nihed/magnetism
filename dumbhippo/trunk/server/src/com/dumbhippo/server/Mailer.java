package com.dumbhippo.server;

import javax.ejb.Local;
import javax.mail.internet.MimeMessage;

import com.dumbhippo.persistence.Person;

@Local
public interface Mailer {

	enum SpecialSender {
		INVITATION("Dumb Hippo Invitation <invitations@dumbhippo.com>");
		
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
	
	public MimeMessage createMessage(Person from, Person to) throws NoAddressKnownException;
	public MimeMessage createMessage(SpecialSender from, String to);
	
	public void sendMessage(MimeMessage message);
}
