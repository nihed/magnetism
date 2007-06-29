package com.dumbhippo.web.servlets;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.FacebookSystemException;
import com.dumbhippo.server.FacebookTracker;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.tx.RetryException;
import com.dumbhippo.web.SigninBean;
import com.dumbhippo.web.UserSigninBean;
import com.dumbhippo.web.WebEJBUtil;

public class FacebookVerifyServlet extends AbstractServlet {

	private static final Logger logger = GlobalSetup.getLogger(VerifyServlet.class);
	
	static final long serialVersionUID = 1;
	
	@Override
	protected String wrappedDoGet(HttpServletRequest request, HttpServletResponse response) throws IOException, HumanVisibleException, HttpException, ServletException, RetryException {
		String facebookAuthToken = request.getParameter("auth_token");
		if (facebookAuthToken != null)
			facebookAuthToken = facebookAuthToken.trim();
		
		if (facebookAuthToken == null || facebookAuthToken.equals("")) 
			throw new HttpException(HttpResponseCode.BAD_REQUEST, "Facebook auth_token not provided");

		ServletConfig config = getServletConfig();		
		String redirect = config.getInitParameter("redirect");
		String redirectUrl = "/";
		if (redirect != null && !redirect.equals("home"))
			redirectUrl = redirectUrl + redirect;
		
		redirectUrl = redirectUrl + "?auth_token=" + facebookAuthToken;
		FacebookTracker facebookTracker = WebEJBUtil.defaultLookup(FacebookTracker.class);
		SigninBean signin = SigninBean.getForRequest(request);
		
		if (!(signin instanceof UserSigninBean))
			throw new RuntimeException("this operation requires checking signin.valid first to be sure a user is signed in");
		
    	try {
    	    // request a session key for the signed in user and set it in the database 
    	    facebookTracker.updateOrCreateFacebookAccount(((UserSigninBean)signin).getViewpoint(), facebookAuthToken);
    	} catch (FacebookSystemException e) {
            redirectUrl = redirectUrl + "&error_message=" + e.getMessage();		
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
