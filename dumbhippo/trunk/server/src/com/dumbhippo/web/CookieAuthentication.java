package com.dumbhippo.web;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;
import com.dumbhippo.web.LoginCookie.BadTastingException;

public class CookieAuthentication {
	
	@SuppressWarnings("serial")
	public static class NotLoggedInException extends Exception {

		public NotLoggedInException(String string) {
			super(string);
		}
	}
	
	public static Person authFromFacesContext() throws BadTastingException, NotLoggedInException {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest req = (HttpServletRequest) ctx.getRequest();		
		return authenticate(req);		
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
	public static Person authenticate(HttpServletRequest request) throws BadTastingException, NotLoggedInException {
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
	public static Person authenticate(LoginCookie loginCookie) throws BadTastingException, NotLoggedInException {

		if (loginCookie == null) {
			throw new NotLoggedInException("No login cookie set");
		}

		return authenticate(loginCookie.getPersonId(), loginCookie.getAuthKey());
	}	
	
	/**
	 * Try to login from a personId/authKey.
	 * 
	 * @param personId
	 *            the person ID
	 * @param authKey
	 *            the auth key
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public static Person authenticate(String personId, String authKey) throws BadTastingException, NotLoggedInException {
		// This should be one of the only classes in web tier 
		// using account system
		AccountSystem accountSystem = WebEJBUtil.uncheckedDefaultLookup(AccountSystem.class);
		IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		Person person;		
		try {
			person = identitySpider.lookupGuidString(Person.class, personId);
		} catch (GuidNotFoundException e) {
			throw new BadTastingException("Cookie had unknown person ID '" + personId + "'");
		} catch (ParseException e) {
			throw new BadTastingException("Cookie had malformded person ID '" + personId + "'");
		}
		if (!accountSystem.checkClientCookie(person, authKey)) {
			throw new BadTastingException("Cookie had invalid or expired auth key in it '" + authKey + "'");
		}
		return person;
	}
}
