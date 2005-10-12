package com.dumbhippo.web;

import java.util.Collection;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.InvitationSystem;

/**
 * InviteBean corresponds to the verify account page, the page that a user
 * arrives at from their account confirmation email.
 * 
 * @author dff
 */

public class VerifyBean {
	private String authKey;
	private boolean valid;
	private Collection<String> inviterNames;
	private HippoAccount newAccount;
	
	private InvitationSystem invitationSystem;
	
	private SigninBean signIn;

	public VerifyBean() throws NamingException {
		InitialContext ctx = new InitialContext();
		invitationSystem = (InvitationSystem) ctx.lookup(InvitationSystem.class.getName());
	}	
	
	public String getAuthKey() {
		return authKey;
	}

	/**
	 * Takes an invitation authentication key and creates a new user account, etc
	 * from it.
	 * 
	 * @param newValue
	 * @throws NamingException
	 */
	public void setAuthKey(String newValue) throws NamingException {
		authKey = newValue;
		System.out.println("validating auth key");
		Invitation invite = invitationSystem.lookupInvitationByKey(getAuthKey());
		if (invite != null)
			inviterNames = invitationSystem.getInviterNames(invite);
		else
			inviterNames = null;
		valid = (inviterNames != null);
		if (valid) {
			signIn.initNewAccountFromResource(invite.getInvitee());
		}
	}

	public Collection<String> getInviterNames() {
		return inviterNames;
	}

	public boolean isValid() {
		return valid;
	}

	public HippoAccount getNewAccount() {
		return newAccount;
	}

	public void setNewAccount(HippoAccount newAccount) {
		this.newAccount = newAccount;
	}

	public void setSignIn(SigninBean signIn) {
		this.signIn = signIn;
	}
}
