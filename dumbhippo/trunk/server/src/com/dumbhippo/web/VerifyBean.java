package com.dumbhippo.web;

import java.util.Collection;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dumbhippo.persistence.Client;
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
	
	private transient AccountSystem accountSystem;
	private transient InvitationSystem invitationSystem;
	
	public VerifyBean() throws NamingException {
		InitialContext ctx = new InitialContext();
		invitationSystem = (InvitationSystem) ctx.lookup(InvitationSystem.class.getName());
		accountSystem = (AccountSystem) (new InitialContext()).lookup(AccountSystem.class.getName());
	}	
	
	public String getAuthKey() {
		return authKey;
	}

	private String computeClientIdentifier() {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();
		
		StringBuilder ret = new StringBuilder();
		ret.append(req.getRemoteAddr());
		String agent = req.getHeader("user-agent");
		if (agent != null) {
			ret.append('/');
			ret.append(agent);
		}
		
		return ret.toString();
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
			newAccount = accountSystem.createAccountFromResource(invite.getInvitee());
			
			Client newClient = accountSystem.authorizeNewClient(newAccount, computeClientIdentifier());
			
			ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
			HttpServletResponse response = (HttpServletResponse) ctx.getResponse();
			
			LoginCookie loginCookie = new LoginCookie(newAccount, newClient);
			
			response.addCookie(loginCookie.getCookie());
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
}
