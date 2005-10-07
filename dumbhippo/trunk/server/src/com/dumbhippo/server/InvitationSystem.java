package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;

@Local
public interface InvitationSystem {
	
	/**
	 * Look up an invitation by authentication key
	 * @param authKey potential authentication key
	 * @return the corresponding invitation, or null if none
	 */
	public Invitation lookupInvitationByKey(String authKey);
	
	/**
	 * Add inviter as a person wanting to invite invitee into the system.
	 * @param inviter the person doing the inviting
	 * @param invitee the person being invited
	 * @return an invitation object describing the Multiple Invite Group
	 */
	public Invitation createGetInvitation(Person inviter, Resource invitee);
	
	/**
	 * Mark an invitation as viewed; this creates an initial HippoAccount
	 * for the user and such, and grants the client access to the account
	 * via a shared secret.
	 * 
	 * @param invite
	 * @return an shared secret usable for authentication
	 */
	public HippoAccount viewInvitation(Invitation invite);
	
	/**
	 * Send an email to the invitee that inviter has requested them
	 * to join.
	 * @param spider
	 * @param invite
	 * @param inviter
	 */
	public void sendEmailNotification(Invitation invite, Person inviter);
}
