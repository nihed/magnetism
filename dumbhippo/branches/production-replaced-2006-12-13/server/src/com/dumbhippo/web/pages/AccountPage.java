package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
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
	
	private PersonViewer personViewer;
	private PersonView person;
	private Configuration config;
	private ClaimVerifier claimVerifier;
	private ExternalAccountSystem externalAccounts;
	private FacebookTracker facebookTracker;
	private FacebookSystem facebookSystem;
	private String facebookAuthToken;
	
	public AccountPage() {
		personViewer = WebEJBUtil.defaultLookup(PersonViewer.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);
		claimVerifier = WebEJBUtil.defaultLookup(ClaimVerifier.class);
		externalAccounts = WebEJBUtil.defaultLookup(ExternalAccountSystem.class);
		facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
		facebookSystem =  WebEJBUtil.defaultLookup(FacebookSystem.class);
		facebookAuthToken = null;
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = personViewer.getPersonView(signin.getViewpoint(), signin.getUser(), PersonViewExtra.ALL_RESOURCES);
		
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
		ExternalAccount external;
		try {
			external = externalAccounts.lookupExternalAccount(signin.getViewpoint(), signin.getUser(), ExternalAccountType.RHAPSODY);
		} catch (NotFoundException e) {
			return null;
		}
		if (external.getSentiment() != Sentiment.LOVE) {
			return null;
		} else {
			return external.getFeed().getSource().getUrl();
		}
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
	
	public String getLinkedInSentiment() {
		return getExternalAccountSentiment(ExternalAccountType.LINKED_IN);
	}
	
	public String getLinkedInHateQuip() {
		return getExternalAccountHateQuip(ExternalAccountType.LINKED_IN);
	}
	
	public String getLinkedInName() {
		return getExternalAccountHandle(ExternalAccountType.LINKED_IN);
	}

	public String getYouTubeSentiment() {
		return getExternalAccountSentiment(ExternalAccountType.YOUTUBE);
	}
	
	public String getYouTubeHateQuip() {
		return getExternalAccountHateQuip(ExternalAccountType.YOUTUBE);
	}
	
	public String getYouTubeName() {
		return getExternalAccountHandle(ExternalAccountType.YOUTUBE);
	}
		
	public String getWebsiteUrl() {
		logger.debug("returning {} for website ", getExternalAccountHandle(ExternalAccountType.WEBSITE));
		return getExternalAccountHandle(ExternalAccountType.WEBSITE);
	}
	
	public String getBlogUrl() {
		logger.debug("returning {} for blog ", getExternalAccountHandle(ExternalAccountType.BLOG));
		return getExternalAccountHandle(ExternalAccountType.BLOG);
	}	
	
    public void setFacebookAuthToken(String facebookAuthToken) {
    	this.facebookAuthToken = facebookAuthToken;  	
    	// request a session key for the signed in user and set it in the database 
    	facebookTracker.updateOrCreateFacebookAccount(signin.getViewpoint(), facebookAuthToken);
    }
    
    public String getFacebookAuthToken() {
    	return facebookAuthToken;
    }
    
    public String getFacebookApiKey() {
        return facebookSystem.getApiKey();
    }
}
