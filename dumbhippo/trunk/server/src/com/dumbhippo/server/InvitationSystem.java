package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.views.InvitationView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface InvitationSystem {
	
	static public final String INVITATION_SUCCESS_STRING = new String("Congratulations");
		
	public InvitationToken lookupInvitation(UserViewpoint viewpoint, long id);
	
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
	public InvitationView lookupInvitationViewFor(UserViewpoint viewpoint, Resource invitee);
	
	/**
	 * Find all inviters for resources provably owned by the current users
	 * Will return inviters whose invitations are expired, but will not return
	 * inviters who deleted their invitation.
	 * @param viewpoint The current user (the person that was invited)
	 * @param extras info to stuff in the PersonView objects
	 * @return a set of all the inviters for the invitee; the
	 *         resulting PersonView use invitee as the viewpoint.
	 */
	// Not used at the moment
	// public Set<PersonView> findInviters(UserViewpoint viewpoint, PersonViewExtra... extras);
	
	
	/**
	 * Find a selection of current invitations sent by the viewer.
	 * 
	 * @param viewpoint a person who is viewing invitations they sent
	 * @param start invitation to start with
	 * @param max maximum number of invitations to get
	 * @return a list of InvitationViews that correspond to outstanding invitations
	 * sent by the inviter
	 */
	public List<InvitationView> findOutstandingInvitations(UserViewpoint viewpoint, 
			                                               int start, 
			                                               int max);
	
	/**
	 * Count all current invitations sent by the viewer.
	 * 
	 * @param inviter a person that has been sending invitations
	 * @return the number of outstanding invitations sent by the inviter
	 */
	public int countOutstandingInvitations(UserViewpoint viewpoint);
	
	/**
	 * Deletes an invitation created by a given viewer with a given authentication key.
	 * 
	 * @param viewpoint viewer of the invitation to be deleted, must be the person who 
	 *        created the invitation
	 * @param authKey authentication key for the invitation to be deleted
	 * @return deleted invitation or null
	 */
	public InvitationView deleteInvitation(UserViewpoint viewpoint, String authKey);

	/**
	 * Restores an invitation created by a given viewer with a given authentication key.
	 * 
	 * @param viewpoint viewer of the invitation to be restored, must be the person who 
	 *        created the invitation
	 * @param authKey authentication key for the invitation to be restored
	 */
	public void restoreInvitation(UserViewpoint viewpoint, String authKey);
	
	/**
	 * If invitee has already been invited and the invitation is not expired,
	 * gets the invitation. addToInvitation enables "piling on" to an existing
	 * invitation, but the invitation will be returned even if addToinvitation is false, 
	 * so that we can use the invitation URL in a context like an email notification.
	 * 
	 * @param inviter possible inviter
	 * @param invitee possible invitee
	 * @param addToInvitiation if true true, and the inviter is not already part
	 *   of the invitater set, the inviter will be added and the invitation
	 *   date updated. 
	 * @return invitation if any, or null if no valid invitation exists
	 */
	public InvitationToken updateValidInvitation(User inviter, Resource invitee, boolean addToInvitation);
	
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
	 * Adds the current user as a person wanting to invite invitee into the system.
	 * Adds the invitee as a contact if they weren't already. Sends out
	 * the invitation to the resource if appropriate. This is an "explicit 
	 * invitation".
	 * 
	 * @param viewpoint the current user (the person doing the inviting)
	 * @param promotionCode code of a promotion (like a web page offering open 
	 *    invites) that the invitation comes from, or null.
	 * @param email
	 * @param subject subject for the email, text format
	 * @param message message to send (from the inviter to invitee), text format
	 * @throws ValidationException if email address is bogus 
	 * @returns note that will be displayed to the user or null
	 */
	public String sendEmailInvitation(UserViewpoint viewpoint, PromotionCode promotionCode, String email, 
			                          String subject, String message) throws ValidationException;
	
	/**
	 * Mark an invitation as viewed; this creates an initial Account
	 * for the user and such, and grants the client access to the account
	 * by adding a Client object. If firstClientName is null no client 
	 * is added.
	 * 
	 * @param invite the invitation
	 * @param firstClientName name of the first client to create
	 * @param disable true if the user wants to disable the account
	 * @return initial client authorized to access the account
	 */
	public Client viewInvitation(InvitationToken invite, String firstClientName, boolean disable);
	
	/**
	 * Return the names (from the system viewpoint) of the inviting
	 * people for an invitation.
	 * 
	 * @param invite an invitation
	 * @return a collection of names
	 */
	// Not used right now
	// public Collection<String> getInviterNames(InvitationToken invite);
	
	/**
	 * Return number of invitations the user has left to send.
	 * @param viewpoint viewing person
	 * @param user user
	 * @return number of invitations
	 */
	public int getInvitations(Viewpoint viewpoint, User user);
	
	
	/**
	 * Get the invite that resulted in the account being created.
	 * @return the invite, or null if the account wasn't created from an invitation
	 */
	public InvitationToken getCreatingInvitation(Account account);
	
	/** 
	 * See if user has invited the resource and has not deleted their invitation.
	 * @param viewpoint the current user
	 * @param invitee invitee resource
	 * @return true if this user has invited this invitee and has not deleted 
	 *         their invitation
	 */
	public boolean hasInvited(UserViewpoint viewpoint, Resource invitee);
	
	/**
	 * Get the number of invitations held by "system" users, these are 
	 * normally "public" or "up for grabs"
	 *  
	 * @param viewpoint the logged-in user, must be an admin
	 * @return unclaimed invite count
	 */
	public int getSystemInvitationCount(UserViewpoint viewpoint);
	
	/**
	 * Get the number of unused invitations held by any user, includes
	 * the count from getSystemInvitationCount().
	 *  
	 * @param viewpoint the logged-in user, must be an admin
	 * @return unclaimed invite count
	 */
	public int getTotalInvitationCount(UserViewpoint viewpoint);

	/**
	 * Get the number of self invitations that can be used for open sign up. 
	 * 
	 * @return count of self invitations available
	 */
    public int getSelfInvitationCount();
}
