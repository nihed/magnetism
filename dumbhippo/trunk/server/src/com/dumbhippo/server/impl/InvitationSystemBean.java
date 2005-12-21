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
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.CreateInvitationResult;
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
	
	/**
	 * The inviter argument is optional and means we only return an invitation token if this inviter is
	 * among the inviters.
	 * 
	 * @param inviter only return non-null if this inviter is already in the inviters; null to always return invitation
	 * @param invitee the invitee
	 * @return invitation token or null
	 */
	protected InvitationToken lookupInvitationFor(User inviter, Resource invitee) {
		InvitationToken ret;
		try {
			String inviterClause = "";
			if (inviter != null)
				inviterClause = "AND :inviter MEMBER OF iv.inviters";
			
			// we get the newest invitation token, sort by date in descending order
			Query q = em.createQuery(
				"FROM InvitationToken AS iv WHERE iv.invitee = :resource " + inviterClause + " ORDER BY iv.creationDate DESC");
			q.setParameter("resource", invitee);
			if (inviter != null)
				q.setParameter("inviter", inviter);
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

	// this gets the invitation only if it already exists, but may 
	// update the expiration date and add a new inviter
	public InvitationToken getInvitation(User inviter, Resource invitee) {
		InvitationToken iv = lookupInvitationFor(null, invitee);
		if (iv == null)
			return null;
		iv.addInviter(inviter); // no-op if already added
		return iv;
	}
	
	public Pair<CreateInvitationResult,InvitationToken> createInvitation(User inviter, Resource invitee) {
		// be sure the invitee is our contact (even if we 
		// end up not sending the invite)
		spider.createContact(inviter, invitee);
		
		// this also catches inviting yourself and keeps us from 
		// sending mail to disabled accounts
		User user = spider.lookupUserByResource(invitee);
		if (user != null) {
			logger.debug("not inviting '" + invitee + "' due to existing account " + user);
			return new Pair<CreateInvitationResult,InvitationToken>(CreateInvitationResult.ALREADY_HAS_ACCOUNT, null);
		}
		
		boolean created = false;
		
		InvitationToken iv = lookupInvitationFor(null, invitee);
		if (iv == null || iv.isExpired()) {
			created = true; // renewing an expiration counts as creating (causes us to send new email)
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
				created = true; // adding you as an inviter counts as creating
			
			// this changes the creation date, extending the expiration 
			// time limit
			iv.addInviter(inviter);
			// we don't deduct an invitation from your account if you just "pile on" to an existing one
		}
		
		if (created) {
			return new Pair<CreateInvitationResult,InvitationToken>(CreateInvitationResult.INVITE_CREATED, iv);
		} else {
			return new Pair<CreateInvitationResult,InvitationToken>(CreateInvitationResult.ALREADY_INVITED, iv);
		}
	}
	
	public String sendInvitation(User inviter, Resource invitee, String subject, String message) {	
		Pair<CreateInvitationResult,InvitationToken> p = createInvitation(inviter, invitee);
		CreateInvitationResult result = p.getFirst();
		InvitationToken iv = p.getSecond();
		
		if (result == CreateInvitationResult.ALREADY_HAS_ACCOUNT) {
			User user = spider.lookupUserByResource(invitee);
			return invitee.getHumanReadableString() + " already has an account '" + user.getNickname() + "', now added to your friends list.";
		} else if (result == CreateInvitationResult.INVITE_CREATED) {
			if (invitee instanceof EmailResource) {
				sendEmailNotification(iv, inviter, subject, message);
			} else {
				throw new RuntimeException("no way to send this invite! unhandled resource type " + invitee.getClass().getName());
			}
			return null;
		} else if (result == CreateInvitationResult.ALREADY_INVITED){
			return "You had already invited " + invitee.getHumanReadableString() + " so we didn't send them another mail.";
		} else {
			return null;
		}
	}

	public String sendEmailInvitation(User inviter, String email, String subject, String message) {
		Resource emailRes = spider.getEmail(email);
		return sendInvitation(inviter, emailRes, subject, message);
	}
	
	private void sendEmailNotification(InvitationToken invite, User inviter, String subject, String message) {
		EmailResource invitee = (EmailResource) invite.getInvitee();
		
		if (!noMail.getMailEnabled(invitee)) {
			logger.debug("Mail is disabled to " + invitee + " not sending invitation");
			return;
		}
		
		String inviteeEmail = invitee.getEmail();
		
		MimeMessage msg = mailer.createMessage(Mailer.SpecialSender.INVITATION, inviteeEmail);

		PersonView viewedInviter = spider.getPersonView(new Viewpoint(inviter), inviter);
		String inviterName = viewedInviter.getName();
		
		String baseurl;
		URL baseurlObject;
		try {
			baseurl = configuration.getProperty(HippoProperty.BASEURL);
			baseurlObject = new URL(baseurl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		if (subject == null || subject.trim().length() == 0)
			subject = "Invitation from " + inviterName + " to join Dumb Hippo";
		
		StringBuilder messageText = new StringBuilder();
		XmlBuilder messageHtml = new XmlBuilder();
		
		messageHtml.appendHtmlHead("");
		messageHtml.append("<body>\n");
		
		messageHtml.append("<div style=\"padding: 1em;\">\n");
		messageHtml.appendTextNode("a", "Click here to start using Dumb Hippo", "href", invite.getAuthURL(baseurlObject));
		messageHtml.append("</div>\n");

		messageText.append("\nFollow this link to start using Dumb Hippo:\n      ");
		messageText.append(invite.getAuthURL(baseurlObject));
		messageText.append("\n\n");
		
		if (message != null && message.trim().length() > 0) {
			messageHtml.append("<div style=\"padding: 1.5em;\">\n");
			messageHtml.appendTextAsHtml(message, null);
			messageHtml.append("</div>\n");
			
			messageText.append(message);
			messageText.append("\n\n");
		}
		
		messageHtml.append("<div style=\"padding: 1em;\">\n");
		messageHtml.append("This was sent to you by <a href=\"");
		messageHtml.appendEscaped(baseurl + "/viewperson?who=" + inviter.getId());
		messageHtml.append("\">");
		messageHtml.appendEscaped(inviterName);
		messageHtml.append("</a> using ");
		messageHtml.appendTextNode("a", "Dumb Hippo", "href", baseurl);
		messageHtml.append("</div>\n");
		
		messageText.append("This was sent to you by ");
		messageText.append(inviterName);
		messageText.append(" using Dumb Hippo at ");
		messageText.append(baseurl);
		messageText.append("\n");
		
		messageHtml.append("</body>\n</html>\n");
		
		mailer.setMessageContent(msg, subject, messageText.toString(), messageHtml.toString());

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

		User newUser = acct.getOwner();
		
		if (!em.contains (invite)) {
			// re-attach
			invite = em.find(InvitationToken.class, invite.getId());
		}
		invite.setViewed(true);
		invite.setResultingPerson(newUser);
		
		// needed to fix newUser.getAccount() returning null inside identitySpider?
		em.flush();
		
		// add all inviters as our contacts
		for (User inviter : invite.getInviters()) {
			Account inviterAccount = inviter.getAccount();
			spider.createContact(newUser, inviterAccount);
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

	public boolean hasInvited(User user, Resource invitee) {
		if (user == null)
			throw new IllegalArgumentException("null user to hasInvited");
		
		// iv will be null if user is not among the inviters
		InvitationToken iv = lookupInvitationFor(user, invitee);
		if (iv != null && !iv.isExpired())
			return true;
		else 
			return false;
	}
}
