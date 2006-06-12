package com.dumbhippo.web.pages;

import org.slf4j.Logger;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.InvitationView;
import com.dumbhippo.web.ListBean;

/**
 * This class contains some functions required by both invite
 * and invites pages.
 * 
 * @author marinaz
 */
public abstract class AbstractInvitePage extends AbstractSigninRequiredPage {
	static protected final Logger logger = GlobalSetup.getLogger(InvitePage.class);
	
	// information about existing outstanding invitations
	private ListBean<InvitationView> outstandingInvitations;
	
	
	protected AbstractInvitePage() {
	}
	
	public ListBean<InvitationView> getOutstandingInvitations() {
		if (outstandingInvitations == null) {
			//logger.debug("Getting outstanding invitations by {}", signin.getUser().getId());
			outstandingInvitations = 
				new ListBean<InvitationView>(
				    invitationSystem.findOutstandingInvitations(getUserSignin().getViewpoint(), 
				    		                                    0, -1));
		}
		return outstandingInvitations;
	}
}
