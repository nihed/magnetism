package com.dumbhippo.server;

import java.util.Collection;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.Pair;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;

@Local
public interface InvitationSystem {
		
	/**
	 * Find all inviters for resources provably owned by a person.
	 * 
	 * @param invitee The person that was invited
	 * @param extras info to stuff in the PersonView objects
	 * @return a set of all the inviters for the invitee; the
	 *   resulting PersonView use invitee as the viewpoint.
	 */
	public Set<PersonView> findInviters(User invitee, PersonViewExtra... extras);
	
	/**
	 * Add inviter as a person wanting to invite the owner
	 * of an email address.  Adds invitee as contact of inviter 
	 * if they aren't already; sends out the invite. Doesn't do 
	 * anything if the inviter has already invited the invitee.
	 * @see createGetInvitation
	 * 
	 * @param inviter
	 * @param email
	 * @returns note for the user or null
	 */
	public String sendEmailInvitation(User inviter, String email);
	
	/**
	 * Add inviter as a person wanting to invite invitee into the system.
	 * Adds the invitee as a contact if they weren't already. Sends out
	 * the invitation to the resource if needed.
	 * 
	 * @param inviter the person doing the inviting
	 * @param invitee the person being invited
	 * @returns note for the user or null
	 */
	public String sendInvitation(User inviter, Resource invitee);
	
	
	/**
	 * If invitee has already been invited, ensures inviter is 
	 * in the inviter set and returns the invitation url.
	 * Else returns null.
	 * 
	 * @param inviter possible inviter
	 * @param invitee possible invitee
	 * @return invitation url if any, or null
	 */
	public String getInvitationUrl(User inviter, Resource invitee);
	
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
	public Pair<Client,User> viewInvitation(InvitationToken invite, String firstClientName, boolean disable);
	
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
	 * See if user has invited the resource.
	 * @param user the user
	 * @param invitee invitee resource
	 * @return true if this user has invited this invitee
	 */
	public boolean hasInvited(User user, Resource invitee);
}
