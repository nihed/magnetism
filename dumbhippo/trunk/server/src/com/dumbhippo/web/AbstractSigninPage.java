package com.dumbhippo.web;

import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;


/**
 * This class contains some information required by pages for which
 * the user has to be signed in, such as home page and invite pages.
 * 
 * @author marinaz
 */
public abstract class AbstractSigninPage {

	@Signin
	protected UserSigninBean signin;

	protected IdentitySpider identitySpider;
	protected InvitationSystem invitationSystem;
	protected int invitations;
	protected PersonView person;
	protected ListBean<PersonView> contacts;	
	
	protected AbstractSigninPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		invitations = -1;
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser(), PersonViewExtra.ALL_RESOURCES);
		
		return person;
	}
	
	public int getInvitations() {
		if (invitations < 0) {
			invitations = invitationSystem.getInvitations(signin.getUser()); 
		}
		return invitations;
	}
	
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			contacts = new ListBean<PersonView>(PersonView.sortedList(identitySpider.getContacts(signin.getViewpoint(), signin.getUser(), false, PersonViewExtra.INVITED_STATUS, PersonViewExtra.PRIMARY_EMAIL, PersonViewExtra.PRIMARY_AIM)));
		}
		return contacts;
	}
	
}
