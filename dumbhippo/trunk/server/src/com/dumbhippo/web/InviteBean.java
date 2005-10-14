package com.dumbhippo.web;

import javax.naming.NamingException;

import com.dumbhippo.persistence.Invitation;
import com.dumbhippo.persistence.Person;
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

	@Inject
	private IdentitySpider identitySpider;
	
	@Inject
	private InvitationSystem invitationSystem;

	@Inject
	private EjbLink ejb;
	
	public InviteBean() {
		EjbLink.injectFromFacesContext(this, Scope.NONE);		
	}

	// action handler for form submit
	public String doInvite() throws NamingException {
		if (!ejb.checkLoginFromFacesContext(this)) {
			throw new RuntimeException("not signed in");
		}
	
		Person person = identitySpider.lookupPersonById(ejb.getLoggedInUser());
		
		Invitation invitation 
			= invitationSystem.createEmailInvitation(person, getEmail());

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
