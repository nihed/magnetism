/**
 * 
 */
package com.dumbhippo.persistence;

import java.net.URL;

import com.dumbhippo.server.IdentitySpider;

/**
 * @author hp
 *
 */
public interface InvitableResource {
	public void sendInvite(IdentitySpider spider, URL prefix, Invitation invitation, Person inviter);
}
