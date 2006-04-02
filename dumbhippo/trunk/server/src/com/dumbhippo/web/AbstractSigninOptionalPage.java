package com.dumbhippo.web;

/**
 * A subclass of AbstractSigninPage that allows anonymous viewers of the page.
 * To call most methods on AbstractSigninPage, it's necessary to check first
 * that the viewer is not anonymous with getSignin().isValid()
 * 
 * @author Havoc Pennington
 *
 */
public class AbstractSigninOptionalPage extends AbstractSigninPage {

	@Signin
	private SigninBean signin;
	
	public SigninBean getSignin() {
		return signin;
	}
}
