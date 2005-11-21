package com.dumbhippo.web;

import com.dumbhippo.server.InvitationSystem;

/**
 * InvitePage corresponds to invite.jsp
 * 
 * @author dff
 * 
 */

public class InvitePage {
	private String fullName;

	private String email;
	
	@Signin
	private SigninBean signin;

	private InvitationSystem invitationSystem;
	
	private int invitations;

	public InvitePage() {
		invitations = -1;
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
	}
	
	public String getFullName() {
		return fullName;
	}

	public void setFullName(String newValue) {
		fullName = newValue;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String newValue) {
		email = newValue;
	}

	public SigninBean getSignin() {
		return signin;
	}
		
	public int getInvitations() {
		if (invitations < 0) {
			invitations = invitationSystem.getInvitations(signin.getUser()); 
		}
		return invitations;
	}
}
