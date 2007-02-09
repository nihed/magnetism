package com.dumbhippo.web.pages;

import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;


public class NowPlayingPage {

	@Signin
	private UserSigninBean signin;
	
	public NowPlayingPage() {
	}
	
	public SigninBean getSignin() {
		return signin;
	}
}
