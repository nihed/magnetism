package com.dumbhippo.server.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationSystemRemote;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class InvitationSystemBean implements InvitationSystem, InvitationSystemRemote {

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private AccountSystem accounts;
	
	@EJB
	private IdentitySpider spider;
	
	@EJB
	private Mailer mailer;
	
	@EJB
	private Configuration configuration;
	
	protected InvitationToken lookupInvitationFor(Resource invitee) {
		InvitationToken ret;
		try {
			ret = (InvitationToken) em.createQuery(
				"from InvitationToken as iv where iv.invitee = :resource")
				.setParameter("resource", invitee).getSingleResult();
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}
	
	public Set<PersonView> findInviters(Person invitee) {
		Query query = em.createQuery("select inviter from " +
								     "Person inviter, InvitationToken invite, ResourceOwnershipClaim roc " + 
								     "where inviter in elements(invite.inviters) and " +
								     "roc.resource = invite.invitee and " +
								     "roc.assertedBy = :theman and " +
								     "roc.claimedOwner = :invitee");
		query.setParameter("invitee", invitee);
		query.setParameter("theman", spider.getTheMan());
		
		@SuppressWarnings("unchecked")
		List<Person> inviters = query.getResultList(); 
		
		Viewpoint viewpoint = new Viewpoint(invitee);
		
		Set<PersonView> result = new HashSet<PersonView>();
		for (Person p : inviters)
			result.add(spider.getPersonView(viewpoint, p));
		
		return result; 
	}

	public InvitationToken createGetInvitation(Person inviter, Resource invitee) {
		InvitationToken iv = lookupInvitationFor(invitee);
		if (iv == null) {
			iv = new InvitationToken(invitee, inviter);
			em.persist(iv);
		} else {
			iv.addInviter(inviter);
		}

		return iv;
	}

	public InvitationToken createEmailInvitation(Person inviter, String email) {
		Resource emailRes = spider.getEmail(email);
		return createGetInvitation(inviter, emailRes);
	}	
	
	protected static final String invitationFromAddress = "Dumb Hippo InvitationToken <invitations@dumbhippo.com>";
	
	public void sendEmailNotification(InvitationToken invite, Person inviter) {
		EmailResource invitee = (EmailResource) invite.getInvitee();
		String inviteeEmail = invitee.getEmail();

		MimeMessage msg = mailer.createMessage(Mailer.SpecialSender.INVITATION, inviteeEmail);

		PersonView viewedInviter = spider.getPersonView(new Viewpoint(inviter), inviter);
		String inviterName = viewedInviter.getHumanReadableName();
		
		URL url;
		try {
			String baseurl = configuration.getProperty(HippoProperty.BASEURL);
			url = new URL(baseurl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		try {
			msg.setSubject("InvitationToken from " + inviterName + " to join Dumb Hippo");
			msg.setText("Moo!\n\nYou've been invited by " + inviterName + " to join Dumb Hippo!\n\n"
					+ "Follow this link to see what the mooing's about:\n" + invite.getAuthURL(url));
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

		mailer.sendMessage(msg);
	}

	protected void notifyInvitationViewed(InvitationToken invite) {
		// adding @suppresswarnings here makes javac crash, whee
		for (Person inviter : invite.getInviters()) {
			// TODO send notification via xmpp to inviter that invitee viewed
		}
	}
	
	public Pair<Client,Person> viewInvitation(InvitationToken invite, String firstClientName) {
		if (invite.isViewed()) {
			throw new IllegalArgumentException("InvitationToken " + invite + "has already been viewed");
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
			InvitationToken persisted = em.find(InvitationToken.class, invite.getId());
			persisted.setViewed(true);
			persisted.setResultingPerson(acct.getOwner());
			invite = persisted;
		}

		notifyInvitationViewed(invite);
		
		return new Pair<Client,Person>(client, invite.getResultingPerson());
	}

	public Collection<String> getInviterNames(InvitationToken invite) {
		Set<String> names = new HashSet<String>();  
		for (Person inviter : invite.getInviters()) {
			PersonView view = spider.getSystemView(inviter);
	        String readable = view.getHumanReadableName();
	        if (readable != null) {    
	        	names.add(readable);
	        }
		}
		return Collections.unmodifiableCollection(names);
	}
}
