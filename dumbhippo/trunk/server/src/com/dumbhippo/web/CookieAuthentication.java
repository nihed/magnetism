package com.dumbhippo.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class CookieAuthentication {
	
	private static final Log logger = GlobalSetup.getLog(CookieAuthentication.class);
	
	@SuppressWarnings("serial")
	public static class NotLoggedInException extends Exception {

		public NotLoggedInException(String string) {
			super(string);
		}
	}
	
	/**
	 * Look for login cookie and find corresponding account; throw exception if
	 * login fails.
	 * 
	 * @param request
	 *            the http request
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public static User authenticate(HttpServletRequest request) throws BadTastingException, NotLoggedInException {
		LoginCookie loginCookie = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (c.getName().equals(LoginCookie.COOKIE_NAME)) {
					loginCookie = new LoginCookie(c);
					break;
				}
			}
		}

		return authenticate(loginCookie);
	}

	/**
	 * Try to login from a cookie.
	 * 
	 * @param loginCookie
	 *            the http cookie with the login info
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public static User authenticate(LoginCookie loginCookie) throws BadTastingException, NotLoggedInException {

		if (loginCookie == null) {
			throw new NotLoggedInException("No login cookie set");
		}

		return authenticate(loginCookie.getPersonId(), loginCookie.getAuthKey());
	}	
	
	/**
	 * Try to login from a personId/authKey.
	 * 
	 * @param userId
	 *            the person ID
	 * @param authKey
	 *            the auth key
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public static User authenticate(String userId, String authKey) throws BadTastingException, NotLoggedInException {
		// This should be one of the only classes in web tier 
		// using account system
		AccountSystem accountSystem = WebEJBUtil.uncheckedDefaultLookup(AccountSystem.class);
		IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		User user;		
		try {
			user = identitySpider.lookupGuidString(User.class, userId);
			logger.debug("Loaded new user in authenticate(): " + user);
		} catch (NotFoundException e) {
			throw new BadTastingException("Cookie had unknown person ID '" + userId + "'");
		} catch (ParseException e) {
			throw new BadTastingException("Cookie had malformed person ID '" + userId + "'");
		}
		if (!accountSystem.checkClientCookie(user, authKey)) {
			throw new BadTastingException("Cookie had invalid or expired auth key in it '" + authKey + "'");
		}
		return user;
	}
}
