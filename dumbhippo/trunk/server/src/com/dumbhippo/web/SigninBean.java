package com.dumbhippo.web;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.Site;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountType;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.views.Viewpoint;
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
	private static final String SITE_KEY = "site";
	
	private String server;
	private Site site;
	
	// The Site is stored with its own SITE_KEY instead of as part of the 
	// SigninBean because sometimes we need the site as part of signing
	// in, e.g. to put the host name in our login cookie.
	public static Site getSiteForRequest(HttpServletRequest request) {
		Site site;
		try {
			site = (Site) request.getAttribute(SITE_KEY);
		} catch (ClassCastException e) {
			logger.error("Value of {} wasn't a Site value", SITE_KEY);
			site = null;
		}
		
		if (site == null) {
			site = Site.MUGSHOT;
			URL requestUrl = null;
			try {
				String requestUrlString = request.getRequestURL().toString();
				// logger.debug("Request url '{}'", requestUrlString);
				requestUrl = new URL(requestUrlString);
			} catch (MalformedURLException e) {
				logger.warn("Request contained malformed URL '{}': {}", request.getRequestURL().toString(), e);
			}
			if (requestUrl != null) {
				Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
				String host = config.getBaseUrlGnome().getHost();
				if (requestUrl.getHost().equals(host)) {
					site = Site.GNOME;
				}
			}
			
			request.setAttribute(SITE_KEY, site);
		}
		
		return site;
	}
	
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
			Site site = getSiteForRequest(request);
			
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
					logger.debug("storing cookie-authenticated user {} in session", account.getOwner());
				} catch (BadTastingException e) {
					logger.warn("Cookie was malformed", e);
					userGuid = null;
				} catch (NotLoggedInException e) {
					logger.debug("Cookie not valid: {}", e.getMessage());
					userGuid = null;
				}
				
				if (userGuid == null) {
					// If we fail, try http auth (which the browser should only have
					// attempted if we sent SC_UNAUTHORIZED, which we only do from 
					// the DAV servlet)
					try {
						account = HttpAuthentication.authenticate(request);
						userGuid = account.getOwner().getGuid();
						storeGuid(request.getSession(), userGuid);
						logger.debug("storing http-authenticated user {} in session", account.getOwner());						
					} catch (NotLoggedInException e) {
						logger.debug("Http auth not valid: {}", e.getMessage());
						userGuid = null;
					}
				}

			} else {
				AccountSystem accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);
				try {
					account = accountSystem.lookupAccountByOwnerId(userGuid);					
				} catch (NotFoundException e) {
					logger.warn("Couldn't load account for stored authenticated user");
				}
			}
			
			if (account != null)
				result = new UserSigninBean(account, site);
			else
				result = new AnonymousSigninBean(site);

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
	 * Store authentication information on the session and/or in a persistent client
	 * cookie after initial authentication of a client
	 * 
	 * @param request request object
	 * @param response response object
	 * @param client Client object that the user has authenticated against
	 * @return a string that indicates a good default location for the state of the user;
	 *    (at the moment, always "/", the account status bar will take care of any
	 *    necessary initial actions that the user might need to reenable the account, etc.)
	 */
	public static String initializeAuthentication(HttpServletRequest request, HttpServletResponse response, Client client) {
		Account account = client.getAccount();
		User user = account.getOwner();
		Site site = getSiteForRequest(request);
		
		// If the user is ever authenticated at the GNOME site, we can transfer their
		// account to be a GNOME account. 
		// If the user is ever authenticated at the Mugshot site and they have accepted
		// terms of use, we should make their page public.
		if (site.getAccountType() == AccountType.GNOME)
			account.setAccountType(AccountType.GNOME);
		else if (site.getAccountType() == AccountType.MUGSHOT && account.getHasAcceptedTerms())
			account.setPublicPage(true);
		
		if (account.isActive()) {
			setCookie(site, response, user.getGuid(), client.getAuthKey());
		} else {
			SigninBean.storeGuid(request.getSession(), user.getGuid());
			request.getSession().setAttribute(CLIENT_ID_KEY, client.getId());
			setAuthenticatedCookie(site, response);
		}		

		return "/";
	}
	
	/** 
	 * Initializes auth in a context where we can't or have no need to set the cookie, 
	 * right now used with DAV and HTTP auth
	 * 
	 * @param request
	 * @param client
	 */
	public static void initializeAuthenticationNoCookie(HttpServletRequest request, Client client) {
		Account account = client.getAccount();
		User user = account.getOwner();
		Site site = getSiteForRequest(request);
		
		// If the user is ever authenticated at the GNOME site, we can transfer their
		// account to be a GNOME account. 
		// If the user is ever authenticated at the Mugshot site and they have accepted
		// terms of use, we should make their page public.
		// TODO: make sure this method doesn't get called in situations when the user is
		// not using mugshot.org explicitly; this only gets called from HttpAuthentication
		// as "a hack to use DAV with apps that don't use the browser cookies"
		if (site.getAccountType() == AccountType.GNOME)
			account.setAccountType(AccountType.GNOME);
		else if (site.getAccountType() == AccountType.MUGSHOT && account.getHasAcceptedTerms())
			account.setPublicPage(true);
		
		if (account.isActive()) {
			;
		} else {
			SigninBean.storeGuid(request.getSession(), user.getGuid());
			request.getSession().setAttribute(CLIENT_ID_KEY, client.getId());
		}
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
				Site site = getSiteForRequest(request);
				
				// If the user is ever authenticated at the GNOME site, we can transfer their
				// account to be a GNOME account. 
				// If the user is ever authenticated at the Mugshot site and they have accepted
				// terms of use, we should make their page public.
				// TODO: make sure this method doesn't get called in situations when the user is
				// not using mugshot.org explicitly; this is only called from HttpMethodsServlet2
				if (site.getAccountType() == AccountType.GNOME)
					account.setAccountType(AccountType.GNOME);
				else if (site.getAccountType() == AccountType.MUGSHOT && account.getHasAcceptedTerms())
					account.setPublicPage(true);
				
				if (account.isActive())
					setCookie(site, response, userId, client.getAuthKey());
				else {
					unsetCookie(response);
					setAuthenticatedCookie(site, response);
				}
			} catch (NotFoundException e) {
				// Client must have been deleted since we first authorized the session, do nothing
			}
		}
	}
	
	private static void setCookie(Site site, HttpServletResponse response, Guid personId, String authKey) {
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		String host = config.getBaseUrlObject(site).getHost();
		
		LoginCookie loginCookie = new LoginCookie(host, personId.toString(), authKey);
		response.addCookie(loginCookie.getCookie());
		logger.debug("Set cookie for personId = {} authKey = {}", personId, authKey);
	}
	
	// The authenticated= cookie is a sesssion cookie we set that says that the user
	// has authenticated, but unlike auth= doesn't include the user's credentials.
	// This is set in the "signed in but account is not active" state and used
	// by  by our caching infrastructure to know that the user shouldn't be served
	// anonymous cached content.
	private static void setAuthenticatedCookie(Site site, HttpServletResponse response) {
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		String host = config.getBaseUrlObject(site).getHost();

		response.addCookie(LoginCookie.newAuthenticatedCookie(host));
		logger.debug("Set authenticated cookie");
	}

	public static void unsetCookie(HttpServletResponse response) {
		response.addCookie(LoginCookie.newDeleteCookie());
		logger.debug("Unset auth cookie");
	}
	
	protected SigninBean(Site site) {
		this.site = site;
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
			String url = config.getBaseUrl(site);
			// if you get this exception, should probably just add new 
			// config props for host and port
			if (!url.startsWith("http://") || url.endsWith("/"))
				throw new RuntimeException("FIXME need to be smarter");
			server = url.substring("http://".length());
		}
		return server;
	}
	
	public Site getSite() {
		return site;
	}
	
	/** Are we signed in? returns false if anonymous */
	public abstract boolean isValid();
	/** Are we signed in for a user that isn't disabled or in need of Terms-of-Use acceptance? */
	public abstract boolean isActive();
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
