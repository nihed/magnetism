package com.dumbhippo.web;

import com.dumbhippo.persistence.Resource;
import com.dumbhippo.server.InvitationView;

/**
 * InvitePage corresponds to invite.jsp
 * 
 * @author dff, marinaz
 * 
 */
public class InvitePage extends AbstractInvitePage {

	// information about person to invite
	private String email;
	
	// previous invitation by the inviter to the invitee 
	private InvitationView previousInvitation;
	private boolean checkedForPreviousInvitation;
	
	public InvitePage() {
		checkedForPreviousInvitation = false;
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String newValue) {
		email = newValue;
	}		
	
	public InvitationView getPreviousInvitation() {
		if (checkedForPreviousInvitation) {
			return previousInvitation;
		}
		
    	checkedForPreviousInvitation = true;
    	
		if (email == null) {
			// we need to know invitee's e-mail to check for the previous invitatation
			// previousInvitation is probably null anyway, but set it to null just in case
            previousInvitation = null;
		} else {
		    Resource emailRes = identitySpider.getEmail(email);
	        previousInvitation = 
	        	invitationSystem.lookupInvitationViewFor(getUserSignin().getViewpoint(), 
	        			                                 emailRes);
	    }
	    
	    return previousInvitation;
	}
	
	/**
	 * This function checks if previousInvitation is currently a valid invitation.
	 * It is useful when we want to allow someone who is out of invitation vouchers to
	 * resend an invitation. From an interface point of view, we want to make the 
	 * invitee e-mail field read-only in that case. 
	 * 
	 * @return a flag indicating if previousInvitation is currently a valid invitation
	 */
	public boolean isValidPreviousInvitation() {
		if (getPreviousInvitation() == null) {
			return false;
		}

		// we want to make sure the invitation token did not expire
		// also, having the invitation token not deleted in the system
		// (as opposed to this particular inviter not having deleted
		// the invitation) is sufficient for the invitation to be valid
		// and ready to be resent without using up invitations
		return getPreviousInvitation().getInvite().isValid();
	}
	
	/**
	 * @return invitation subject
	 */
	public String getSubject() {
        if (getPreviousInvitation() != null) {
        	return previousInvitation.getInviterData().getInvitationSubject();
        } else {
        	return "Invitation from " + getPerson().getName() + " to use Mugshot";
        }        	
	}
	
	/**
	 * @return invitation message
	 */
	public String getMessage() {
        if (getPreviousInvitation() != null) {
        	return previousInvitation.getInviterData().getInvitationMessage();
        } else {
        	return "[your message goes here]";
        }        	
	}	
}
