package com.dumbhippo.web;

import org.slf4j.Logger;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.InvitationView;

/**
 * This class contains some functions required by both invite
 * and invites pages.
 * 
 * @author marinaz
 */
public abstract class AbstractInvitePage extends AbstractSigninPage {
	static protected final Logger logger = GlobalSetup.getLogger(InvitePage.class);
	
	// information about existing outstanding invitations
	private ListBean<InvitationView> outstandingInvitations;
	
	public ListBean<InvitationView> getOutstandingInvitations() {
		if (outstandingInvitations == null) {
			logger.debug("Getting outstanding invitations by " + signin.getUser().getId());
			outstandingInvitations = new ListBean<InvitationView>(invitationSystem.findOutstandingInvitations(signin.getUser()));
		}
		return outstandingInvitations;
	}

}
