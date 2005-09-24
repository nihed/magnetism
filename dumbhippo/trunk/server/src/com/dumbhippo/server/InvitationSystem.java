package com.dumbhippo.server;

public interface InvitationSystem {
	public String createInvitationKey(Account inviter, String aimAddress);
}
