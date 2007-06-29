package com.dumbhippo.web.pages;

import com.dumbhippo.server.views.AnonymousViewpoint;
import com.dumbhippo.server.views.Viewpoint;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;

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
	
	public Viewpoint getViewpoint() {
		if (getSignin().isValid()) {
			return getUserSignin().getViewpoint();
		}
		return AnonymousViewpoint.getInstance();
	}	
	
	@Override
	public SigninBean getSignin() {
		return signin;
	}
}
