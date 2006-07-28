package com.dumbhippo.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.NotFoundException;
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
	private static final String CLIENT_ID_KEY = "dumbhippo.signedInClient";
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
			Account account = null;
			
			// We can't cache the User or Account objects in session scope since we won't notice if 
			// there are changes to them. So we cache SigninBean only with request scope and put 
			// only the guid in session scope. This also nicely dodges threading issues since 
			// Guid is immutable.
			userGuid = (Guid) request.getSession().getAttribute(USER_ID_KEY);
					
			if (userGuid == null) {
				try {
					account = CookieAuthentication.authenticate(request);
					userGuid = account.getOwner().getGuid();
					storeGuid(request.getSession(), userGuid);
					logger.debug("storing authenticated user {} in session", account.getOwner());
				} catch (BadTastingException e) {
					logger.warn("Cookie was malformed", e);
					userGuid = null;
				} catch (NotLoggedInException e) {
					logger.debug("Cookie not valid: {}", e.getMessage());
					userGuid = null;
				}
			} else {
				AccountSystem accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);
				try {
					account = accountSystem.lookupAccountByOwnerId(userGuid);
				} catch (NotFoundException e) {
					logger.warn("Couldn't load account for stored authenticated user");
				}
			}
			
			if (account != null) {
				if (!account.getHasAcceptedTerms() || account.isDisabled() || account.isAdminDisabled())
					result = new DisabledSigninBean(account);
				else
					result = new UserSigninBean(account);
			} else
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
	
	public static void storeGuid(HttpSession session, Guid guid) {
		session.setAttribute(USER_ID_KEY, guid);		
	}

	/**
	 * Store authentication information on the session and/or in a persistant client
	 * cookie after initial authentication of a client
	 * 
	 * @param request request object
	 * @param response response object
	 * @param client Client object that the user has authenticated against
	 * @return a string that indicates a good default location for the state of the user;
	 *    for example, if the account is disabled, we return "/we-miss-you" where the
	 *    user is allowed to re-enable their account
	 */
	public static String initializeAuthentication(HttpServletRequest request, HttpServletResponse response, Client client) {
		Account account = client.getAccount();
		User user = account.getOwner();
		if (!account.isDisabled() && !account.isAdminDisabled() && account.getHasAcceptedTerms()) {
			setCookie(response, user.getGuid(), client.getAuthKey());
		} else {
			SigninBean.storeGuid(request.getSession(), user.getGuid());
			request.getSession().setAttribute(CLIENT_ID_KEY, client.getId());		
		}		

		if (account.isDisabled())
			return "/we-miss-you";
		else if (!account.getHasAcceptedTerms())
			return "/download?acceptMessage=true";
		else
			return "/";
	}
	
	/**
	 * Update stored authentication information on the session and/or in client cookies
	 * after a possible change in the enablement of an account
	 * @param request request object
	 * @param response response object
	 */
	public static void updateAuthentication(HttpServletRequest request, HttpServletResponse response) {
		AccountSystem accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);
		Guid userId = (Guid)request.getSession().getAttribute(USER_ID_KEY);
		Long clientId = (Long)request.getSession().getAttribute(CLIENT_ID_KEY);
		if (userId != null && clientId != null) {
			try {
				Client client = accountSystem.getExistingClient(userId, clientId);
				Account account = client.getAccount();
				if (!account.isDisabled() && !account.isAdminDisabled() && account.getHasAcceptedTerms())
					setCookie(response, userId, client.getAuthKey());
				else
					unsetCookie(response);
			} catch (NotFoundException e) {
				// Client must have been deleted since we first authorized the session, do nothing
			}
		}
	}
	
	private static void setCookie(HttpServletResponse response, Guid personId, String authKey) {
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		String host = config.getBaseUrl().getHost();
		
		LoginCookie loginCookie = new LoginCookie(host, personId.toString(), authKey);
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
	public abstract boolean isDisabled();
	public abstract boolean getNeedsTermsOfUse();
	
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
