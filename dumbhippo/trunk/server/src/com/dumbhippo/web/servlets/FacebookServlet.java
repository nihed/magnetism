package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.FacebookSystemException;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.WebEJBUtil;
import com.facebook.api.FacebookParam;
import com.facebook.api.FacebookSignatureUtil;

public class FacebookServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FacebookServlet.class);
	
	static final long serialVersionUID = 1;
	
	private Configuration config;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}	
	
	@Override
	protected String wrappedDoPost(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException, RetryException {
		logger.debug("full request is: {}", request.toString());
		logger.debug("context params are:");
        for (Object o : request.getParameterMap().entrySet()) {
        	@SuppressWarnings("unchecked")
            Map.Entry<String, String[]> mapEntry = (Map.Entry<String, String[]>)o;
            logger.debug("{} = {}", mapEntry.getKey(), mapEntry.getValue()[0]);
        }
      
        @SuppressWarnings("unchecked")
        Map<String, CharSequence> facebookParams = FacebookSignatureUtil.extractFacebookParamsFromArray(request.getParameterMap());
        String secret = null;
        try {
        	secret = config.getPropertyNoDefault(HippoProperty.FACEBOOK_SECRET).trim();
			if (secret.length() == 0)
				secret = null;				
		} catch (PropertyNotFoundException e) {
			secret = null;
		}
		
		String errorMessage = null;
		User user = null;
		if (secret == null) {
			errorMessage = "We could not verify Facebook information due to a missing secret key we should share with Facebook.";   
			logger.warn("Facebook secret is not set, can't verify requests from Facebook.");
		} else {        
	        boolean signatureValid = FacebookSignatureUtil.verifySignature(facebookParams, secret);
	        if (!signatureValid) {
				errorMessage = "We could not verify Facebook information because the signature supplied for Facebook parameters was not valid.";           	
	        } else if (facebookParams.get(FacebookParam.ADDED.toString()).toString().equals("1")) {
	        	// get the user who owns the related FacebookResource
	            String sessionKey = facebookParams.get(FacebookParam.SESSION_KEY.toString()).toString();
	            String facebookUserId = facebookParams.get(FacebookParam.USER.toString()).toString(); 
	        	IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	        	try {
	        	    user = identitySpider.lookupUserByFacebookUserId(SystemViewpoint.getInstance(), facebookUserId);
			        if (user != null) {
	    	            try {
	    	            FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
	    	        	// TODO: can change this into updateExistingFacebookAccount
	    	            facebookTracker.updateOrCreateFacebookAccount(new UserViewpoint(user, Site.MUGSHOT), sessionKey, facebookUserId, true);
	    	            } catch (FacebookSystemException e) {
	                        errorMessage = e.getMessage();		
	    	            }
			        }
		        } catch (NotFoundException e) {
		        	//nothing to do
		        }
	        }
		}

		// this returns some code in FBML we'll return for our app page on Facebook
		// it intentionally points to my test server for now
		XmlBuilder xml = new XmlBuilder();		

        xml.appendTextNode("fb:header", "Musgshot");
        xml.appendTextNode("div", "Mugshot allows you and your friends to see your activity from lots of other sites on the internet and automatically puts that in your profile and news feed.",
                           "style", "margin-left:45px; margin-bottom:10px;");
		if (user != null && errorMessage == null) {
			xml.append("Updates to the information below will be reflected in ");
		    xml.appendTextNode("a", "your Mugshot account", "href",
				               "http://dogfood.mugshot.org/person?who=" + user.getId(), "target", "_blank");
		    xml.append(".");
		    xml.appendTextNode("h3", "Photos and Video", "style", "margin-top:10px");
		    xml.openElement("fb:editor", "action", "http://apps.facebook.com/mugshot-test");
		    for (ExternalAccountView externalAccount : getSupportedAccounts(user)) {
			    xml.openElement("fb:editor-custom", "label", externalAccount.getSiteName());
			    
			    if (externalAccount.getExternalAccount().isLovedAndEnabled()) {
			        xml.appendEmptyNode("input", "name", externalAccount.getDomNodeIdName(), "value", externalAccount.getExternalAccount().getAccountInfo());
			    } else {
			    	xml.appendEmptyNode("input", "name", externalAccount.getDomNodeIdName());
			    }
			    xml.appendEmptyNode("br");
			    
			    if (externalAccount.isInfoTypeProvidedBySite()) {
			        xml.append("Enter your ");
			        xml.appendTextNode("a", externalAccount.getSiteName(), 
			        		           "href", externalAccount.getExternalAccount().getSiteLink(), 
			        		           "target", "_blank");
			        xml.append(" " + externalAccount.getSiteUserInfoType() + ".");
			    } else {
			        xml.append("Enter your " + externalAccount.getSiteUserInfoType() + " ");
			        xml.appendTextNode("a", externalAccount.getSiteName(), 
			        		           "href", externalAccount.getExternalAccount().getSiteLink(), 
			        		           "target", "_blank");
			        xml.append(" account.");			    	
			    }
			    
			    if (externalAccount.getExternalAccountType().getSupportType().trim().length() > 0) {
			    	xml.append("Your activity will be updated when you " + externalAccount.getExternalAccountType().getSupportType() + ".");
			    } else {
			    	xml.append("A link to this account will be included in your profile.");
			    }
			    
			    xml.closeElement(); // fb:editor-custom	
		    }
		    xml.openElement("fb:editor-buttonset");
		    xml.appendEmptyNode("fb:editor-button", "value", "Update Info!");
		    xml.appendEmptyNode("fb:editor-cancel");
		    xml.closeElement(); // fb:editor-buttonset
		    xml.closeElement(); // fb:editor 		    
		} else {
		    xml.append("You need to be ");
		    xml.appendTextNode("a", "logged in to Mugshot", "href",
				    "http://dogfood.mugshot.org/account", "target", "_blank");
	    	xml.append(" to be able to verify your Mugshot account.");
		    xml.openElement("form", "action", "http://dogfood.mugshot.org/facebook-add", "target", "_blank", "method", "GET");
		    xml.appendEmptyNode("input", "type", "submit", "value", "Verify My Mugshot Account");
		    xml.closeElement();
		}
		
		response.setContentType("text/html");
		response.getOutputStream().write(xml.getBytes());
		
		return null;
	}	
		
	/**
	 * Returns a list of supported account views.
	 * If the user is not null, the ExternalAccount information for the
	 * user will be filled in for the account types for which the user has accounts.
	 */
	private List<ExternalAccountView> getSupportedAccounts(User user) {
		List<ExternalAccountView> supportedAccounts = new ArrayList<ExternalAccountView>(); 
		ExternalAccountSystem externalAccounts = WebEJBUtil.defaultLookup(ExternalAccountSystem.class);
		for (ExternalAccountType type : ExternalAccountType.alphabetizedValues()) {
			if (type.isSupported()) {
				if (user != null) {
					try {
					    ExternalAccount externalAccount = 
					    	externalAccounts.lookupExternalAccount(new UserViewpoint(user, Site.MUGSHOT), user, type);
					    supportedAccounts.add(new ExternalAccountView(externalAccount));
					} catch (NotFoundException e) {
						supportedAccounts.add(new ExternalAccountView(type));
					}
				} else {
					supportedAccounts.add(new ExternalAccountView(type));
				}
			}
		}
		return supportedAccounts;
	}
	
	@Override
	protected boolean isReadWrite(HttpServletRequest request) {
		// The method is GET, since we need links that the user can just click upon,
		// but they have side effects. This is OK since the links are unique, so 
		// caching won't happen.
		
		return true;
	}
	

	@Override
	protected boolean requiresTransaction(HttpServletRequest request) {
		return true;
	}
}
