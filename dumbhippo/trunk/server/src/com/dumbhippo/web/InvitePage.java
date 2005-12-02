package com.dumbhippo.web;

import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;

/**
 * InvitePage corresponds to invite.jsp
 * 
 * @author dff
 * 
 */

public class InvitePage {
	// Information about person to invite
	private String email;
	
	@Signin
	private SigninBean signin;

	private InvitationSystem invitationSystem;
	private IdentitySpider identitySpider;
	
	// information about person doing the inviting
	private int invitations;
	private PersonView person;

	public InvitePage() {
		invitations = -1;
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	}
	
	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser(), PersonViewExtra.PRIMARY_EMAIL);
		
		return person;
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
