package com.dumbhippo.web;

/**
 * A subclass of AbstractSigninPage that throws an exception on page load
 * if the user is not logged in.
 * 
 * @author Havoc Pennington
 */
public class AbstractSigninRequiredPage extends AbstractSigninPage {

	@Signin
	private UserSigninBean signin;

	public UserSigninBean getSignin() {
		return signin;
	}
}
