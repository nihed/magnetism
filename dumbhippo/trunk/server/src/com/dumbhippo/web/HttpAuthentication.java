package com.dumbhippo.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;

import com.dumbhippo.Base64;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.server.HumanVisibleException;
import com.dumbhippo.server.SigninSystem;

public class HttpAuthentication {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(HttpAuthentication.class);
	
	/**
	 * Look for http auth header and find corresponding account; throw exception if
	 * login fails.
	 * 
	 * @param request
	 *            the http request
	 * @throws NotLoggedInException
	 */
	public static Account authenticate(HttpServletRequest request) throws NotLoggedInException {
		// this is not exactly intended for user visibility, it's just a hack to use DAV 
		// with apps that don't use the browser cookies. The username is the GUID and the 
		// password is the password someone set on the site.
		
		String auth = request.getHeader("Authorization");
		if (auth == null)
			throw new NotLoggedInException("No Authorization header");
	
		// don't debug log the auth header, since it contains 
		// password
		logger.debug("Authorization header present");
		
		if (!auth.toUpperCase().startsWith("BASIC "))
			throw new NotLoggedInException("Don't understand HTTP auth mechanisms other than BASIC");
		
		String userPasswordEncoded = auth.substring("BASIC ".length());
		String userPassword = Base64.decode(userPasswordEncoded);
		
		if (userPassword.length() < Guid.STRING_LENGTH + 1)
			throw new NotLoggedInException("username is too short to be valid");
		
		String user = userPassword.substring(0, Guid.STRING_LENGTH);
		
		Guid guid;
		try {
			guid = new Guid(user);
		} catch (ParseException e) {
			throw new NotLoggedInException("invalid user guid: '" + user + "': " + e.getMessage(), e);
		}

		String password = userPassword.substring(Guid.STRING_LENGTH + 1);
		
		SigninSystem signinSystem = WebEJBUtil.defaultLookup(SigninSystem.class);
		
		Client client;
		try {
			client = signinSystem.authenticatePassword(guid, password, SigninBean.computeClientIdentifier(request));
		} catch (HumanVisibleException e) {
			throw new NotLoggedInException(e.getMessage(), e);
		}

		HttpSession sess = request.getSession(false);
		if (sess != null)
			sess.invalidate();

		SigninBean.initializeAuthenticationNoCookie(request, client);
		
		return client.getAccount();
	}
}
