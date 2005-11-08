package com.dumbhippo.web;

import com.dumbhippo.persistence.InvitationToken;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.InvitationSystem;

/**
 * InviteBean corresponds to the invite JSF page.
 * 
 * @author dff
 * 
 */

public class InviteBean {
	private String fullName;

	private String email;

	// FIXME should go away and be replaced by cookie
	private String inviterEmail;
	
	private String authKey;
	
	@Signin
	private SigninBean signin;

	private InvitationSystem invitationSystem;
	
	public InviteBean() {
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
	}

	// action handler for form submit
	public String doInvite() {
		Person user = signin.getUser();
		
		InvitationToken invitation 
			= invitationSystem.createEmailInvitation(user, getEmail());
		
		invitationSystem.sendEmailNotification(invitation, user);

		this.authKey = invitation.getAuthKey();
		return "invitesent";
	}
	
	// PROPERTY: fullName
	public String getFullName() {
		return fullName;
	}

	public void setFullName(String newValue) {
		fullName = newValue;
	}

	// PROPERTY: email
	public String getEmail() {
		return email;
	}

	public void setEmail(String newValue) {
		email = newValue;
	}

	public String getInviterEmail() {
		return inviterEmail;
	}

	public void setInviterEmail(String inviterEmail) {
		this.inviterEmail = inviterEmail;
	}

	public String getAuthKey() {
		return authKey;
	}

	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	public SigninBean getSignin() {
		return signin;
	}
}
