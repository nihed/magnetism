package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.AccountFeed;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
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
	private ExternalAccountSystem externalAccounts;
	
	public AccountPage() {
		identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);
		claimVerifier = WebEJBUtil.defaultLookup(ClaimVerifier.class);
		externalAccounts = WebEJBUtil.defaultLookup(ExternalAccountSystem.class);
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
	
	public String getRhapsodyListeningHistoryFeedUrl() {
		AccountFeed rhapsodyHistoryFeed = signin.getUser().getAccount().getRhapsodyHistoryFeed();
		if (rhapsodyHistoryFeed != null) {
			return rhapsodyHistoryFeed.getFeed().getSource().getUrl();
		}
		return null;
	}
	
	private String getExternalAccountSentiment(ExternalAccountType type) {
		ExternalAccount external;
		try {
			external = externalAccounts.lookupExternalAccount(signin.getViewpoint(), signin.getUser(), type);
		} catch (NotFoundException e) {
			return Sentiment.INDIFFERENT.name().toLowerCase();
		}
		
		return external.getSentiment().name().toLowerCase();
	}
	
	private String getExternalAccountHateQuip(ExternalAccountType type) {
		ExternalAccount external;
		try {
			external = externalAccounts.lookupExternalAccount(signin.getViewpoint(), signin.getUser(), type);
		} catch (NotFoundException e) {
			return "";
		}
		
		return external.getQuip();
	}
	
	// don't export the "Handle" vague name to the .jsp please, change the bean getter 
	// to be something legible like "email" or "userid" or whatever it is for the account type
	private String getExternalAccountHandle(ExternalAccountType type) {
		ExternalAccount external;
		try {
			external = externalAccounts.lookupExternalAccount(signin.getViewpoint(), signin.getUser(), type);
		} catch (NotFoundException e) {
			return "";
		}
		return external.getHandle();
	}

	// don't export the "Extra" vague name to the .jsp please, change the bean getter 
	// to be something legible like "email" or "userid" or whatever it is for the account type
	private String getExternalAccountExtra(ExternalAccountType type) {
		ExternalAccount external;
		try {
			external = externalAccounts.lookupExternalAccount(signin.getViewpoint(), signin.getUser(), type);
		} catch (NotFoundException e) {
			return "";
		}
		return external.getExtra();
	}

	public String getMySpaceSentiment() {
		return getExternalAccountSentiment(ExternalAccountType.MYSPACE);
	}
	
	public String getMySpaceHateQuip() {
		return getExternalAccountHateQuip(ExternalAccountType.MYSPACE);
	}
	
	public String getMySpaceName() {
		return getExternalAccountHandle(ExternalAccountType.MYSPACE);
	}
	
	public String getFlickrSentiment() {
		return getExternalAccountSentiment(ExternalAccountType.FLICKR);
	}
	
	public String getFlickrHateQuip() {
		return getExternalAccountHateQuip(ExternalAccountType.FLICKR);
	}
	
	public String getFlickrEmail() {
		return getExternalAccountExtra(ExternalAccountType.FLICKR);
	}
}
