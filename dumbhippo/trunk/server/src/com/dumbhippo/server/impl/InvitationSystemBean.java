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

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationSystemRemote;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.Viewpoint;

@Stateless
public class InvitationSystemBean implements InvitationSystem, InvitationSystemRemote {

	static private final Log logger = GlobalSetup.getLog(InvitationSystemBean.class);
	
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
	
	@EJB
	private NoMailSystem noMail;
	
	protected InvitationToken lookupInvitationFor(Resource invitee) {
		InvitationToken ret;
		try {
			// we get the newest invitation token, sort by date in descending order
			Query q = em.createQuery(
				"FROM InvitationToken AS iv WHERE iv.invitee = :resource ORDER BY iv.creationDate DESC");
			q.setParameter("resource", invitee);
			q.setMaxResults(1); // only need the first one
			ret = (InvitationToken) q.getSingleResult();
			
		} catch (EntityNotFoundException e) {
			ret = null;
		}
		return ret;
	}
	
	public Set<PersonView> findInviters(User invitee, PersonViewExtra... extras) {
		// FIXME this is slightly odd since it merges the inviter lists for all 
		// invitations to the resource ever, instead of using the newest InvitationToken
		// (right now, all InvitationToken after the earliest are based on the old 
		// ones, so it makes no difference however)
		Query query = em.createQuery("select inviter from " +
								     "User inviter, InvitationToken invite, AccountClaim ar " + 
								     "where inviter member of invite.inviters and " +
								     "ar.resource = invite.invitee and " +
								     "ar.owner = :invitee");
		query.setParameter("invitee", invitee);
		
		@SuppressWarnings("unchecked")
		List<Person> inviters = query.getResultList(); 
		
		Viewpoint viewpoint = new Viewpoint(invitee);
		
		Set<PersonView> result = new HashSet<PersonView>();
		for (Person p : inviters)
			result.add(spider.getPersonView(viewpoint, p, extras));
		
		return result; 
	}

	private Account getAccount(User inviter) {
		Account account = inviter.getAccount();
		if (account == null || !em.contains(account))
			account = accounts.lookupAccountByUser(inviter);
		if (account == null)
			throw new RuntimeException("user " + inviter + " with no account???");
		return account;
	}

	// this gets the invitation url only if it already exists, but may 
	// update the expiration date and add a new inviter
	public String getInvitationUrl(User inviter, Resource invitee) {
		InvitationToken iv = lookupInvitationFor(invitee);
		if (iv == null)
			return null;
		iv.addInviter(inviter); // no-op if already added
		return iv.getAuthURL(configuration.getProperty(HippoProperty.BASEURL));
	}
	
	public String sendInvitation(User inviter, Resource invitee) {
		
		// be sure the invitee is our contact (even if we 
		// end up not sending the invite)
		spider.createContact(inviter, invitee);
		
		// this also catches inviting yourself and keeps us from 
		// sending mail to disabled accounts
		User user = spider.lookupUserByResource(invitee);
		if (user != null) {
			logger.debug("not inviting '" + invitee + "' due to existing account " + user);
			return invitee.getHumanReadableString() + " already has an account '" + user.getNickname() + "', now added to your friends list.";
		}
		
		boolean needSendNotification = false;
		String ret = null;
		
		InvitationToken iv = lookupInvitationFor(invitee);
		if (iv == null || iv.isExpired()) {
			needSendNotification = true; // always send if expired, since it's been a while
			if (iv != null) {
				// create a new auth key that isn't expired,
				// preserving current inviter list etc.
				iv = new InvitationToken(iv);
				iv.addInviter(inviter);
			} else {
				iv = new InvitationToken(invitee, inviter);
			}
			em.persist(iv);
			
			Account account = getAccount(inviter);
			
			// if we messed up and let them send too many invitations,
			// we just live with it here; we should have blocked
			// it or displayed an error in the UI before now.
			// Yes this is a race that could let someone send more 
			// invitations than they were allowed to, but that's 
			// better than throwing a mysterious error at this stage.
			// I can't figure out how you'd exploit it to 
			// go crazy and send hundreds of extra invitations.
			if (account.canSendInvitations(1))
				account.deductInvitations(1);
		} else {
			if (!iv.getInviters().contains(inviter))
				needSendNotification = true;
			
			// this changes the creation date, extending the expiration 
			// time limit
			iv.addInviter(inviter);
			// we don't deduct an invitation from your account if you just "pile on" to an existing one
		}
		
		if (needSendNotification) {
			if (invitee instanceof EmailResource) {
				sendEmailNotification(iv, inviter);
			} else {
				throw new RuntimeException("no way to send this invite! unhandled resource type " + invitee.getClass().getName());
			}
		} else {
			ret = "You had already invited " + invitee.getHumanReadableString() + " so we didn't send them another mail.";
		}
		
		return ret;
	}

	public String sendEmailInvitation(User inviter, String email) {
		Resource emailRes = spider.getEmail(email);
		return sendInvitation(inviter, emailRes);
	}	
	
	private void sendEmailNotification(InvitationToken invite, User inviter) {
		EmailResource invitee = (EmailResource) invite.getInvitee();
		
		if (!noMail.getMailEnabled(invitee)) {
			logger.debug("Mail is disabled to " + invitee + " not sending invitation");
			return;
		}
		
		String inviteeEmail = invitee.getEmail();
		
		MimeMessage msg = mailer.createMessage(Mailer.SpecialSender.INVITATION, inviteeEmail);

		PersonView viewedInviter = spider.getPersonView(new Viewpoint(inviter), inviter);
		String inviterName = viewedInviter.getName();
		
		URL url;
		try {
			String baseurl = configuration.getProperty(HippoProperty.BASEURL);
			url = new URL(baseurl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		try {
			msg.setSubject("Invitation from " + inviterName + " to join Dumb Hippo");
			msg.setText("Moo!\n\nYou've been invited by " + inviterName + " to join Dumb Hippo!\n\n"
					+ "Follow this link to see what the mooing's about:\n" + invite.getAuthURL(url));
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

		mailer.sendMessage(msg);
	}

	protected void notifyInvitationViewed(InvitationToken invite) {
		// adding @suppresswarnings here makes javac crash, whee
		//for (Person inviter : invite.getInviters()) {
		// TODO send notification via xmpp to inviter that invitee viewed
		//}
	}
	
	public Pair<Client,User> viewInvitation(InvitationToken invite, String firstClientName, boolean disable) {
		if (invite.isViewed()) {
			throw new IllegalArgumentException("InvitationToken " + invite + " has already been viewed");
		}
		
		Resource invitationResource = invite.getInvitee();
		Account acct = accounts.createAccountFromResource(invitationResource);
		if (disable)
			acct.setDisabled(true);
		
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

		if (!disable)
			notifyInvitationViewed(invite);
		
		// FIXME this cast is just laziness to avoid changing the db schema of InvitationToken
		return new Pair<Client,User>(client, (User) invite.getResultingPerson());
	}

	public Collection<String> getInviterNames(InvitationToken invite) {
		Set<String> names = new HashSet<String>();  
		for (User inviter : invite.getInviters()) {
			PersonView view = spider.getSystemView(inviter);
	        String readable = view.getName();
	        if (readable != null) {    
	        	names.add(readable);
	        }
		}
		return Collections.unmodifiableCollection(names);
	}

	public int getInvitations(User user) {
		Account account = getAccount(user);
		return account.getInvitations();
	}
}
