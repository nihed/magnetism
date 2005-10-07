/**
 * 
 */
package com.dumbhippo.persistence;

import java.net.URL;

import javax.annotation.EJB;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;


/**
 * @author hp
 *
 */
@Entity
public class EmailResource extends Resource implements InvitableResource {
	
	private static final long serialVersionUID = 0L;
	
	@EJB
	private transient IdentitySpider spider;

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
	
	
	/**
	 * This is protected so only the container calls it. 
	 * This is because EmailResource is treated as immutable,
	 * i.e. once a GUID-EmailAddress pair exists, we never 
	 * change the address associated with that GUID. 
	 * So you don't want to setEmail(). Instead, create
	 * a new EmailResource with the new email.
	 * 
	 * @param email
	 */
	protected void setEmail(String email) {
		this.email = email;
	}

	public void sendInvite(URL prefix, Invitation invitation, Person inviter) {
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
