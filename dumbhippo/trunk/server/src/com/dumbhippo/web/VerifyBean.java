package com.dumbhippo.web;

import java.util.Collection;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;

import com.dumbhippo.persistence.*;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;

/**
 * InviteBean corresponds to the verify account page, the page that a user
 * arrives at from their account confirmation email.
 * 
 * @author dff
 */

public class VerifyBean {
	private String authKey;

	private String invitedBy; // TODO: should be a collection of some sort\

	private boolean valid;
	private Collection<String> inviterNames;

	// PROPERTY: authKey
	public String getAuthKey() {
		return authKey;
	}

	public void setAuthKey(String newValue) {
		authKey = newValue;
		System.out.println("validating auth key");
		Invitation invite = invitationSystem.lookupInvitationByKey(getAuthKey());
		if (invite != null)
			inviterNames = invitationSystem.getInviterNames(invite);
		else
			inviterNames = null;
		valid = (inviterNames != null);
	}

	// PROPERTY: invitedBy
	public String getInvitedBy() {
		return invitedBy;
	}

	public void setInvitedBy(String newValue) {
		invitedBy = newValue;
	}

	private transient InvitationSystem invitationSystem;

	private transient IdentitySpider identitySpider;

	public VerifyBean() throws NamingException {
		InitialContext ctx = new InitialContext();
		invitationSystem = (InvitationSystem) ctx.lookup(InvitationSystem.class.getName());
		identitySpider = (IdentitySpider) ctx.lookup(IdentitySpider.class.getName());
	}

	// called to verify user
	public String doVerify() {

		return "mainpage";
	}

	public Collection<String> getInviterNames() {
		return inviterNames;
	}

	public boolean isValid() {
		return valid;
	}
}
