package com.dumbhippo.web.pages;

import com.dumbhippo.web.Signin;
import com.dumbhippo.web.UserSigninBean;

/**
 * A subclass of AbstractSigninPage that throws an exception on page load
 * if the user is not logged in.
 * 
 * @author Havoc Pennington
 */
public class AbstractSigninRequiredPage extends AbstractSigninPage {

	@Signin
	private UserSigninBean signin;

	@Override
	public UserSigninBean getSignin() {
		return signin;
	}
}
