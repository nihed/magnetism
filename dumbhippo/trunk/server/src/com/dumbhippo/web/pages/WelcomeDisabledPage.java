package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class WelcomeDisabledPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(WelcomeDisabledPage.class);

	@Signin
	private UserSigninBean signin;
	
	private Configuration configuration;
	private IdentitySpider identitySpider;
	private PersonView person;
	
	public WelcomeDisabledPage() {
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser(), PersonViewExtra.PRIMARY_EMAIL);
		
		return person;
	}
	
	public String getFeedbackEmail() {
		return configuration.getProperty(HippoProperty.FEEDBACK_EMAIL);
	}
}
