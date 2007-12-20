package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookResource;
import com.dumbhippo.persistence.UserNameChangedRevision;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.RevisionControl;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.UserDMO;
import com.dumbhippo.services.FacebookWebServices;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.WebEJBUtil;
import com.facebook.api.FacebookParam;
import com.facebook.api.FacebookSignatureUtil;

public class FacebookSigninServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FacebookAddServlet.class);
	
	static final long serialVersionUID = 1;
	
	// how long to wait on the web services calls
	static protected final int REQUEST_TIMEOUT = 1000 * 12;
	
	private Configuration config;
	
	@Override
	public void init() {
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}	
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException, RetryException {
		String redirectUrl = "/facebook-welcome";
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
		if (secret == null) {
			errorMessage = "We could not verify Facebook information due to a missing secret key we should share with Facebook.";   
		} else {        
	        boolean signatureValid = FacebookSignatureUtil.verifySignature(facebookParams, secret);
	        if (!signatureValid) {
				errorMessage = "We could not verify Facebook information because the signature supplied for Facebook parameters was not valid.";     	
	        } else {
	            AccountSystem accounts = WebEJBUtil.defaultLookup(AccountSystem.class);
	        	IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
	        	FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
	        	RevisionControl revisionControl = WebEJBUtil.defaultLookup(RevisionControl.class);
	        	String facebookUserId = facebookParams.get(FacebookParam.USER.toString()).toString(); 
	            try {
			        FacebookResource res = identitySpider.lookupFacebook(facebookUserId);
			        AccountClaim ac = res.getAccountClaim();
			        if (ac != null) {
			        	Client client = accounts.authorizeNewClient(ac.getOwner().getAccount(), SigninBean.computeClientIdentifier(request));
			    		HttpSession sess = request.getSession(false);
			    		if (sess != null)
			    			sess.invalidate();
			    		SigninBean.initializeAuthentication(request, response, client);
			    		ac.getOwner().getAccount().setPublicPage(true);
			    		// set a better name for a Facebook user, since now the user can edit it, and
			    		// we won't be needing to get it from Facebook again
			    		if (ac.getOwner().getNickname().contains("Facebook user")) {
				            FacebookWebServices ws = new FacebookWebServices(REQUEST_TIMEOUT, config);
			                FacebookAccount facebookAccount = facebookTracker.getFacebookAccount(facebookUserId);                    
			                if (facebookAccount != null) {
			                    String name = ws.getName(facebookAccount);
			                    if (name.trim().length() > 0) {
			                    	name = name.trim();
			                    	ac.getOwner().setNickname(name);
			            		    DataService.currentSessionRW().changed(UserDMO.class, ac.getOwner().getGuid(), "name");
			            		    revisionControl.persistRevision(new UserNameChangedRevision(ac.getOwner(), new Date(), name));
			                    }
			                }		    		
			    		}
			    		return redirectToNextPage(request, response, "/account", null);
			        } else {
		            	errorMessage = "FacebookResource for " + facebookUserId + " was not claimed by any user.";   	            	
			        }			        
	            } catch (NotFoundException e) {
	            	errorMessage = "We could not find a FacebookResource for Facebook user " + facebookUserId + ".";   	            	
	            }
	        }
		}
		
		if (errorMessage != null) {
			logger.error("Will show the following error to the user: {}", errorMessage);
			redirectUrl = redirectUrl + "?error_message=" + errorMessage;	  
		} else {
			logger.error("Redirecting to a facebook-welcome page for unknown reason!");
		}
		
		response.sendRedirect(redirectUrl);
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
