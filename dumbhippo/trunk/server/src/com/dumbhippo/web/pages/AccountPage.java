package com.dumbhippo.web.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Pair;
import com.dumbhippo.SortUtils;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.OnlineAccountType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.XmppResource;
import com.dumbhippo.server.AmazonUpdater;
import com.dumbhippo.server.ClaimVerifier;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.web.ListBean;
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
	private ClaimVerifier claimVerifier;
	private Configuration config;
	private ExternalAccountSystem externalAccounts;
	private FacebookSystem facebookSystem;
	private String facebookAuthToken;
	private String facebookErrorMessage;
	private AmazonUpdater amazonUpdater;
	private ListBean<ExternalAccountView> supportedAccountsListBean;
	private ListBean<ExternalAccountView> gnomeSupportedAccountsListBean;
	
	public AccountPage() {
		claimVerifier = WebEJBUtil.defaultLookup(ClaimVerifier.class);
		personViewer = WebEJBUtil.defaultLookup(PersonViewer.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);
		externalAccounts = WebEJBUtil.defaultLookup(ExternalAccountSystem.class);
		facebookSystem =  WebEJBUtil.defaultLookup(FacebookSystem.class);
		facebookAuthToken = null;
		facebookErrorMessage = null;
		amazonUpdater = WebEJBUtil.defaultLookup(AmazonUpdater.class);
		supportedAccountsListBean = null;
		gnomeSupportedAccountsListBean = null;
	}
	
	public SigninBean getSignin() {
		return signin;
	}

	public PersonView getPerson() {
		if (person == null)
			person = personViewer.getPersonView(signin.getViewpoint(), signin.getUser());
		
		return person;
	}
	
	public boolean isLoggedInToFacebook() {
		try {
		    FacebookAccount facebookAccount = facebookSystem.lookupFacebookAccount(signin.getViewpoint(), signin.getUser()); 
		    return facebookAccount.isSessionKeyValid();
		} catch (NotFoundException e) {
			return false;
		}
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
	
	public boolean getHasPassword() {
		return signin.getUser().getAccount().getHasPassword();
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
	
	private List<ExternalAccountView> getAccountsListForType(OnlineAccountType onlineAccountType) {		
		Set<ExternalAccount> externalAccountsSet = 
            externalAccounts.lookupExternalAccounts(signin.getViewpoint(), signin.getUser(), onlineAccountType);
		
        List<ExternalAccountView> accountsList= new ArrayList<ExternalAccountView>();  		
        ExternalAccountView indifferentAccount = null;     
    
        for (ExternalAccount externalAccount : externalAccountsSet) {
    	    if (externalAccount.getSentiment() == Sentiment.LOVE) {
    	    	if (externalAccount.isMugshotEnabledWithDefault()) {    	
    	    	    // always list the Mugshot enabled account first
    	    		accountsList.add(0, new ExternalAccountView(externalAccount));
    	    	} else { 
    		        accountsList.add(new ExternalAccountView(externalAccount));    
    	    	}
    		} else if (externalAccount.getSentiment() == Sentiment.HATE) {
    	    	// if there is a hated account, we want to return it on its own
    	        // if you hate this account type, you can't have other accounts of this type that you love or
    	        // add new accounts of this type, you can edit the hated account type in place though
    	    	accountsList.clear();
    	    	accountsList.add(new ExternalAccountView(externalAccount));
    		    return accountsList;   		    
    	    } else {
    		    // we don't return here because it is possible to have old indifferent accounts,
    		    // but also have some loved accounts or one hated account
    		    indifferentAccount = new ExternalAccountView(externalAccount);
    	    }		    	    
        }
    
    	if (accountsList.size() > 1) {
    		boolean isFirstLovedAccount = true;
    	    for (ExternalAccountView lovedAccount : accountsList) {
    	    	lovedAccount.setAllowHate(false);
    	    	
    	    	if (!isFirstLovedAccount)
    	    	    lovedAccount.setNewType(false);
    	    	else
    	    		isFirstLovedAccount = false;
    	    }
    	}
    	
    	ExternalAccountView viewForAddingNewAccount = null;
	    // we should only allow the user to edit one indifferent account at a time; since we never delete accounts, this
	    // is a way to allow reusing accounts
        if (indifferentAccount != null) {
        	viewForAddingNewAccount = indifferentAccount;				        	
        } else {
        	viewForAddingNewAccount = new ExternalAccountView(onlineAccountType);
        }
        
        if (accountsList.size() != 0) {
            viewForAddingNewAccount.setAllowHate(false);
            viewForAddingNewAccount.setNewType(false);
        }
        
        accountsList.add(viewForAddingNewAccount);
        
        if (onlineAccountType.getAccountType() != null && onlineAccountType.getAccountType() == ExternalAccountType.FACEBOOK) {
        	// make sure we only list one account for Facebook, because we don't support adding multiple Facebook accounts at the moment
        	return accountsList.subList(0, 1);
        }
        
        return accountsList;
    }

	/**
	 * Returns a list of supported account views; with the ExternalAccount information for the
	 * user filled in for the account types for which the user has accounts.
	 */
	public ListBean<ExternalAccountView> getSupportedAccounts() {
		if (supportedAccountsListBean != null) {
			return supportedAccountsListBean;
		}
			
		List<ExternalAccountView> supportedAccounts = new ArrayList<ExternalAccountView>(); 
		for (ExternalAccountType type : ExternalAccountType.alphabetizedValues()) {
			if (type.isSupported()) {
	            OnlineAccountType onlineAccountType = externalAccounts.getOnlineAccountType(type);
	            supportedAccounts.addAll(getAccountsListForType(onlineAccountType));
			}
		}
		
		supportedAccountsListBean = new ListBean<ExternalAccountView>(supportedAccounts);
		return supportedAccountsListBean;
	}

	public ListBean<ExternalAccountView> getGnomeSupportedAccounts() {
		if (gnomeSupportedAccountsListBean != null) {
			return gnomeSupportedAccountsListBean;
		}
			
		List<ExternalAccountView> supportedAccounts = new ArrayList<ExternalAccountView>(); 
		List<OnlineAccountType> allTypes = externalAccounts.getAllOnlineAccountTypes();
		List<OnlineAccountType> alphabetizedTypes =
			SortUtils.sortCollection(allTypes.toArray(new OnlineAccountType[allTypes.size()]), "getFullName");
		
		for (OnlineAccountType type : alphabetizedTypes) {
			if (type.isSupported()) {
				supportedAccounts.addAll(getAccountsListForType(type));
			}
		}
		
		gnomeSupportedAccountsListBean = new ListBean<ExternalAccountView>(supportedAccounts);
		return gnomeSupportedAccountsListBean;
	}
	
	/**
	 * Returns names and links for user's Amazon reviews page and public wish lists.
	 */
	public List<Pair<String, String>> getAmazonLinks() {
		if (!getExternalAccountSentiment(ExternalAccountType.AMAZON).equalsIgnoreCase(Sentiment.LOVE.name()))
			return null;
		
		String amazonUserId = getExternalAccountHandle(ExternalAccountType.AMAZON);
		
		return amazonUpdater.getAmazonLinks(amazonUserId, false);
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
    }

    public void setFacebookErrorMessage(String facebookErrorMessage) {
    	this.facebookErrorMessage = facebookErrorMessage;  	
    }
    
    public String getFacebookErrorMessage() {
        return facebookErrorMessage;	 	
    }
    
    public String getFacebookAuthToken() {
    	return facebookAuthToken;
    }
    
    public String getFacebookApiKey() {
        return facebookSystem.getApiKey();
    }
    
    public List<XmppResource> getClaimedXmppResources() {
    	return claimVerifier.getPendingClaimedResources(signin.getUser(), XmppResource.class);
    }
}
