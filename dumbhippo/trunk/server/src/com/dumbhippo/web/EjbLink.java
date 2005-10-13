package com.dumbhippo.web;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AbstractEjbLink;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.BanFromWebTier;
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

	static private Log logger = LogFactory.getLog(EjbLink.class);

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
		
		if (clazz.isAnnotationPresent(BanFromWebTier.class)) {
			throw new IllegalArgumentException("Class " + clazz.getCanonicalName() + " is banned from the web tier");
		}
		
		T obj = uncheckedNameLookup(clazz);

		if (obj == null)
			return null;
		
		/*
		 for (Class i : obj.getClass().getInterfaces()) {
			 logger.info("  implements " + i.getCanonicalName());
			 for (Annotation a : i.getAnnotations()) {
				 logger.info("     with annotation " + a.getClass().getCanonicalName());
			 }
		 }
		 */
		 
		if (obj instanceof LoginRequired) {
			logger.info("  logging this object in");
			
			LoginRequired loginRequired = (LoginRequired) obj;
			
			if (personId == null) {
				throw new IllegalStateException("To use EJB " + clazz.getCanonicalName() + " you must log the user in"); 
			} else {
				loginRequired.setLoggedInUserId(personId);
			}
		} else {
			logger.info("  object does not need login");
		}

		// create our own proxy, though since JBoss does this anyway it may be kind of 
		// pointless. For now it just checks the @BanFromWebTier annotation on methods
		// and removes the JBossProxy interface in effect.
		T proxy = (T) InterfaceFilterProxyFactory.newProxyInstance(obj, clazz);
		
		return proxy;
	}

	/**
	 * Look for login cookie and find corresponding account; throw exception if
	 * login fails.
	 * @throws BadTastingException
	 * @throws NotLoggedInException
	 */
	public void attemptLoginFromFacesContext() throws BadTastingException, NotLoggedInException {
		ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
		HttpServletRequest request = (HttpServletRequest) ctx.getRequest();

		if (request == null) {
			throw new IllegalStateException("No current HTTP request to get login cookie from");
		}
		
		attemptLogin(request);
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
