package com.dumbhippo.web;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
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

	private transient InvitationSystem invitationSystem;

	private transient IdentitySpider identitySpider;
	
	public InviteBean() throws NamingException {
		InitialContext ctx = new InitialContext();
		invitationSystem = (InvitationSystem) ctx.lookup(InvitationSystem.class.getName());
		identitySpider = (IdentitySpider) ctx.lookup(IdentitySpider.class.getName());		
	}
	
	private Person temporaryHackCreateAccount(String email) throws NamingException {
		EmailResource inviterEmail = identitySpider.getEmail(getInviterEmail());
		Person inviter;
		try {
			inviter = identitySpider.lookupPersonByEmail(inviterEmail);
		} catch (EJBException e) { // FIXME should be EntityNotFoundException according to spec
			AccountSystem accounts = (AccountSystem) (new InitialContext()).lookup(AccountSystem.class.getName());
			HippoAccount acct = accounts.createAccountFromEmail(email);
			inviter = acct.getOwner();
		}
		return inviter;
	}

	// action handler for form submit
	public String doInvite() throws NamingException {
		Person inviter = temporaryHackCreateAccount(getInviterEmail());
		Invitation invitation = invitationSystem.createEmailInvitation(inviter, getEmail());

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