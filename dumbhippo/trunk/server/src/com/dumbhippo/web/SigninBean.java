package com.dumbhippo.web;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.IdentitySpider;
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
public class SigninBean implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = GlobalSetup.getLogger(SigninBean.class);
	
	private static final String USER_ID_KEY = "dumbhippo.signedInUserId";
	private static final String SIGNIN_BEAN_KEY = "signin";
	
	private Guid userGuid;
	private User user; // lazily initialized
	private Boolean disabled; // lazily initialized
	private Boolean musicSharingEnabled; // lazily initialized
	
	private SigninBean(HttpServletRequest request) {

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
				logger.debug("Cookie was malformed", e);
				userGuid = null;
				user = null;
			} catch (NotLoggedInException e) {
				logger.debug("Cookie not valid", e);
				userGuid = null;
				user = null;
			}
		} else {
			logger.debug("loaded authenticated user ID {} from session", userGuid);
		}
		
		logger.debug("storing SigninBean on request, valid = {}", isValid());
		request.setAttribute(SIGNIN_BEAN_KEY, this);
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
		
		if (result == null)
			result = new SigninBean(request);
				
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

	public boolean isValid() {
		return userGuid != null;
	}
	
	public User getUser() {
		if (userGuid != null && user == null) {
			IdentitySpider spider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			try {
				user = spider.lookupGuid(User.class, userGuid);
			} catch (NotFoundException e) {
				user = null;
				userGuid = null;
			}
		}

		return user;
	}
	
	public String getUserId() {
		if (userGuid != null)
			return userGuid.toString();
		else
			return null;
	}
	
	public Guid getUserGuid() {
		return userGuid;
	}
	
	public Viewpoint getViewpoint() {
		// FIXME: would it be better to cache the result? 
		return new Viewpoint(getUser());
	}
	
	public boolean isDisabled() {
		if (disabled == null) {
			IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			disabled = Boolean.valueOf(identitySpider.getAccountDisabled(getUser()));
			logger.debug("AccountPage loaded disabled = {}", disabled);
		}
		return disabled;
	}
	
	public boolean isMusicSharingEnabled() {
		if (musicSharingEnabled == null) {
			IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			musicSharingEnabled = Boolean.valueOf(identitySpider.getMusicSharingEnabled(getUser()));
		}
		return musicSharingEnabled;
	}
}
