package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.FacebookSystemException;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.WebEJBUtil;
import com.facebook.api.FacebookParam;
import com.facebook.api.FacebookSignatureUtil;

public class FacebookServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FacebookAddServlet.class);
	
	static final long serialVersionUID = 1;
	
	private Configuration config;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}	
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException, RetryException {
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
		xml.openElement("p");
		if (user != null && errorMessage == null) {
			xml.append("We already know that you are ");
		    xml.appendTextNode("a", user.getNickname(), "href",
				               "http://dogfood.mugshot.org/person?who=" + user.getId(), "target", "_blank");
		    xml.append(" on Mugshot!");
		} else {
		    xml.append("You need to be ");
		    xml.appendTextNode("a", "logged in to Mugshot", "href",
				    "http://dogfood.mugshot.org/account", "target", "_blank");
	    	xml.append(" to be able to verify your Mugshot account.");
		    xml.openElement("form", "action", "http://dogfood.mugshot.org/facebook-add", "target", "_blank", "method", "GET");
		    xml.appendEmptyNode("input", "type", "submit", "value", "Verify My Mugshot Account");
		    xml.closeElement();
		}
		xml.closeElement();
		
		response.setContentType("text/html");
		response.getOutputStream().write(xml.getBytes());
		
		return null;
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
