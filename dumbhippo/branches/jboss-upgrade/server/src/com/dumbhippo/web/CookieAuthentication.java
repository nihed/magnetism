package com.dumbhippo.web;

import java.net.URL;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.UnauthorizedException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class CookieAuthentication {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(CookieAuthentication.class);
	
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
	public static Account authenticate(HttpServletRequest request) throws BadTastingException, NotLoggedInException {
		LoginCookie loginCookie = null;
		Cookie[] cookies = request.getCookies();
		
		Configuration config = WebEJBUtil.defaultLookup(Configuration.class);
		URL url = config.getBaseUrl();
		String host = url.getHost();
		
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (c.getName().equals(LoginCookie.COOKIE_NAME)) {
					// An old cookie might not have the host specified in it;
					// treat that as matching any server host
					LoginCookie possibleCookie = new LoginCookie(c);
					String cookieHost = possibleCookie.getHost();
					if (cookieHost == null || cookieHost.equals(host)) {
						//logger.debug("Found login cookie");
						loginCookie = possibleCookie;
						if (cookieHost != null)
							break;
					}
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
	public static Account authenticate(LoginCookie loginCookie) throws BadTastingException, NotLoggedInException {

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
	public static Account authenticate(String userId, String authKey) throws BadTastingException, NotLoggedInException {
		// This should be one of the only classes in web tier 
		// using account system
		AccountSystem accountSystem = WebEJBUtil.uncheckedDefaultLookup(AccountSystem.class);
		try {
			Guid guid = new Guid(userId);
			return accountSystem.checkClientCookie(guid, authKey);
		} catch (NotFoundException e) {
			throw new NotLoggedInException("Cookie had unknown person ID '" + userId + "'");
		} catch (ParseException e) {
			throw new BadTastingException("Cookie had malformed person ID '" + userId + "'");
		} catch (UnauthorizedException e) {
			throw new NotLoggedInException("Cookie had invalid or expired auth key in it '" + authKey + "'");
		}
	}
}
