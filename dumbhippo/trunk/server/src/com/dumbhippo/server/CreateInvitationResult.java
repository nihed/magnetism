package com.dumbhippo.server;

/**
 * When you try to create an invitation, there are various flavors of 
 * "success" - this isn't an exception since all of these are normally fine
 * @author Havoc Pennington
 *
 */
public enum CreateInvitationResult {
	INVITE_CREATED,
	ALREADY_HAS_ACCOUNT,
	ALREADY_INVITED
}
