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

	// FIXME some subclasses require signin so this would be 
	// UserSigninBean, but for now we have to accomodate 
	// ViewPersonPage, PersonMusicPage, etc. that don't require it
	// the real fix is probably changing the inheritance hierarchy
	// of the page beans
	@Signin
	private SigninBean signin;

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
	
	/**
	 * Return user signin bean; throws an exception if our signin is 
	 * not a user signin
	 * @return
	 */
	public UserSigninBean getUserSignin() {
		if (!(signin instanceof UserSigninBean))
			throw new RuntimeException("This page requires signin");
		return (UserSigninBean) signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), getUserSignin().getUser(), PersonViewExtra.ALL_RESOURCES);
		
		return person;
	}
	
	public int getInvitations() {
		if (invitations < 0) {
			invitations = invitationSystem.getInvitations(getUserSignin().getUser()); 
		}
		return invitations;
	}
	
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			contacts = new ListBean<PersonView>(PersonView.sortedList(identitySpider.getContacts(signin.getViewpoint(), getUserSignin().getUser(), false, PersonViewExtra.INVITED_STATUS, PersonViewExtra.PRIMARY_EMAIL, PersonViewExtra.PRIMARY_AIM)));
		}
		return contacts;
	}
	
}
