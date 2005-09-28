/**
 * 
 */
package com.dumbhippo.server;

/**
 * @author hp
 *
 */
public interface InvitableResource {
	public void sendInvite(IdentitySpider spider, Invitation invitation, Person inviter);
}
