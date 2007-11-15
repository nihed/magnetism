package com.dumbhippo.web.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.FacebookSystemException;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;
import com.facebook.api.FacebookParam;
import com.facebook.api.FacebookSignatureUtil;

public class FacebookAddServlet extends AbstractServlet {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FacebookAddServlet.class);
	
	static final long serialVersionUID = 1;
	
	// how long to wait on the Facebook API call
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
		
		if (secret == null) {
			String errorMessage = "We could not verify Facebook information due to a missing secret key we should share with Facebook.";   
			redirectUrl = redirectUrl + "?error_message=" + errorMessage;
			logger.warn("Facebook secret is not set, can't verify new  additions of the Facebook application.");
		} else {        
	        boolean signatureValid = FacebookSignatureUtil.verifySignature(facebookParams, secret);
	        if (!signatureValid) {
				String errorMessage = "We could not verify Facebook information because the signature supplied for Facebook parameters was not valid.";   
				redirectUrl = redirectUrl + "?error_message=" + errorMessage;	        	
	        } else {
	            FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
			    SigninBean signin = SigninBean.getForRequest(request);	
			    if (signin instanceof UserSigninBean) {
	    	        try {
	    	            // set the session key and the user id for the signed in user, set their facebookApplicationEnabled flag to true  
	    	            String sessionKey = facebookParams.get(FacebookParam.SESSION_KEY.toString()).toString();
	    	            String facebookUserId = facebookParams.get(FacebookParam.USER.toString()).toString(); 
	    	            facebookTracker.updateOrCreateFacebookAccount(((UserSigninBean)signin).getViewpoint(), sessionKey, facebookUserId, true);
	    	        } catch (FacebookSystemException e) {
	                    redirectUrl = redirectUrl + "?error_message=" + e.getMessage();		
	    	        }
		        }
	        }
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
