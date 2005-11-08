package com.dumbhippo.server;

import java.util.Collection;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;

@Local
public interface InvitationSystem {
	
	/**
	 * Look up an invitation by authentication key
	 * @param authKey potential authentication key
	 * @return the corresponding invitation, or null if none
	 */
	public InvitationToken lookupInvitationByKey(String authKey);
	
	/**
	 * Find all inviters for resources provably owned by a person.
	 * 
	 * @param invitee The person that was invited
	 * @return a set of all the inviters for the invitee; the
	 *   resulting PersonView use invitee as the viewpoint.
	 */
	public Set<PersonView> findInviters(Person invitee);
	
	/**
	 * Add inviter as a person wanting to invite the owner
	 * of an email address.  @see createGetInvitation
	 * 
	 * @param inviter
	 * @param email
	 * @return
	 */
	public InvitationToken createEmailInvitation(Person inviter, String email);
	
	/**
	 * Add inviter as a person wanting to invite invitee into the system.
	 * @param inviter the person doing the inviting
	 * @param invitee the person being invited
	 * @return an invitation object describing the Multiple Invite Group
	 */
	public InvitationToken createGetInvitation(Person inviter, Resource invitee);
	
	/**
	 * Mark an invitation as viewed; this creates an initial HippoAccount
	 * for the user and such, and grants the client access to the account
	 * by adding a Client object. If firstClientName is null no client 
	 * is added.
	 * 
	 * @param invite the invitation
	 * @param firstClientName name of the first client to create
	 * @return initial client authorized to access the account
	 */
	public Client viewInvitation(InvitationToken invite, String firstClientName);
	
	/**
	 * Return the names (from the system viewpoint) of the inviting
	 * people for an invitation.
	 * 
	 * @param invite an invitation
	 * @return a collection of names
	 */
	public Collection<String> getInviterNames(InvitationToken invite);
	
	/**
	 * Send an email to the invitee that inviter has requested them
	 * to join.
	 * @param spider
	 * @param invite
	 * @param inviter
	 */
	public void sendEmailNotification(InvitationToken invite, Person inviter);
}
