package com.dumbhippo.web.pages;

import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;


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
	protected PersonViewer personViewer;
	protected AccountSystem accountSystem;
	protected InvitationSystem invitationSystem;
	protected int invitations;
	protected PersonView signinPerson;
	
	protected AbstractSigninPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		personViewer = WebEJBUtil.defaultLookup(PersonViewer.class);
		accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);
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

	/**
	 * Return a PersonView of the signed in user.
	 * 
	 * @return a PersonView of the signed in user
	 */
	public PersonView getPerson() {
		if (signinPerson == null && getSignin().isValid())
			signinPerson = personViewer.getPersonView(getSignin().getViewpoint(), getUserSignin().getUser(), PersonViewExtra.ALL_RESOURCES);
		
		return signinPerson;
	}
	
	public int getInvitations() {
		if ((invitations < 0)  && getSignin().isValid()) {
			invitations = invitationSystem.getInvitations(getUserSignin().getUser()); 
		}
		return invitations;
	}
}
