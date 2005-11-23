package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;

/**
 * @author hp
 *
 * Has fun tutorials and lets you set up personal information.
 */
public class AccountPage {
	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(AccountPage.class);

	@Signin
	private SigninBean signin;
	
	private IdentitySpider identitySpider;
	private PersonView person;
	private Configuration config;
	private ClaimVerifier claimVerifier;
	private Boolean disabled;
	
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
	
	public String getAddAimLink() {
		String token = claimVerifier.getAuthKey(signin.getUser(), null); 
		return "aim:GoIM?screenname=" + config.getPropertyFatalIfUnset(HippoProperty.AIMBOT_NAME) 
		+ "&message=Hey+Bot!+Crunch+this:+" + token;
	}
	
	public boolean getDisabled() {
		if (disabled == null) {
			disabled = Boolean.valueOf(identitySpider.getAccountDisabled(signin.getUser()));
			logger.debug("AccountPage loaded disabled = " + disabled);
		}
		return disabled;
	}
}
