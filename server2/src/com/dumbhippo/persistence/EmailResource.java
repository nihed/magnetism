/**
 * 
 */
package com.dumbhippo.persistence;

import java.net.URL;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.server.IdentitySpider;


/**
 * @author hp
 *
 */
@Entity
public class EmailResource extends Resource implements InvitableResource {
	private String email;
	
	protected static final String invitationFromAddress = "Dumb Hippo Invitation <invitations@dumbhippo.com>";
	
	protected EmailResource() {}

	public EmailResource(String string) {
		super();
		setEmail(string);
	}

	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}

	public void sendInvite(IdentitySpider spider, URL prefix, Invitation invitation, Person inviter) {
		try {
			javax.mail.Session mailsess;
			MimeMessage msg;
			PersonView viewedInviter = spider.getViewpoint(null, inviter);
			
			String inviteeEmail = getEmail();
			InternetAddress invitationTo = new InternetAddress(inviteeEmail);
			InternetAddress[] invitationToList = { invitationTo };

			InternetAddress invitationFrom;
			invitationFrom = new InternetAddress(invitationFromAddress, true);

			InternetAddress[] invitationFromList = { invitationFrom };
			mailsess = javax.mail.Session.getDefaultInstance(System
					.getProperties());
			msg = new MimeMessage(mailsess);
			msg.addFrom(invitationFromList);
			String inviterName = viewedInviter.getHumanReadableName();
			msg.setSubject("Invitation from " + inviterName
					+ " to join Dumb Hippo");
			msg.setText("Moo!\n\nYou've been invited by " + inviterName
					+ " to join Dumb Hippo!\n\n"
					+ "Follow this link to see what the mooing's about:\n"
					+ invitation.getAuthURL(prefix));
			mailsess.getTransport().sendMessage(msg, invitationToList);
		} catch (AddressException e) {
			throw new RuntimeException(e);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	@Transient
	public String getHumanReadableString() {
		return getEmail();
	}
}
