package com.dumbhippo.server;

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
	 * Send an email to the invitee that inviter has requested them
	 * to join.
	 * @param spider
	 * @param invite
	 * @param inviter
	 */
	public void sendEmailNotification(IdentitySpider spider, Invitation invite, Person inviter);
}
