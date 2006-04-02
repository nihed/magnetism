package com.dumbhippo.web;

import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;


/**
 * This base class contains methods for getting information about the person
 * who is signed in. It has two subclasses that are normally used rather than
 * this one directly; AbstractSigninRequiredPage requires someone to be logged in, 
 * and AbstractSigninOptionalPage allows anonymous viewers of the page. However,
 * most methods on this class can't be called if the viewer is anonymous, so 
 * when using AbstractSigninOptionalPage you have to check getSignin().isValid(). 
 * 
 * @author marinaz
 */
public abstract class AbstractSigninPage {

	protected IdentitySpider identitySpider;
	protected InvitationSystem invitationSystem;
	protected int invitations;
	protected PersonView signinPerson;
	protected ListBean<PersonView> contacts;	
	
	protected AbstractSigninPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);		
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		invitations = -1;
	}
	
	abstract public SigninBean getSignin();
	
	/**
	 * Return user signin bean; throws an exception if our signin is 
	 * not a user signin. Any method that calls this must only be called
	 * if getSignin().isValid()
	 * @return the user signin bean, or throws an exception
	 */
	public UserSigninBean getUserSignin() {
		SigninBean signin = getSignin();
		if (!(signin instanceof UserSigninBean))
			throw new IllegalStateException("this operation requires checking signin.valid first to be sure a user is signed in");
		return (UserSigninBean) signin;
	}

	public PersonView getPerson() {
		if (signinPerson == null)
			signinPerson = identitySpider.getPersonView(getSignin().getViewpoint(), getUserSignin().getUser(), PersonViewExtra.ALL_RESOURCES);
		
		return signinPerson;
	}
	
	public int getInvitations() {
		if (invitations < 0) {
			invitations = invitationSystem.getInvitations(getUserSignin().getUser()); 
		}
		return invitations;
	}
	
	public ListBean<PersonView> getContacts() {
		if (contacts == null) {
			contacts = new ListBean<PersonView>(PersonView.sortedList(identitySpider.getContacts(getSignin().getViewpoint(), getUserSignin().getUser(), false, PersonViewExtra.INVITED_STATUS, PersonViewExtra.PRIMARY_EMAIL, PersonViewExtra.PRIMARY_AIM)));
		}
		return contacts;
	}
}
