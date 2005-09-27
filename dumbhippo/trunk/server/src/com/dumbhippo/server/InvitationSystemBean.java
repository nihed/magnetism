package com.dumbhippo.server;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.hibernate.Session;

import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class InvitationSystemBean implements InvitationSystem {

	protected static final String invitationFromAddress = "Dumb Hippo Invitation <invitations@dumbhippo.com>";

	protected static final String invitationLandingURLPrefix = "http://dumbhippo.com/newuser?auth=";

	protected Invitation lookupInvitationFor(Person invitee) {
		Session hsession = Storage.getGlobalPerThreadSession().getSession();
		return (Invitation) hsession.createQuery(
				"from Invitation as iv where iv.invitee = :person")
				.setParameter("person", invitee).uniqueResult();
	}

	public Invitation createGetInvitation(Person inviter, Person invitee) {
		SessionWrapper session = Storage.getGlobalPerThreadSession();
		session.beginTransaction();
		Invitation iv = lookupInvitationFor(invitee);
		if (iv == null) {
			iv = new Invitation(invitee, inviter);
			session.getSession().save(iv);
		} else {
			iv.addInviter(inviter);
		}

		session.commitTransaction();
		return iv;
	}

	private String generateAuthURL(Invitation invite) {
		return invitationLandingURLPrefix + invite.getAuthKey();
	}

	public void sendEmailNotification(IdentitySpider spider, Invitation invite, Person inviter) {
		try {
			javax.mail.Session mailsess;
			MimeMessage msg;
			String inviteeEmail = spider.getEmailAddress(invite.getInvitee());
			InternetAddress invitationTo = new InternetAddress(inviteeEmail);
			InternetAddress[] invitationToList = { invitationTo };

			InternetAddress invitationFrom;
			invitationFrom = new InternetAddress(invitationFromAddress, true);

			InternetAddress[] invitationFromList = { invitationFrom };
			mailsess = javax.mail.Session.getDefaultInstance(System
					.getProperties());
			msg = new MimeMessage(mailsess);
			msg.addFrom(invitationFromList);
			String inviterName = spider.getHumanReadableId(inviter);
			msg.setSubject("Invitation from " + inviterName
					+ " to join Dumb Hippo");
			msg.setText("Moo!\n\nYou've been invited by " + inviterName
					+ " to join Dumb Hippo!\n\n"
					+ "Follow this link to see what the mooing's about:\n"
					+ generateAuthURL(invite));
			mailsess.getTransport().sendMessage(msg, invitationToList);
		} catch (AddressException e) {
			throw new RuntimeException(e);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
