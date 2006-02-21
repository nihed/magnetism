package com.dumbhippo.web;

import org.slf4j.Logger;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.InvitationView;
import com.dumbhippo.server.Viewpoint;

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
	private int start;
	private int totalInvitations;
	protected int maxInvitationsShown;
	
	
	protected AbstractInvitePage() {
		start = 0;
		totalInvitations = -1;
		maxInvitationsShown = 4;
	}
	
	public ListBean<InvitationView> getOutstandingInvitations() {
		if (outstandingInvitations == null) {
			//logger.debug("Getting outstanding invitations by {}", signin.getUser().getId());
			outstandingInvitations = 
				new ListBean<InvitationView>(
				    invitationSystem.findOutstandingInvitations(new Viewpoint(signin.getUser()), 
				    		                                    start, 
				    		                                    maxInvitationsShown+1));
		}
		return outstandingInvitations;
	}

	public int getTotalInvitations() {
		if (totalInvitations < 0) {
			totalInvitations = 
				invitationSystem.countOutstandingInvitations(new Viewpoint(signin.getUser()));
		}
		return totalInvitations;
	}
	
	public void setStart(int start) {
		if (start < 0) {
			this.start = 0;
		} else {		
	        this.start = start;
		}
	}
	
	public int getStart() {
		return start;
	}
	
	public int getMaxInvitationsShown() {
		return maxInvitationsShown;
	}
}
