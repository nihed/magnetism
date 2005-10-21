package com.dumbhippo.server.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationSystemRemote;
import com.dumbhippo.server.PersonView;

@Stateless
public class InvitationSystemBean implements InvitationSystem, InvitationSystemRemote {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private AccountSystem accounts;
	
	@EJB
	private IdentitySpider spider;
	
	@javax.annotation.Resource(name="java:/Mail")
	private Session mailSession;
	
	protected Invitation lookupInvitationFor(Resource invitee) {
		Invitation ret;
		try {
			ret = (Invitation) em.createQuery(
				"from Invitation as iv where iv.invitee = :resource")
				.setParameter("resource", invitee).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}

	public Invitation createGetInvitation(Person inviter, Resource invitee) {
		Invitation iv = lookupInvitationFor(invitee);
		if (iv == null) {
			iv = new Invitation(invitee, inviter);
			em.persist(iv);
		} else {
			iv.addInviter(inviter);
		}

		return iv;
	}

	public Invitation createEmailInvitation(Person inviter, String email) {
		Resource emailRes = spider.getEmail(email);
		return createGetInvitation(inviter, emailRes);
	}	
	
	protected static final String invitationFromAddress = "Dumb Hippo Invitation <invitations@dumbhippo.com>";
	
	public void sendEmailNotification(Invitation invite, Person inviter) {
		EmailResource invitee = (EmailResource) invite.getInvitee();

		try {
			MimeMessage msg;
			PersonView viewedInviter = spider.getViewpoint(null, inviter);
			
			String inviteeEmail = invitee.getEmail();
			InternetAddress invitationTo = new InternetAddress(inviteeEmail);

			InternetAddress invitationFrom;
			invitationFrom = new InternetAddress(invitationFromAddress, true);
						
			msg = new MimeMessage(mailSession);
			
			msg.setFrom(invitationFrom);
			msg.setRecipient(Message.RecipientType.TO, invitationTo);
			
			String inviterName = viewedInviter.getHumanReadableName();
			msg.setSubject("Invitation from " + inviterName
					+ " to join Dumb Hippo");
			URL url;
			try {
				String baseurl = System.getProperty("dumbhippo.server.baseurl", "http://dumbhippo.com");
				url = new URL(baseurl); 
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
			msg.setText("Moo!\n\nYou've been invited by " + inviterName
					+ " to join Dumb Hippo!\n\n"
					+ "Follow this link to see what the mooing's about:\n"
					+ invite.getAuthURL(url));
			Transport.send(msg);
		} catch (AddressException e) {
			throw new RuntimeException(e);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	public Invitation lookupInvitationByKey(String authKey) {
		Invitation ret;
		try {
			ret = (Invitation) em.createQuery(
				"from Invitation as iv where iv.authKey = :key")
				.setParameter("key", authKey).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		} catch (Exception e) { // FIXME !  needed because an org.hibernate. exception gets thrown
			                    // probably a jboss bug
			ret = null;
		}
		return ret;
	}

	protected void notifyInvitationViewed(Invitation invite) {
		// adding @suppresswarnings here makes javac crash, whee
		for (Person inviter : invite.getInviters()) {
			// TODO send notification via xmpp to inviter that invitee viewed
		}
	}
	
	public Client viewInvitation(Invitation invite, String firstClientName) {
		if (invite.isViewed()) {
			throw new IllegalArgumentException("Invitation " + invite + "has already been viewed");
		}
		
		Resource invitationResource = invite.getInvitee();
		HippoAccount acct = accounts.createAccountFromResource(invitationResource);
		
		Client client = null;
		if (firstClientName != null) {
			client = accounts.authorizeNewClient(acct, firstClientName);
		}

		invite.setViewed(true);
		invite.setResultingPerson(acct.getOwner());
		if (!em.contains (invite)) {
			// we have to modify a persisted copy also...
			Invitation persisted = em.find(Invitation.class, invite.getId());
			persisted.setViewed(true);
			persisted.setResultingPerson(acct.getOwner());
		}

		notifyInvitationViewed(invite);
		
		return client;
	}

	public Collection<String> getInviterNames(Invitation invite) {
		Set<String> names = new HashSet<String>();  
		for (Person inviter : invite.getInviters()) {
			PersonView view = spider.getSystemViewpoint(inviter);
	        String readable = view.getHumanReadableName();
	        if (readable != null) {    
	        	names.add(readable);
	        }
		}
		return Collections.unmodifiableCollection(names);
	}
}
