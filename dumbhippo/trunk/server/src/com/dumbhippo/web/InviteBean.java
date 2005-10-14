package com.dumbhippo.web;

import javax.naming.NamingException;

import com.dumbhippo.persistence.Invitation;
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

	private InvitationSystem invitationSystem;

	private SigninBean signin;
	
	public InviteBean() {
		invitationSystem = (new EjbLink()).getEjb(InvitationSystem.class);		
	}

	// Injected
	public void setSignin(SigninBean signin) {
		this.signin = signin;
	}

	// action handler for form submit
	public String doInvite() throws NamingException {
		if (!signin.isValid()) {
			throw new RuntimeException("not signed in");
		}
		
		Invitation invitation 
			= invitationSystem.createEmailInvitation(signin.getAccount().getOwner(), getEmail());

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
}