/**
 * 
 */
package com.dumbhippo.persistence;

import java.net.URL;

/**
 * @author hp
 *
 */
public interface InvitableResource {
	public void sendInvite(URL prefix, Invitation invitation, Person inviter);
}
