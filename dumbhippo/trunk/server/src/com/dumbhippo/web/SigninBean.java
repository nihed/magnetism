package com.dumbhippo.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.web.CookieAuthentication.NotLoggedInException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

/**
 * An object that caches the currently logged in user, stored in JSF
 * and the HTTP session.
 * 
 * @author walters
 *
 */
public abstract class SigninBean  {
	private static final Logger logger = GlobalSetup.getLogger(SigninBean.class);
	
	private static final String USER_ID_KEY = "dumbhippo.signedInUserId";
	private static final String SIGNIN_BEAN_KEY = "signin";
	
	private String server;
	
	public static SigninBean getForRequest(HttpServletRequest request) {
		SigninBean result = null;
		
		if (request == null)
			throw new NullPointerException("null request");
		
		try {
			result = (SigninBean) request.getAttribute(SIGNIN_BEAN_KEY);
		} catch (ClassCastException e) {
			logger.error("Value of {} wasn't a SigninBean", SIGNIN_BEAN_KEY);
			result = null;
		}
		
		if (result == null) {
			Guid userGuid = null;
			User user = null;
			
			// We can't cache the User or Account objects in session scope since we won't notice if 
			// there are changes to them. So we cache SigninBean only with request scope and put 
			// only the guid in session scope. This also nicely dodges threading issues since 
			// Guid is immutable.
			userGuid = (Guid) request.getSession().getAttribute(USER_ID_KEY);
					
			if (userGuid == null) {
				try {
					user = CookieAuthentication.authenticate(request);
					userGuid = user.getGuid();
					request.getSession().setAttribute(USER_ID_KEY, userGuid);
					logger.debug("storing authenticated user {} in session", user);
				} catch (BadTastingException e) {
					logger.warn("Cookie was malformed", e);
					userGuid = null;
					user = null;
				} catch (NotLoggedInException e) {
					logger.debug("Cookie not valid: {}", e.getMessage());
					userGuid = null;
					user = null;
				}
			} else {
				logger.debug("loaded authenticated user ID {} from session", userGuid);
			}
			
			if (userGuid != null)
				result = new UserSigninBean(userGuid, user);
			else
				result = new AnonymousSigninBean();

			logger.debug("storing SigninBean on request, valid = {}", result.isValid());
			request.setAttribute(SIGNIN_BEAN_KEY, result);
			
		}
				
		return result;
	}
	
	public static String computeClientIdentifier(HttpServletRequest request) {
		StringBuilder ret = new StringBuilder();
		ret.append(request.getRemoteAddr());
		String agent = request.getHeader("user-agent");
		if (agent != null) {
			ret.append('/');
			ret.append(agent);
		}
		
		return ret.toString();
	}

	public static void setCookie(HttpServletResponse response, String personId, String authKey) {
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		String host = config.getBaseUrl().getHost();
				
		LoginCookie loginCookie = new LoginCookie(host, personId, authKey);
		response.addCookie(loginCookie.getCookie());
		logger.debug("Set cookie for personId = {} authKey = {}", personId, authKey);
	}

	public static void unsetCookie(HttpServletResponse response) {
		response.addCookie(LoginCookie.newDeleteCookie());
		logger.debug("Unset auth cookie");
	}
	
	/** 
	 * Return the server in host:port format suitable for use in a URI,
	 * used for example to generate mugshot: URIs or absolute links.
	 * 
	 * Sets a precedent for random global configuration available from 
	 * SigninBean, for better or worse. Add a "config" bean? This 
	 * is a simple approach for now, we'll see if it gets out of hand.
	 * 
	 * @return the server name
	 */
	public String getServer() {
		if (server == null) {
			Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
			String url = config.getPropertyFatalIfUnset(HippoProperty.BASEURL);
			// if you get this exception, should probably just add new 
			// config props for host and port
			if (!url.startsWith("http://") || url.endsWith("/"))
				throw new RuntimeException("FIXME need to be smarter");
			server = url.substring("http://".length());
		}
		return server;
	}
	
	public abstract boolean isValid();
	public abstract Viewpoint getViewpoint();
	
	/**
	 * Clear any cached objects that might be associated with a particular
	 * Hibernate session and transaction. We use this in RewriteServlet 
	 * to avoid having the User object in the SigninBean be detached from the 
	 * the transaction we create when handling a JSP page. This is a bit
	 * hacky ... with some reorganization it should be possible to
	 * scope the transaction around the lookup of the User as well ...
	 * but it keeps things simple. 
	 */	
	public abstract void resetSessionObjects();
}
