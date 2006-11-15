package com.dumbhippo.server.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.InviterData;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.CreateInvitationResult;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationSystemRemote;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.InvitationView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;

@Stateless
public class InvitationSystemBean implements InvitationSystem, InvitationSystemRemote {

	static private final Logger logger = GlobalSetup.getLogger(InvitationSystemBean.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private PersonViewer personViewer;
	
	@EJB
	private AccountSystem accounts;
	
	@EJB
	private GroupSystem groupSystem;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private IdentitySpider spider;
	
	@EJB
	private Mailer mailer;
	
	@EJB
	private Configuration configuration;
	
	@EJB
	private NoMailSystem noMail;
	
	@EJB 
	private WantsInSystem wantsInSystem;
	
	@EJB
	private IdentitySpider identitySpider;
	
	public InvitationToken lookupInvitationFor(User inviter, Resource invitee) {
		InvitationToken invite;
		try {
			String inviterClause = "";
			if (inviter != null)
				inviterClause = "AND EXISTS (SELECT ivd FROM InviterData ivd WHERE ivd MEMBER OF iv.inviters AND ivd.inviter = :inviter)";
			
			// we get the newest invitation token, sort by date in descending order
			Query q = em.createQuery(
				"FROM InvitationToken AS iv WHERE iv.invitee = :resource " + inviterClause + " ORDER BY iv.creationDate DESC");
			q.setParameter("resource", invitee);
			if (inviter != null)
				q.setParameter("inviter", inviter);
			q.setMaxResults(1); // only need the first one
			invite = (InvitationToken) q.getSingleResult();
			
		} catch (NoResultException e) {
			invite = null;
		}
		return invite;
	}


	public InvitationToken lookupInvitation(UserViewpoint viewpoint, long id) {
		InvitationToken token;		
		try {
			token = em.find(InvitationToken.class, id);
		} catch (NoResultException e) {
			return null;
		}
		if (!token.getResultingPerson().equals(viewpoint.getViewer())) {
			logger.debug("Can't access invitation id " + id + " from user " + viewpoint.getViewer());
			return null;
		}
		return token;
	}	
	
	public InvitationToken lookupInvitation(User inviter, String authKey) {
		InvitationToken invite;
		try {

			Query q = em.createQuery(
				"FROM InvitationToken AS iv WHERE iv.authKey = :authKey AND " + 
				"EXISTS (SELECT ivd FROM InviterData ivd WHERE " +
				         "ivd MEMBER OF iv.inviters AND " +
				         "ivd.inviter = :inviter)");		
			q.setParameter("authKey", authKey);
            q.setParameter("inviter", inviter);
			// we expect there to be at most one result because authentication key must be unique
			invite = (InvitationToken) q.getSingleResult();		
		} catch (NoResultException e) {
			invite = null;
		} catch (NonUniqueResultException e) {
			throw new RuntimeException("Multiple InvitationToken results for authentication key " 
					                   + authKey + " while authentication key must be unique. " 
					                   + e.getMessage());
		}
		return invite;		
	}

	public InvitationView lookupInvitationViewFor(UserViewpoint viewpoint, Resource invitee) {
		// when someone is viewing an invitation, they can see it only if they
		// are an inviter
		User inviter = viewpoint.getViewer();
		InvitationToken invite = lookupInvitationFor(inviter, invitee);
		if (invite == null) {
			return null;
		}
		
		InviterData ivd = getInviterData(invite, inviter);
		
		Set<Group> suggestedGroups = 
			groupSystem.getInvitedToGroups(ivd.getInviter(), invite.getInvitee());
		
        InvitationView invitationView = new InvitationView(invite, ivd, suggestedGroups);
        
        return invitationView;		
	}
	
	/*
	public Set<PersonView> findInviters(UserViewpoint viewpoint, PersonViewExtra... extras) {
		User invitee = viewpoint.getViewer();
		
		// All InvitationTokens after the earliest one are based on the old 
		// ones, so we only want to get the InviterData for the newest InvitationToken
		// for the invitee. This helps us avoid returning duplicate inviters.
		// This will return inviters whose invitations are expired, but will not return
		// inviters who deleted their invitation.
		Query query = em.createQuery("SELECT inviterData FROM " +
								     "InviterData inviterData, InvitationToken invite, AccountClaim ar " + 
								     "WHERE inviterData MEMBER OF invite.inviters AND " +
								     "inviterData.deleted = FALSE AND " +
								     "ar.resource = invite.invitee AND " +
								     "ar.owner = :invitee AND " +
								     "invite.creationDate = " +
								     "(SELECT MAX(invite2.creationDate) " +
								     " FROM InvitationToken invite2, AccountClaim ar2 " +
								     " WHERE ar2.resource = invite2.invitee AND " +
								     " ar2.owner = :invitee)");
		query.setParameter("invitee", invitee);
		
		@SuppressWarnings("unchecked")
		List<InviterData> inviters = query.getResultList(); 
		
		Set<PersonView> result = new HashSet<PersonView>();
		for (InviterData inviter : inviters)
			result.add(spider.getPersonView(viewpoint, inviter.getInviter(), extras));
		
		return result; 
	}
	*/

	public List<InvitationView> findOutstandingInvitations(UserViewpoint viewpoint, 
			                                               int start, 
			                                               int max) {	
		// we want to provide the invitations for which the person viewing the 
		// invitations is the inviter
		User inviter = viewpoint.getViewer();
		// get only the InvitationTokens for which ResultingPerson is null,
		// sorted by date in descending order
		Query q = em.createQuery(
			"FROM InvitationToken AS iv WHERE EXISTS " +
			"(SELECT ivd FROM InviterData ivd WHERE " +
			"ivd MEMBER OF iv.inviters AND " +
			"ivd.inviter = :inviter AND " +
			"ivd.deleted = FALSE AND " +
			"iv.resultingPerson = NULL) " +
			"ORDER BY iv.creationDate DESC");
		
		q.setParameter("inviter", inviter);
		
		if (max > 0)
			q.setMaxResults(max);
		
		q.setFirstResult(start);
		
		@SuppressWarnings("unchecked")
		List<InvitationToken> outstandingInvitations = q.getResultList();
		
		List<InvitationView> outstandingInvitationViews = new ArrayList<InvitationView> ();
		
		// can we mix accessing data through the database with accessing it through 
		// the persistence classes? should this also be a database query?
		for (InvitationToken invite : outstandingInvitations) {
			if (!invite.isValid()) {
				// deleted invitations should have been filtered out by the query,
				// but we also want to filter out expired invitations
				continue;
			}			
			InviterData inviterData = getInviterData(invite, inviter);
			// inviterData should not come back null, because we just obtained all 
			// the invitations sent out by this particular inviter, but if it
			// is null, it's ok to pass it to the invitationView constructor too
			Set<Group> suggestedGroups = 
				groupSystem.getInvitedToGroups(inviterData.getInviter(), invite.getInvitee());
            InvitationView invitationView = new InvitationView(invite, inviterData, suggestedGroups);
            outstandingInvitationViews.add(invitationView);
		}
			
		return outstandingInvitationViews; 
	}
	
	public int countOutstandingInvitations(UserViewpoint viewpoint) {		
		// it would have been nice to use a COUNT query here, but because 
		// we want to not count expired invitations, we need to query
		// for invitation tokens
		// next, it seems to be better to construct an unneeded InvitationView
		// list, than to duplicate the filtering in findOutstandingInvitations
		return findOutstandingInvitations(viewpoint, 0, -1).size();
	}
	
	public InvitationView deleteInvitation(UserViewpoint viewpoint, String authKey) {
		User inviter = viewpoint.getViewer();
		// Could have used lookupInvitationFor if made the deletion based on the
		// e-mail address, and not the authentication key. Would have to make
		// sure to go through all InvitationTokens for the invitee then. There
		// has to be only one that is valid, but can be multiple expired ones.
		InvitationToken invite = lookupInvitation(inviter, authKey);
		
		if (invite == null) {
			return null;
		}
		
		InviterData ivd = getInviterData(invite, inviter);
		
		// based on the result of lookupInvitation, ivd shouldn't be null, 
		// but just in case
		if (ivd == null) {
			return null;
		}
		
		if (ivd.isDeleted()) {
			// if it is already marked deleted, there is nothing to "delete"
			return null;
		}
		
		ivd.setDeleted(true);
		// there is still a loophole here, because if someone spent their
		// invitation on this invitation and is getting reimbursed now, 
		// while someone else piled on or used to have an old expired invitation, 
		// this invitation ends up being "free" 
		// not sure if it is worth it to do anything about it now		
        if (ivd.isInvitationDeducted()) {
        	// reimburse
    		Account account = getAccount(inviter);
    		account.addInvitations(1);
    	    // We do not want to unset invitationDeducted here, because we want to
    		// preserve the information on whether an invitation voucher was originally 
    		// deducted for logic in restoring an invitation. 
        }
        
        // go through all the inviters, if they all have deleted their invitation,
        // the token should be marked as deleted
        boolean deleteToken = true;
        for (InviterData inviterData : invite.getInviters()) {
        	if (!inviterData.isDeleted()) {
        		deleteToken = false;
        		break;
        	}
		}
        
        if (deleteToken) {
        	invite.setDeleted(true);
        }
		
		Set<Group> suggestedGroups = 
			groupSystem.getInvitedToGroups(ivd.getInviter(), invite.getInvitee());
        InvitationView invitationView = new InvitationView(invite, ivd, suggestedGroups);
        return invitationView;		
	}
	
	public void restoreInvitation(UserViewpoint viewpoint, String authKey) {
		User inviter = viewpoint.getViewer();
		InvitationToken invite = lookupInvitation(inviter, authKey);
		
		if (invite == null) {
			return;
		}
		
		InviterData ivd = getInviterData(invite, inviter);
		
		// based on the result of lookupInvitation, ivd shouldn't be null, 
		// but just in case
		if (ivd == null) {
			return;
		}
			
		if (!ivd.isDeleted()) {
			// nothing to restore
			return;
		}
		
		ivd.setDeleted(false);		
        if (ivd.isInvitationDeducted()) {
        	// if the invitation was originally deducted, it must have been 
        	// reimbursed, so deduct it again
    		Account account = getAccount(inviter);
    		// this is a loophole, but normally the user would not do anything
    		// between deleting an invitation and trying to restore it
			if (account.canSendInvitations(1)) {				
    		    account.deductInvitations(1);
			}
        }
        	
        invite.setDeleted(false);	
	}
	
	private Account getAccount(User inviter) {
		Account account = inviter.getAccount();
		if (account == null || !em.contains(account))
			account = accounts.lookupAccountByUser(inviter);
		if (account == null)
			throw new RuntimeException("user " + inviter + " with no account???");
		return account;
	}

	public InvitationToken updateValidInvitation(User inviter, Resource invitee, boolean addToInvitation) {
		InvitationToken iv = lookupInvitationFor(null, invitee);
		if (iv == null || !iv.isValid())
			return null;
		if (addToInvitation && getInviterData(iv, inviter) == null) {
			// it's a new inviter for this invitation, so we can add him to
			// the inviters list and update the invitation date
			long currentTime = System.currentTimeMillis();
			InviterData ivd = 
				new InviterData(inviter, currentTime, 
						        "You joined an existing invitation.", 
						        "", false);
			em.persist(ivd);
			iv.addInviter(ivd);
			iv.setCreationDate(new Date(currentTime));
		}
		return iv;
	}
	
	public Pair<CreateInvitationResult,InvitationToken> 
	    createInvitation(User inviter, PromotionCode promotionCode, Resource invitee, 
	    		         String subject, String message) {
		// be sure the invitee is our contact (even if we 
		// end up not sending the invite)
		spider.createContact(inviter, invitee);
		// this also catches inviting yourself and keeps us from 
		// sending mail to disabled accounts 
		User user = spider.lookupUserByResource(SystemViewpoint.getInstance(), invitee);
		if (user != null) {
			logger.debug("not inviting '{}' due to existing user {}", invitee, user);
			return new Pair<CreateInvitationResult,InvitationToken>(CreateInvitationResult.ALREADY_HAS_ACCOUNT, null);
		}

		CreateInvitationResult result = CreateInvitationResult.REPEAT_INVITE;
		InvitationToken iv = lookupInvitationFor(null, invitee);
		if (iv == null || !iv.isValid()) {	
			Account account = getAccount(inviter);
			// first things first, do they have the dough
            boolean invitationDeducted = false;
			if (account.canSendInvitations(1)) {
				account.deductInvitations(1);
				invitationDeducted = true;
			} else {
				result = CreateInvitationResult.INVITE_WAS_NOT_CREATED;
				return new Pair<CreateInvitationResult,InvitationToken>(result, null);
			}
			
			// renewing an expiration counts as creating (causes us to send new email)
			result = CreateInvitationResult.INVITE_CREATED; 
			if (iv != null) {	
				iv = updateInvitation(iv, inviter, subject, message, invitationDeducted);				
			} else {		
				InviterData inviterData = new InviterData(inviter, subject, message, invitationDeducted);
				em.persist(inviterData);
				iv = new InvitationToken(invitee, inviterData);
				iv.setPromotionCode(promotionCode);
				em.persist(iv);
			}			
		} else {	
			InviterData ivd = getInviterData(iv, inviter); 
			if (ivd == null) {	
				// adding a person as an inviter causes us to send e-mail and update
				// invite information, though not deduct invitations available to
				// the inviter
				result = CreateInvitationResult.NEW_INVITER; 
				iv = updateInvitation(iv, inviter, subject, message, false);				
			} else {	
				// this is the default case of CreateInvitationResult.REPEAT_INVITE
				// when someone wants to resend an invitation
				// we want update the invitation with the new subject and message,
				// but want to preserve the information on whether the person
				// previously spent an invitation on the invite
				iv = updateInvitation(iv, inviter, subject, message, ivd.isInvitationDeducted());
			}
			
			// If the invitation doesn't already have a promotion code, set
			// one, but if does, then we simply ingore the new code;
			// keeping a list of promotion codes isn't worth the complexity.
			if (promotionCode != null && iv.getPromotion() == null)
				iv.setPromotionCode(promotionCode);
		}
	    return new Pair<CreateInvitationResult,InvitationToken>(result, iv);
	}
	
	/**
	 * Creates new inviter data for the invitation or updates the existing one.
	 * Creates a new invitation token with the same data as the old invitation token,
	 * if the old one has expired.
	 * 
	 * @param iv the invitation token
	 * @param inviter the inviter for the inviter data
	 * @param subject subject of inviter's invitation
	 * @param message message of inviter's invitation
	 * @param invitationDeducted flag indicating if invitations were deducted from the inviter
	 *                           to send out this invitation
	 */
	private InvitationToken updateInvitation(InvitationToken iv, User inviter, 
			                                 String subject, String message, 
			                                 boolean invitationDeducted) {
		// in case it has been deleted previously
		iv.setDeleted(false);
		InviterData ivd = getInviterData(iv, inviter);
		long currentTime = System.currentTimeMillis();
		if (ivd == null) {	
			ivd = new InviterData(inviter, currentTime, subject, message, invitationDeducted);
			em.persist(ivd);
            // in some cases, this will add a new inviter to an expired invitation, 
			// but it's ok
			iv.addInviter(ivd);
		} else {		
			ivd.setInvitationDate(new Date(currentTime));
			ivd.setInvitationSubject(subject);
			ivd.setInvitationMessage(message);
			ivd.setInvitationDeducted(invitationDeducted);
			ivd.setDeleted(false);
			// this must be a repeat invite
			ivd.setInitialInvite(false);
		}
		// extend expiration period if not expired; if it
		// is expired, create a new invitation token and move over
		// all the inveterData
		if (!iv.isExpired()) {	
			iv.setCreationDate(new Date(currentTime));
		} else {		
		    // create a new auth key that isn't expired,
		    // preserving current inviter list etc.
		    iv = new InvitationToken(iv, ivd);
			em.persist(iv);			
			// for all inviters other than the current inviter, 
			// unset invitationDeducted
			// even if they did spend an invitation voucher on this
			// invitation in the past, they allowed the invitation
			// to expire, so they should not be reimbursed if they
			// choose to delete this invitation
			for (InviterData inviterData : iv.getInviters()) {
				if (!inviterData.getInviter().equals(inviter)) {
					inviterData.setInvitationDeducted(false);
				}
			}			
		}
		return iv;
	}
	
	private String sendInvitation(final UserViewpoint viewpoint, final PromotionCode promotionCode, 
			                      final Resource invitee, final String subject, final String message) {
		User inviter = viewpoint.getViewer();
		Pair<CreateInvitationResult,InvitationToken> p = createInvitation(inviter, promotionCode, invitee, subject, message);
		CreateInvitationResult result = p.getFirst();
		final InvitationToken iv = p.getSecond();
		String note = null;
		if (result == CreateInvitationResult.INVITE_WAS_NOT_CREATED) {
			return "Your invitation was not sent because you are out of invitation vouchers."; 
		} else if (result == CreateInvitationResult.ALREADY_HAS_ACCOUNT) {
			User user = spider.lookupUserByResource(viewpoint, invitee);
			return invitee.getHumanReadableString() + " already has an account '" + user.getNickname() + "', now added to your friends list.";
		} else { 
			// note should be null or contain INVITATION_SUCCESS_STRING to indicate a successful invitation
			if (result == CreateInvitationResult.REPEAT_INVITE) {
				note = INVITATION_SUCCESS_STRING + ", another invitation was sent to " + invitee.getHumanReadableString() + ".";				
			} else if (result == CreateInvitationResult.NEW_INVITER) {
				note = INVITATION_SUCCESS_STRING + ", an invitation was sent to " + invitee.getHumanReadableString() + "."
				       + " You didn't have to spend an invitation because they were already invited by someone else."; 			
			} else if (result == CreateInvitationResult.INVITE_CREATED) {
				note = INVITATION_SUCCESS_STRING + ", an invitation was sent to " + invitee.getHumanReadableString() + ".";
			} else {
				// unknown case
				return "Your invitation was not sent.";
			}
			
			// In all the three of the above cases, we want to send a notification		
			if (invitee instanceof EmailResource) {
				sendEmailNotification(viewpoint, iv, subject, message);			
			} else {
				throw new RuntimeException("no way to send this invite! unhandled resource type " + invitee.getClass().getName());
			}
			return note;
		}
	}

	public String sendEmailInvitation(UserViewpoint viewpoint, PromotionCode promotionCode, String email, String subject, String message) throws ValidationException {
		Resource emailRes = spider.getEmail(email);
		return sendInvitation(viewpoint, promotionCode, emailRes, subject, message);
	}
	

	protected void notifyInvitationViewed(InvitationToken invite) {
		// adding @suppresswarnings here makes javac crash, whee
		//for (InviterData inviterData : invite.getInviters()) {
		// TODO send notification via xmpp to inviter that invitee viewed
		//}
	}
	
	public Client viewInvitation(InvitationToken invite, String firstClientName, boolean disable) {
		if (invite.isViewed()) {
			throw new IllegalArgumentException("InvitationToken " + invite + " has already been viewed");
		}

		EmailResource invitationResource = (EmailResource) invite.getInvitee();
		
		Account acct = accounts.createAccountFromResource(invitationResource);
		if (disable) {
			identitySpider.setAccountDisabled(acct.getOwner(), true);
		}

		Client client = null;
		if (firstClientName != null) {
			client = accounts.authorizeNewClient(acct, firstClientName);
		}

		User newUser = acct.getOwner();
		
		invite.setViewed(true);
		invite.setResultingPerson(newUser);
		
		String specialCountString = configuration.getProperty(HippoProperty.SPECIAL_NEW_USER_INVITATION_COUNT);
		int specialCount = Integer.parseInt(specialCountString);
		String regularCountString = configuration.getProperty(HippoProperty.NEW_USER_INVITATION_COUNT);
		int regularCount = Integer.parseInt(regularCountString);
		
		if (invite.getPromotionCode() == PromotionCode.MUSIC_INVITE_PAGE_200602) {
			// we aren't really using this promotion for now, so set the regular number of invitations
			acct.setInvitations(regularCount);
			// you are already implicitly wanting this if you came via the music thing;
			// people can always turn it off
			acct.setMusicSharingEnabled(true);
		} else if (invite.getPromotionCode() == PromotionCode.SUMMIT_LANDING_200606) {
			// we aren't really using this promotion for now, so set the regular number of invitations
			acct.setInvitations(regularCount);			
			// current default for enabling music sharing should apply to summit people
			// we also want summit people to be invited to the common group
			String groupGuidString = null;
			try {
			    groupGuidString = configuration.getPropertyNoDefault(HippoProperty.SUMMIT_GROUP_GUID);
			    Group summitGroup = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), groupGuidString);
			    // invite has a List of inviters, let's rather use the Mugshot character explicitly here,
			    // it is important that Mugshot is already a member of a group we are inviting to,
			    // which it already is for the summit group
			    groupSystem.addMember(accounts.getCharacter(Character.MUGSHOT), summitGroup, newUser);
			} catch (PropertyNotFoundException e) {
				logger.error("Summit Group guid not found, exception: {}", e.getMessage());				
		    } catch (NotFoundException e) {
				logger.error("Summit Group not found, guid: {}, exception: {}", groupGuidString, e.getMessage());
			}
		} else if (wantsInSystem.isWantsIn(invitationResource.getEmail())) {
			// This will give invitations to someone whose e-mail is in the wants in list, 
			// even if it wasn't us who invited them in. This seems reasonable, as having
			// "signed up" should give a person a special status. If we want to only give 
			// invites when it was us who invited the person, we either can check the inviter
			// is Mugshot or change the behavior of wantsInSystem.isWantsIn() to check that  
			// we have marked invitationSent for the e-mail as "true", which means it was us
			// who sent the invitation.
			acct.setInvitations(specialCount);			
		} else {
			acct.setInvitations(regularCount);
		}
				
		// needed to fix newUser.getAccount() returning null inside identitySpider?
		em.flush();
		
		// add all inviters as our contacts
		for (InviterData inviterData : invite.getInviters()) {
			Account inviterAccount = inviterData.getInviter().getAccount();
			spider.createContact(newUser, inviterAccount);
		}
		
		if (!disable)
			notifyInvitationViewed(invite);
		
		return client;
	}

	/*
	public Collection<String> getInviterNames(InvitationToken invite) {
		Set<String> names = new HashSet<String>();  
		for (InviterData inviterData : invite.getInviters()) {
			PersonView view = personViewer.getSystemView(inviterData.getInviter());
	        String readable = view.getName();
	        if (readable != null) {    
	        	names.add(readable);
	        }
		}
		return Collections.unmodifiableCollection(names);
	}
	*/
	
	/**
	 * Returns InviterData if there is one that corresponds to the given user
	 * for the given invitation or null.
	 * 
	 * @param invite the invitation
	 * @param inviter the inviter
	 * @return InviterData if there is one that corresponds to the given user
	 * for the given invitation or null
	 */
    private InviterData getInviterData(InvitationToken invite, User inviter) {
		for (InviterData inviterData : invite.getInviters()) {
			if (inviterData.getInviter().equals(inviter)) {
				return inviterData;
			}
		}
        return null;	  	
    }
	
	public InvitationToken getCreatingInvitation(Account account) {
		try {
			return (InvitationToken)em.createQuery("SELECT it FROM InvitationToken it WHERE it.resultingPerson = :owner")
				.setParameter("owner", account.getOwner())
				.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public int getInvitations(User user) {
		Account account = getAccount(user);
		return account.getInvitations();
	}

	public boolean hasInvited(UserViewpoint viewpoint, Resource invitee) {
		User user = viewpoint.getViewer();
		
		// iv will be null if user is not among the inviters
		InvitationToken iv = lookupInvitationFor(user, invitee);
		
		if (iv == null)
			return false;
		
		InviterData ivd = getInviterData(iv, user);
		
		// ivd should not be null because we just got back an iv because the
		// user was among the inviters, but just in case check if ivd != null
		if (ivd != null && iv.isValid() && !ivd.isDeleted())
			return true;
		else 
			return false;
	}
	
	public int getSystemInvitationCount(UserViewpoint viewpoint) {
		if (!spider.isAdministrator(viewpoint.getViewer()))
			throw new RuntimeException("can't do this if you aren't an admin");
		Set<User> already = new HashSet<User>();
		int count = 0;
		for (Character c : Character.values()) {
			// the character enum has the same user more than once
			User u = accounts.getCharacter(c);
			if (already.contains(u))
				continue;
			already.add(u);
			count += u.getAccount().getInvitations();
		}
		return count;
	}

	public int getTotalInvitationCount(UserViewpoint viewpoint) {
		if (!spider.isAdministrator(viewpoint.getViewer()))
			throw new RuntimeException("can't do this if you aren't an admin");
		Query q = em.createQuery("SELECT SUM(a.invitations) FROM Account a");
		return ((Number) q.getSingleResult()).intValue();
	}
	
	public int getSelfInvitationCount() {
		return getInvitations(accounts.getCharacter(Character.MUGSHOT));
	}
	
	private void sendEmailNotification(UserViewpoint viewpoint, InvitationToken invite, String subject, String message) {
		User inviter = viewpoint.getViewer();
		EmailResource invitee = (EmailResource) invite.getInvitee();
		
		if (!noMail.getMailEnabled(invitee)) {
			logger.debug("Mail is disabled to {} not sending invitation", invitee);
			return;
		}
		
		String inviteeEmail = invitee.getEmail();
		
        // if invite is from a special character, like Mugshot, this function will get the character's viewpoint,
		// which is fine
		MimeMessage msg = mailer.createMessage(Mailer.SpecialSender.INVITATION, viewpoint, Mailer.SpecialSender.INVITATION, inviteeEmail);

		PersonView viewedInviter = personViewer.getPersonView(viewpoint, inviter);
		String inviterName = viewedInviter.getName();
		
		String baseurl;
		URL baseurlObject;
		try {
			baseurl = configuration.getProperty(HippoProperty.BASEURL);
			baseurlObject = new URL(baseurl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		User mugshot = accounts.getCharacter(Character.MUGSHOT);
		boolean isMugshotInvite = (viewedInviter.getUser() == mugshot);
		
		if (subject == null || subject.trim().length() == 0) {
			if (isMugshotInvite)
				subject = "Your Mugshot Invitation";
			else
				subject = "Invitation from " + inviterName + " to join Mugshot";				
		}
		
		StringBuilder messageText = new StringBuilder();
		XmlBuilder messageHtml = new XmlBuilder();
		
		messageHtml.appendHtmlHead("");
		messageHtml.append("<body>\n");

		if (message != null && message.trim().length() > 0) {
			messageHtml.append("<div style=\"padding: 1.5em;\">\n");
			messageHtml.appendTextAsHtml(message, null);
			messageHtml.append("</div>\n");
			
			messageText.append(message);
			messageText.append("\n\n");
		}
		
		String inviteUrl = invite.getAuthURL(baseurlObject);
		// Only set the inviter for non-mugshot invitations; the download
		// page assumes the absence of this parameter implies the invitation
		// was from mugshot, which we handle specially.
		if (!isMugshotInvite) {
			inviteUrl += "&inviter=";
			inviteUrl += inviter.getId();
		}
		
		messageHtml.append("<div style=\"padding: 1em;\">");
		messageHtml.appendTextNode("a", inviteUrl, "href", inviteUrl);
		messageHtml.append("</div>\n");
		
		messageText.append(inviteUrl);
		messageText.append("\n\n");
		
		String noSpamDisclaimer = "If you got this by mistake, just ignore it.  We won't send you email again unless you ask us to.  Thanks!";
		
		messageHtml.appendTextNode("div", noSpamDisclaimer);
		messageText.append(noSpamDisclaimer);
		
		messageHtml.append("</body>\n</html>\n");
		
		mailer.setMessageContent(msg, subject, messageText.toString(), messageHtml.toString());
		
		final MimeMessage finalizedMessage = msg;
		
		runner.runTaskOnTransactionCommit(new Runnable() {
			public void run() {
		        mailer.sendMessage(finalizedMessage);
			}
		});
	}		
}
