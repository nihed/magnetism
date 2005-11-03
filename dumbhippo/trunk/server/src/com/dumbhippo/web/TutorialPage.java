package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;

/**
 * @author hp
 *
 * Shows a tutorial on how to share a link, and lets you put in some of your 
 * personal information.
 */
public class TutorialPage {
	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(TutorialPage.class);

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PersonView person;
	
	public TutorialPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser());
		
		return person;
	}
}
