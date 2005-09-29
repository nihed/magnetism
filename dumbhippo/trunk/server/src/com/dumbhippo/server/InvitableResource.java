/**
 * 
 */
package com.dumbhippo.server;

import java.net.URL;

/**
 * @author hp
 *
 */
public interface InvitableResource {
	public void sendInvite(IdentitySpider spider, URL prefix, Invitation invitation, Person inviter);
}
