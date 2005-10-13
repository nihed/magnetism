package com.dumbhippo.web;

import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AbstractEjbLink;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.LoginRequired;
import com.dumbhippo.web.LoginCookie.BadTastingException;

/**
 * All usage of InitialContext() and all logging in / authentication should go
 * through here. We don't want potentially misusable EJB interfaces all over the
 * web tier.
 * 
 * The EjbLink is tied to a particular user.
 * 
 * @author hp
 * 
 */
public class EjbLink extends AbstractEjbLink {

	@SuppressWarnings("serial")
	static public class NotLoggedInException extends Exception {
		public NotLoggedInException(String string) {
			super(string);
		}
	}

	static Log logger = LogFactory.getLog(EjbLink.class);

	// if non-null, we are logged in
	private String personId;

	public EjbLink() {
		super();

	}

	private <T> T uncheckedNameLookup(Class<T> clazz) {
		try {
			return super.nameLookup(clazz);
		} catch (NamingException e) {
			e.printStackTrace();
			logger.error("Failed to look up interface " + clazz.getCanonicalName());
			logger.error(e);
			return null;
		}
	}
	/**
	 * This override drops NamingException and automatically uses the
	 * LoginRequired interface if needed. Throws an unchecked exception if you try to 
	 * use an object that requires login, when you aren't logged in.
	 * 
	 * @throws IllegalStateException if not logged in
	 * @see com.dumbhippo.server.AbstractEjbLink#nameLookup(java.lang.Class)
	 */
	@Override
	public <T> T nameLookup(Class<T> clazz) {
		
		T obj = uncheckedNameLookup(clazz);

		if (obj == null)
			return null;
		
		if (obj instanceof LoginRequired) {
			LoginRequired loginRequired = (LoginRequired) obj;
			
			if (personId == null) {
				throw new IllegalStateException("To use EJB " + clazz.getCanonicalName() + " you must log the user in"); 
			} else {
				loginRequired.setLoggedInUserId(personId);
			}
		}

		// Filter out any interfaces that weren't asked for, especially LoginRequired
		T proxy = (T) InterfaceFilterProxyFactory.newProxyInstance(obj, clazz);

		if (proxy instanceof LoginRequired)
			throw new IllegalStateException("Bug! proxy implements LoginRequired");
		
		return proxy;
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
	public void attemptLogin(HttpServletRequest request) throws BadTastingException, NotLoggedInException {
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

		attemptLogin(loginCookie);
	}

	/**
	 * Try to login from a cookie.
	 * 
	 * @param loginCookie
	 *            the http cookie with the login info
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public void attemptLogin(LoginCookie loginCookie) throws BadTastingException, NotLoggedInException {

		if (loginCookie == null) {
			throw new NotLoggedInException("No login cookie set");
		}

		attemptLogin(loginCookie.getPersonId(), loginCookie.getAuthKey());
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
	public void attemptLogin(String personId, String authKey) throws BadTastingException, NotLoggedInException {

		AccountSystem accountSystem = uncheckedNameLookup(AccountSystem.class);
		
		HippoAccount account = accountSystem.lookupAccountByPersonId(personId);

		if (account == null) {
			throw new BadTastingException("Cookie had invalid person ID in it");
		}

		if (!account.checkClientCookie(authKey)) {
			throw new BadTastingException("Cookie had invalid or expired auth key in it");
		}

		// OK !
		this.personId = personId;
	}

	public String getLoggedInUser() {
		return personId;
	}
}
