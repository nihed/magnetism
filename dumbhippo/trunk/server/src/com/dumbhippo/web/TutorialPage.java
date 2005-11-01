package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonInfo;

/**
 * @author hp
 *
 * Shows a tutorial on how to share a link, and lets you put in some of your 
 * personal information.
 */
public class TutorialPage {
	static private final Log logger = GlobalSetup.getLog(TutorialPage.class);

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PersonInfo personInfo;
	
	public TutorialPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonInfo getPersonInfo() {
		if (personInfo == null)
			personInfo = new PersonInfo(identitySpider, signin.getUser(), signin.getUser());
		
		return personInfo;
	}
}
