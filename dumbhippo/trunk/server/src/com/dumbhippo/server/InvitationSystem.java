package com.dumbhippo.server;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@Local
public interface InvitationSystem {
		
	/**
	 * The inviter argument is optional and means we only return an invitation token if this inviter is
	 * among the inviters. We return an invitation even if it was deleted or has expired and it is up to 
	 * the caller to sort that out.
	 * 
	 * @param inviter only return non-null if this inviter is already in the inviters; null to always return invitation
	 * @param invitee the invitee
	 * @return invitation token or null
	 */
	public InvitationToken lookupInvitationFor(User inviter, Resource invitee);

	/**
	 * Return an invitation token with a given authentication key if it has a 
	 * given inviter.
	 *  
	 * @param inviter the inviter
	 * @param invitee the authentication key of the invitation token
	 * @return invitation token or null
	 */
	public InvitationToken lookupInvitation(User inviter, String authKey); 
	
	/**
	 * Returns an InvitationView of an invite sent by the viewer to the invitee, or
	 * null if there was no such invite. Returns an invitation even if it was deleted 
	 * and it is up to the caller to sort that out.
	 * 
	 * @param viewpoint the person viewing an invitation for which they must be an inviter
	 * @param invitee the invitee
	 * @return invitation view or null
	 */
	public InvitationView lookupInvitationViewFor(Viewpoint viewpoint, Resource invitee);
	
	/**
	 * Find all inviters for resources provably owned by a person.
	 * Will return inviters whose invitations are expired, but will not return
	 * inviters who deleted their invitation.
	 * @param invitee The person that was invited
	 * @param extras info to stuff in the PersonView objects
	 * @return a set of all the inviters for the invitee; the
	 *         resulting PersonView use invitee as the viewpoint.
	 */
	public Set<PersonView> findInviters(User invitee, PersonViewExtra... extras);
	
	
	/**
	 * Find a selection of current invitations sent by the viewer.
	 * 
	 * @param viewpoint a person who is viewing invitations they sent
	 * @param start invitation to start with
	 * @param max maximum number of invitations to get
	 * @return a list of InvitationViews that correspond to outstanding invitations
	 * sent by the inviter
	 */
	public List<InvitationView> findOutstandingInvitations(Viewpoint viewpoint, 
			                                               int start, 
			                                               int max);
	
	/**
	 * Count all current invitations sent by the viewer.
	 * 
	 * @param inviter a person that has been sending invitations
	 * @return the number of outstanding invitations sent by the inviter
	 */
	public int countOutstandingInvitations(Viewpoint viewpoint);
	
	/**
	 * Deletes an invitation created by a given viewer with a given authentication key.
	 * 
	 * @param viewpoint viewer of the invitation to be deleted, must be the person who 
	 *        created the invitation
	 * @param authKey authentication key for the invitation to be deleted
	 * @return deleted invitation or null
	 */
	public InvitationView deleteInvitation(Viewpoint viewpoint, String authKey);

	/**
	 * Restores an invitation created by a given viewer with a given authentication key.
	 * 
	 * @param viewpoint viewer of the invitation to be restored, must be the person who 
	 *        created the invitation
	 * @param authKey authentication key for the invitation to be restored
	 */
	public void restoreInvitation(Viewpoint viewpoint, String authKey);
	
	/**
	 * If invitee has already been invited and the invitation is not expired, 
	 * ensures inviter is in the inviter set and returns the invitation. 
	 * If the inviter is new to the inviter set, the invitation date is updated.
	 * Else returns null.
	 * 
	 * @param inviter possible inviter
	 * @param invitee possible invitee
	 * @return invitation if any, or null
	 */
	public InvitationToken updateValidInvitation(User inviter, Resource invitee);
	
	/**
	 * Ensure the invitation from inviter to invitee exists, if it makes sense. 
	 * Returns the current status of that invitation, and the created invitation token 
	 * if any (this will be null if none was created or exists).
	 * Does not send out any invitation emails or anything.
	 * 
	 * Use this for "implicit invitation" when sharing something with someone.
	 * 
	 * @param inviter the inviter
	 * @param promotionCode code of a promotion (like a web page offering open 
	 *    invites) that the invitation comes from, or null.
	 * @param invitee who to invite
	 * @param subject subject for the email, text format
	 * @param message message to send (from the inviter to invitee), text format
	 * @return the outcome
	 */
	public Pair<CreateInvitationResult,InvitationToken> 
	    createInvitation(User inviter, PromotionCode promotionCode, Resource invitee,
			             String subject, String message);
	
	/**
	 * Add inviter as a person wanting to invite invitee into the system.
	 * Adds the invitee as a contact if they weren't already. Sends out
	 * the invitation to the resource if needed. This is an "explicit invitation"
	 * 
	 * @param inviter the person doing the inviting
	 * @param promotionCode code of a promotion (like a web page offering open 
	 *    invites) that the invitation comes from, or null.
	 * @param invitee the person being invited
	 * @param subject subject for the email, text format
	 * @param message message to send (from the inviter to invitee), text format
	 * @returns note for the user or null
	 */
	public String sendInvitation(User inviter, PromotionCode promotionCode, Resource invitee, 
			                     String subject, String message);
	
	
	/**
	 * Does an "explicit invitation"
	 * Add inviter as a person wanting to invite the owner
	 * of an email address.  Adds invitee as contact of inviter 
	 * if they aren't already; sends out the invite via email. Doesn't do 
	 * anything if the inviter has already invited the invitee.
	 * @see createGetInvitation
	 * 
	 * @param inviter
	 * @param promotionCode code of a promotion (like a web page offering open 
	 *    invites) that the invitation comes from, or null.
	 * @param email
	 * @param subject subject for the email, text format
	 * @param message message to send (from the inviter to invitee), text format
	 * @returns note for the user or null
	 */
	public String sendEmailInvitation(User inviter, PromotionCode promotionCode, String email, 
			                          String subject, String message);
	
	/**
	 * Mark an invitation as viewed; this creates an initial Account
	 * for the user and such, and grants the client access to the account
	 * by adding a Client object. If firstClientName is null no client 
	 * is added.
	 * 
	 * @param invite the invitation
	 * @param firstClientName name of the first client to create
	 * @param disable true if the user wants to disable the account
	 * @return initial client authorized to access the account and the resulting person from the invite
	 */
	public Pair<Client,User> 
	    viewInvitation(InvitationToken invite, String firstClientName, 
	    		       boolean disable);
	
	/**
	 * Return the names (from the system viewpoint) of the inviting
	 * people for an invitation.
	 * 
	 * @param invite an invitation
	 * @return a collection of names
	 */
	public Collection<String> getInviterNames(InvitationToken invite);
	
	/**
	 * Return number of invitations the user has left to send.
	 * @param user user
	 * @return number of invitations
	 */
	public int getInvitations(User user);
	
	
	/**
	 * Get the invite that resulted in the account being created.
	 * @return the invite, or null if the account wasn't created from an invitation
	 */
	public InvitationToken getCreatingInvitation(Account account);
	
	/** 
	 * See if user has invited the resource.
	 * @param user the user
	 * @param invitee invitee resource
	 * @return true if this user has invited this invitee
	 */
	public boolean hasInvited(User user, Resource invitee);
}
