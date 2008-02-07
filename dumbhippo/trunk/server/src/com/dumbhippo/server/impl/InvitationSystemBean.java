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
import com.dumbhippo.Site;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.email.MessageContent;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountType;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.InviterData;
import com.dumbhippo.persistence.LoginToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Token;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.CreateInvitationResult;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.InvitationSystemRemote;
import com.dumbhippo.server.LoginVerifier;
import com.dumbhippo.server.Mailer;
import com.dumbhippo.server.NoMailSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.WantsInSystem;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.InvitationView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.tx.TxUtils;

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
	
	@EJB
	private LoginVerifier loginVerifier;

	private InvitationToken lookupInvitationFor(Resource invitee) {
		List<InvitationToken> invites = lookupInvitationsFor(invitee, true);
		if (invites.size() > 0)
			return invites.get(0);
		else
	        return null;
    }
	
	private List<InvitationToken> lookupInvitationsFor(Resource invitee, boolean retrieveLatestOnly) {
		InvitationToken invite;			
		
		// we get the newest invitation token, sort by date in descending order
	    Query q = em.createQuery(
		              "SELECT iv FROM InvitationToken iv WHERE iv.invitee = :resource ORDER BY iv.creationDate DESC");
		q.setParameter("resource", invitee);
			
		if (retrieveLatestOnly) {
			try {
			    q.setMaxResults(1); // only need the first one
			    invite = (InvitationToken) q.getSingleResult();
			    List<InvitationToken> invites = new ArrayList<InvitationToken>();
			    invites.add(invite);
			    return invites;
		    } catch (NoResultException e) {
			    return new ArrayList<InvitationToken>();
		    }
		} else {    
			return TypeUtils.castList(InvitationToken.class, q.getResultList());
		}
	}

	private InviterData lookupInviterData(UserViewpoint inviter, Resource invitee) {
		InviterData inviterData;
		try {
			// we get the newest invitation token, sort by date in descending order
			Query q = em.createQuery(
				"SELECT ivd FROM InviterData ivd " +
				"  WHERE ivd.inviter = :inviter " +
				"    AND ivd.invitation.invitee = :resource " +
				"  ORDER BY ivd.invitation.creationDate DESC");
			q.setParameter("resource", invitee);
			q.setParameter("inviter", inviter.getViewer());
			q.setMaxResults(1); // only need the first one
			inviterData = (InviterData) q.getSingleResult();
			
		} catch (NoResultException e) {
			inviterData = null;
		}
		return inviterData;
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
				"SELECT ivd.invitation FROM InviterData ivd " +
				"    WHERE ivd.invitation.authKey = :authKey AND " + 
				"          ivd.invitation = iv AND " +
				"          ivd.inviter = :inviter");		
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
		InviterData ivd = lookupInviterData(viewpoint, invitee);
		
		if (ivd == null)
			return null;
		
		Set<Group> suggestedGroups = 
			groupSystem.getInvitedToGroups(viewpoint, invitee);
		
        InvitationView invitationView = new InvitationView(ivd.getInvitation(), ivd, suggestedGroups);
        
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
								     "WHERE inviterData.invitation = invite AND " +
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

	public List<InvitationView> findInvitations(UserViewpoint viewpoint, 
			                                    int start, int max, boolean includeExpired) {	
		// we want to provide the invitations for which the person viewing the 
		// invitations is the inviter
		User inviter = viewpoint.getViewer();
		// get only the InvitationTokens for which ResultingPerson is null,
		// sorted by date in descending order, filter out older invitations 
		// to the same recipient
		// Sometimes we have invitations that do not have a resulting person
		// set, even though there is a user who is claiming the invitee resource
		// in the system. We only set resulting person when a particular invitation
		// token is viewed (getResultingPerson on InvitationToken is OneToOne). 
		// So there might be other (expired) invitations that still have resultingPerson
		// set to null. A user might also bypass viewing an invitation if they already
		// have an account and add the e-mail address an invitation was sent to to
		// that account.
		Query q = em.createQuery(
			"SELECT ivd.invitation FROM InviterData ivd " +
			"    WHERE ivd.inviter = :inviter AND " +
			"       ivd.deleted = FALSE AND " +
			"       ivd.invitation.resultingPerson IS NULL AND " +
			"       NOT EXISTS (SELECT it.id FROM InvitationToken it, InviterData ivd2 " +
			"                   WHERE it.invitee = ivd.invitation.invitee AND" +
			"                         ivd2.inviter = :inviter AND" +
			"                         it.creationDate > ivd.invitation.creationDate AND" +
			"                         ivd2.invitation = it) AND " +		
			"       NOT EXISTS (SELECT ac.id from AccountClaim ac where ac.resource = ivd.invitation.invitee)" +
			"    ORDER BY ivd.invitation.creationDate DESC");
		
		q.setParameter("inviter", inviter);
		
		if (max > 0)
			q.setMaxResults(max);
		
		q.setFirstResult(start);
		
		@SuppressWarnings("unchecked")
		List<InvitationToken> invitations = q.getResultList();
		
		List<InvitationView> invitationViews = new ArrayList<InvitationView> ();
		
		// can we mix accessing data through the database with accessing it through 
		// the persistence classes? should this also be a database query?
		for (InvitationToken invite : invitations) {
			if (!includeExpired && invite.isExpired()) {
				// deleted invitations should have been filtered out by the query,
				// but we also want to filter out expired invitations
				continue;
			}			
			InviterData inviterData = getInviterData(invite, inviter);
			// inviterData should not come back null, because we just obtained all 
			// the invitations sent out by this particular inviter, but if it
			// is null, it's ok to pass it to the invitationView constructor too
			Set<Group> suggestedGroups = 
				groupSystem.getInvitedToGroups(viewpoint, invite.getInvitee());
            InvitationView invitationView = new InvitationView(invite, inviterData, suggestedGroups);
            invitationViews.add(invitationView);
		}
			
		return invitationViews; 
	}

	public void deleteInvitations(UserViewpoint viewpoint, Resource resource) {
		User inviter = viewpoint.getViewer();
		for (InvitationToken invite : lookupInvitationsFor(resource, false)) {
			deleteInvitation(inviter, invite);
		}
	}
	
	public InvitationView deleteInvitation(UserViewpoint viewpoint, String authKey) {
		User inviter = viewpoint.getViewer();		
		InvitationToken invite = lookupInvitation(inviter, authKey);
	
		InviterData ivd = deleteInvitation(inviter, invite); 
		if (ivd != null) {
		    Set<Group> suggestedGroups = 
		   	    groupSystem.getInvitedToGroups(viewpoint, invite.getInvitee());
            InvitationView invitationView = new InvitationView(invite, ivd, suggestedGroups);
            return invitationView;	
		}
		
		return null;
	}
		
	private InviterData deleteInvitation(User inviter, InvitationToken invite) {	
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
        if (ivd.isInvitationDeducted() && !invite.isExpired()) {
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
		
        return ivd;	
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
		InvitationToken iv = lookupInvitationFor(invitee);
		if (iv == null || !iv.isValid())
			return null;
		if (addToInvitation && getInviterData(iv, inviter) == null) {
			// it's a new inviter for this invitation, so we can add him to
			// the inviters list and update the invitation date
			long currentTime = System.currentTimeMillis();
			InviterData ivd = 
				new InviterData(inviter, iv, currentTime, 
						        "You joined an existing invitation.", 
						        "", false);
			em.persist(ivd);
			iv.addInviter(ivd);
			iv.setCreationDate(new Date(currentTime));
		}
		return iv;
	}
	
	public Pair<CreateInvitationResult,Token> 
	    createInvitation(User inviter, PromotionCode promotionCode, Resource invitee, 
	    		         String subject, String message) {
		// be sure the invitee is our contact (even if we 
		// end up not sending the invite)
		spider.createContact(inviter, invitee);
		// this also catches inviting yourself and keeps us from 
		// sending mail to disabled accounts 
		User user = spider.lookupUserByResource(SystemViewpoint.getInstance(), invitee);
		if (user != null) {
			if (user.getAccount().isActive() && user.getAccount().isPublicPage()) {
			    logger.debug("not inviting '{}' due to existing user {}", invitee, user);
			    return new Pair<CreateInvitationResult,Token>(CreateInvitationResult.ALREADY_HAS_ACCOUNT, null);
			} else {
			    logger.debug("not inviting '{}' due to existing user {}, but sending them a login link since their account is disabled", invitee, user);
			    try {
			        return new Pair<CreateInvitationResult,Token>(CreateInvitationResult.ALREADY_HAS_INACTIVE_ACCOUNT, loginVerifier.getLoginToken(invitee));				
			    } catch (HumanVisibleException e) {
			    	throw new RuntimeException("could not create a login link for " + invitee, e);			    					
			    } catch (RetryException e) {
			    	throw new RuntimeException("could not create a login link for " + invitee, e);			    							    	
			    }
			}
		}

		CreateInvitationResult result = CreateInvitationResult.REPEAT_INVITE;
		InvitationToken iv = lookupInvitationFor(invitee);
		if (iv == null || !iv.isValid()) {	
			Account account = getAccount(inviter);
			// first things first, do they have the dough
			if (account.canSendInvitations(1)) {
				account.deductInvitations(1);
			} else {
				result = CreateInvitationResult.INVITE_WAS_NOT_CREATED;
				return new Pair<CreateInvitationResult,Token>(result, null);
			}
			
			// renewing an expiration counts as creating (causes us to send new email)
			result = CreateInvitationResult.INVITE_CREATED; 
			if (iv != null) {	
				iv = updateInvitation(iv, inviter, subject, message, true);				
			} else {		
				iv = new InvitationToken(invitee);
				iv.setPromotionCode(promotionCode);
				em.persist(iv);
				iv.prepareToAddInviter();
				InviterData inviterData = new InviterData(inviter, iv, subject, message, true);
				em.persist(inviterData);
				iv.addInviter(inviterData);
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
	    return new Pair<CreateInvitationResult,Token>(result, iv);
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
            // in some cases, this will add a new inviter to an expired invitation, 
			// but it's ok
			ivd = new InviterData(inviter, iv, currentTime, subject, message, invitationDeducted);
			em.persist(ivd);
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
		    // create a new auth key that isn't expired, preserving current inviter list
		    InvitationToken newInvitation = new InvitationToken(iv.getInvitee());
			em.persist(newInvitation);			
			newInvitation.prepareToAddInviter();

			for (InviterData inviterData : iv.getInviters()) {
				InviterData newData = new InviterData(newInvitation, inviterData);
				
				if (newData.getInviter().equals(inviter)) {
					newData.setInvitationDeducted(invitationDeducted); // Normally true
					// Make sure that dates agree
					newData.setInvitationDate(newInvitation.getCreationDate());
				} else {
					// for all inviters other than the current inviter, unset invitationDeducted
					// even if they did spend an invitation voucher on this
					// invitation in the past, they allowed the invitation
					// to expire, so they should not be reimbursed if they
					// choose to delete this invitation
					newData.setInvitationDeducted(false);
				}
					
				em.persist(newData);
				newInvitation.addInviter(newData);
			}
		}
			
		return iv;
	}
	
	private String sendInvitation(final UserViewpoint viewpoint, final PromotionCode promotionCode, 
			                      final Resource invitee, final String subject, final String message) {
		User inviter = viewpoint.getViewer();
		Pair<CreateInvitationResult,Token> p = createInvitation(inviter, promotionCode, invitee, subject, message);
		CreateInvitationResult result = p.getFirst();
		final Token token = p.getSecond();
		String note = null;
		User user = null;
		
		if (result == CreateInvitationResult.INVITE_WAS_NOT_CREATED) {
			return "Your invitation was not sent because you are out of invitation vouchers."; 
		} else if (result == CreateInvitationResult.ALREADY_HAS_ACCOUNT) {
			user = spider.lookupUserByResource(viewpoint, invitee);
			String hasAccount = invitee.getHumanReadableString() + " already has an account '" + user.getNickname() + "'";
			if (accounts.isSpecialCharacter(inviter) && viewpoint.getSite().getAccountType() != user.getAccount().getAccountType())
				hasAccount= hasAccount + ". This " + user.getAccount().getAccountType().getName() + " account can be used to log in to " + viewpoint.getSite().getSiteName();
			
			// special character invites you on the /signup page if you have no account
			if (!accounts.isSpecialCharacter(inviter))
				return hasAccount + "(now added to your friends list).";
			else
				return hasAccount + ".";
		} else {
			// note should be null or contain INVITATION_SUCCESS_STRING to indicate a successful invitation
			if (result == CreateInvitationResult.REPEAT_INVITE) {
				note = INVITATION_SUCCESS_STRING + ", another invitation was sent to " + invitee.getHumanReadableString() + ".";				
			} else if (result == CreateInvitationResult.NEW_INVITER || result == CreateInvitationResult.ALREADY_HAS_INACTIVE_ACCOUNT) {
				note = INVITATION_SUCCESS_STRING + ", an invitation was sent to " + invitee.getHumanReadableString() + "."
				       + " You didn't have to spend an invitation because they were already invited by someone else."; 		
				if (result == CreateInvitationResult.ALREADY_HAS_INACTIVE_ACCOUNT)
				    user = spider.lookupUserByResource(viewpoint, invitee);
			} else if (result == CreateInvitationResult.INVITE_CREATED) {
				note = INVITATION_SUCCESS_STRING + ", an invitation was sent to " + invitee.getHumanReadableString() + ".";
			} else {
				// unknown case
				return "Your invitation was not sent.";
			}
			
			// In all the three of the above cases, we want to send a notification		
			if (invitee instanceof EmailResource) {
				sendEmailNotification(viewpoint, token, user, subject, message);			
			} else {
				throw new RuntimeException("no way to send this invite! unhandled resource type " + invitee.getClass().getName());
			}
			return note;
		}
	}

	public String sendEmailInvitation(UserViewpoint viewpoint, PromotionCode promotionCode, String email, String subject, String message) throws ValidationException, RetryException {
		Resource emailRes = spider.getEmail(email);
		return sendInvitation(viewpoint, promotionCode, emailRes, subject, message);
	}
	
	public String sendEmailInvitation(UserViewpoint viewpoint, PromotionCode promotionCode, EmailResource email, String subject, String message) {
		return sendInvitation(viewpoint, promotionCode, email, subject, message);
	}

	protected void notifyInvitationViewed(InvitationToken invite) {
		// adding @suppresswarnings here makes javac crash, whee
		//for (InviterData inviterData : invite.getInviters()) {
		// TODO send notification via xmpp to inviter that invitee viewed
		//}
	}
	
	public Client viewInvitation(Site site, InvitationToken invite, String firstClientName, boolean disable) {
		if (invite.isViewed()) {
			throw new IllegalArgumentException("InvitationToken " + invite + " has already been viewed");
		}

		EmailResource invitationResource = (EmailResource) invite.getInvitee();
		
		Account acct = accounts.createAccountFromResource(invitationResource, site.getAccountType());
		if (disable) {
			identitySpider.setAccountDisabled(acct.getOwner(), site, true);
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
			    groupSystem.addMember(accounts.getSiteCharacter(site), summitGroup, newUser);
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
		
		// add all inviters as our contacts, unless they've deleted the invite
		for (InviterData inviterData : invite.getInviters()) {
			if (!inviterData.isDeleted()) {
			    Account inviterAccount = inviterData.getInviter().getAccount();
			    spider.createContact(newUser, inviterAccount);
			}
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

	public int getInvitations(Viewpoint viewpoint, User user) {
		if (!(viewpoint instanceof SystemViewpoint || viewpoint.isOfUser(user)))
			return 0;
		Account account = getAccount(user);
		return account.getInvitations();
	}

	public boolean hasInvited(UserViewpoint viewpoint, Resource invitee) {
		// ivd will be null if user is not among the inviters
		InviterData ivd = lookupInviterData(viewpoint, invitee);
		
		if (ivd != null && ivd.getInvitation().isValid() && !ivd.isDeleted())
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
		// q.getSingleResult() here could return NULL if the Account table were empty, 
		// but that can't happen since we have to be the admin so we have an account
		return ((Number) q.getSingleResult()).intValue();
	}
	
	public int getSelfInvitationCount(Site site) {
		return getInvitations(SystemViewpoint.getInstance(), accounts.getSiteCharacter(site));
	}
	
	static private class InviteMessageContent extends MessageContent {

		private Site site;
		private User user;
		private String subject;
		private String message;
		private String inviteUrl;
		private String inviterName;
		private String inviterPageUrl;
		
		InviteMessageContent(Site site, User user, String subject, String message, String inviteUrl, String inviterName, String inviterPageUrl) {
			this.site = site;
			this.user = user;
			this.subject = subject;
			this.message = message != null ? message.trim() : "";
			this.inviteUrl = inviteUrl;
			this.inviterName = inviterName;
			this.inviterPageUrl = inviterPageUrl;
		}
		
		@Override
		public String getSubject() {
			return subject;
		}

		@Override
		protected void buildMessage(StringBuilder messageText, XmlBuilder messageHtml) {
			openStandardHtmlTemplate(messageHtml);
			
			appendParagraph(messageText, messageHtml, "Hello -");
			
			String intro;
			if (inviterName != null)
				intro = "This is an invitation to join the " + site.getSiteName() + " service from " + inviterName + ".";
			else
				intro = "This is an invitation to join the " + site.getSiteName() + " service.";
			
			appendParagraph(messageText, messageHtml, intro);
			
			if (inviterName != null && message.length() > 0) {
				appendParagraph(messageText, messageHtml, "Your friend " + inviterName + " says:");
				appendBlockquote(messageText, messageHtml, message);
			}
			
			if (user != null && (user.getAccount().isDisabled() || user.getAccount().isAdminDisabled())) {
				if (user.getAccount().getAccountType() == AccountType.GNOME)
			        appendParagraph(messageText, messageHtml, "Your online.gnome.org account is currently disabled and is not visible publicly." +
			    		                                      " Follow this link if you want to reenable it and start using Mugshot: ");
				else 
					appendParagraph(messageText, messageHtml, "Your Mugshot account is currently disabled and is not visible publicly." +
                                                              " Follow this link if you want to reenable it: ");				
			} else if (user != null && (!user.getAccount().getHasAcceptedTerms())) {
				if (user.getAccount().getAccountType() == AccountType.GNOME)
			        appendParagraph(messageText, messageHtml, "Your online.gnome.org account is currently disabled and is not visible publicly because you did not accept terms of use." +
                                                              " Follow this link if you want to enable it and start using Mugshot: ");	
				else 
			        appendParagraph(messageText, messageHtml, "Your Mugshot account is currently disabled and is not visible publicly because you did not accept terms of use." +
                                                              " Follow this link if you want to enable it: ");						
			} else if (user != null && (!user.getAccount().isPublicPage() && user.getAccount().getAccountType() == AccountType.GNOME)) {
				  appendParagraph(messageText, messageHtml, "You can log in to Mugshot using your online.gnome.org account." +
                                                            " Follow this link to enable your Mugshot account: ");
			} else if (user != null) {
				logger.warn("Sending an e-mail to an invitee who is a Mugshot user {} in an unexpected situation.", user);
				appendParagraph(messageText, messageHtml, "Follow this link to log in to your Mugshot account:");
			} else {
			    appendParagraph(messageText, messageHtml, "Follow this link to get started:");
			}
			
			appendLinkAsBlock(messageText, messageHtml, null, inviteUrl);
			
			if (inviterName != null && inviterPageUrl != null) {
				appendParagraph(messageText, messageHtml, "See " + inviterName + "'s page:");
				appendLinkAsBlock(messageText, messageHtml, null, inviterPageUrl);
			}
			
			appendParagraph(messageText, messageHtml, "Thanks!");
			
			appendParagraph(messageText, messageHtml, "- " + site.getSiteName());
			
			closeStandardHtmlTemplate(messageHtml);
		}
	}
	
	private void sendEmailNotification(UserViewpoint viewpoint, Token invite, User user, String subject, String message) {
		User inviter = viewpoint.getViewer();	
		EmailResource invitee;
		String inviteUrl;
		
		String baseurl;
		URL baseurlObject;
		try {
			baseurl = configuration.getBaseUrl(viewpoint);
			baseurlObject = new URL(baseurl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		
		if (invite instanceof InvitationToken) {
	        invitee = (EmailResource)((InvitationToken)invite).getInvitee();
		    inviteUrl = invite.getAuthURL(baseurlObject);
		} else if (invite instanceof LoginToken) {
			invitee = (EmailResource)((LoginToken)invite).getResource();
			if (user.getAccount().getAccountType() == AccountType.GNOME && !user.getAccount().isActive()) {
				// If invitee's account is not active (i.e. if it is disabled or they have not accepted terms of use,
				// we need to send them to the GNOME Online site first. They can then enable Mugshot from there. 
			    inviteUrl = invite.getAuthURL(configuration.getBaseUrlGnome()) + "&next=account";
			} else {
				// If a GNOME Online user never enabled Mugshot (publicPage == false), we can send them directly to
				// the Mugshot site. We send people to the Mugshot site in all other cases too.
				inviteUrl = invite.getAuthURL(configuration.getBaseUrlMugshot()) + "&next=account";
			}
		} else {
			throw new RuntimeException("Unexpected subclass of Token in sendEmailNotification " + invite.getClass());
		}
		
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
		
		User mugshot = accounts.getSiteCharacter(viewpoint.getSite());
		boolean isMugshotInvite = (viewedInviter.getUser().equals(mugshot));
		
		if (subject == null || subject.trim().length() == 0) {
			if (isMugshotInvite)
				subject = "Your " + viewpoint.getSite().getSiteName() + " Invitation";
			else
				subject = "Invitation from " + inviterName + " to join " + viewpoint.getSite().getSiteName();				
		}
						
		// Only set the inviter for non-mugshot invitations; the download
		// page assumes the absence of this parameter implies the invitation
		// was from mugshot, which we handle specially.
		if (!isMugshotInvite) {
			// this parameter is not used when it is a pasrt of the login link, but we might as well keep it
			inviteUrl += "&inviter=";
			inviteUrl += inviter.getId();
		}
		
		String inviterPageUrl = null;
		if (!isMugshotInvite && viewpoint.getSite() != Site.GNOME)
			inviterPageUrl = baseurl + viewedInviter.getHomeUrl();
		
		mailer.setMessageContent(msg, viewpoint.getSite(),
				new InviteMessageContent(viewpoint.getSite(), user, subject, message, inviteUrl,
						isMugshotInvite ? null : inviterName,
						inviterPageUrl));
		
		final MimeMessage finalizedMessage = msg;
		
		TxUtils.runOnCommit(new Runnable() {
			public void run() {
		        mailer.sendMessage(finalizedMessage);
			}
		});
	}		
}
