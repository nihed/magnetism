package com.dumbhippo.web;

import java.util.Collection;

import javax.naming.NamingException;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Invitation;
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

	@Inject
	private InvitationSystem invitationSystem;

	public VerifyBean() throws NamingException {
		EjbLink.injectFromFacesContext(this, Scope.NONE);
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
			Client client = invitationSystem.viewInvitation(invite, SigninBean.computeClientIdentifier());
			SigninBean.setCookie(invite.getResultingPerson().getId(),
					client.getAuthKey());
		}
	}

	public Collection<String> getInviterNames() {
		return inviterNames;
	}

	public boolean isValid() {
		return valid;
	}
}
