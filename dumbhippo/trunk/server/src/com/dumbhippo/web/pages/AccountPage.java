package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.web.Signin;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;

/**
 * @author hp
 *
 * Page to manage personal information associated with an account.
 */
public class AccountPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(AccountPage.class);

	@Signin
	private UserSigninBean signin;
	
	private IdentitySpider identitySpider;
	private PersonView person;
	private Configuration config;
	private ClaimVerifier claimVerifier;
	
	public AccountPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);
		claimVerifier = WebEJBUtil.defaultLookup(ClaimVerifier.class);
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = identitySpider.getPersonView(signin.getViewpoint(), signin.getUser(), PersonViewExtra.ALL_RESOURCES);
		
		return person;
	}

	public boolean getCanRemoveEmails() {
		return person.getAllEmails().size() > 1;
	}
	
	public String getAimPresenceKey() {
		try {
			return config.getPropertyNoDefault(HippoProperty.AIM_PRESENCE_KEY);
		} catch (PropertyNotFoundException pnfe) {
			return null;
		}
	}
	
	public String getAddAimLink() {
		String token = claimVerifier.getAuthKey(signin.getUser(), null); 
		return "aim:GoIM?screenname=" + config.getPropertyFatalIfUnset(HippoProperty.AIMBOT_NAME) 
		+ "&message=Hey+Bot!+Crunch+this:+" + token;
	}
	
	public boolean getHasPassword() {
		return signin.getUser().getAccount().getHasPassword();
	}
}
