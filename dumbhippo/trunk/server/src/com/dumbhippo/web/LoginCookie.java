package com.dumbhippo.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.server.AccountSystem;

/**
 * Represents the persistent login information stored on a client for a
 * particular user.
 * 
 * @author walters
 * 
 */
public class LoginCookie {
	
	static private final String COOKIE_NAME = "auth";
	
	private transient Cookie cachedCookie = null;
	
	private String personId;

	private String authKey;

	/**
	 * Thrown when a cookie is syntactically malformed; for example, bad hex
	 * encoding.
	 * 
	 * @author walters
	 * 
	 */
	@SuppressWarnings("serial")
	public class BadTastingException extends Exception {
		public BadTastingException(String string) {
			super(string);
		}
	};

	private void validateHex(String hexStr) throws BadTastingException {
		if (hexStr.length() % 2 != 0)
			throw new BadTastingException("invalid hex string length");
		for (int i = 0; i < hexStr.length(); i++) {
			char c = hexStr.charAt(i);
			int codepoint = (int) c;
			if (!((codepoint >= '0' && codepoint <= '9')
					|| codepoint >= 'a' && codepoint <= 'f'))
				throw new BadTastingException("Invalid hex string character '" + c + "'");
		}
	}

	private Cookie computeCookie() {
		StringBuilder val;
		val = new StringBuilder();
		try {
			validateHex(personId);
		} catch (BadTastingException e) {
			throw new RuntimeException("Bug! ID of account owner is invalid hex?", e);
		}
		val.append(personId);
		val.append('/');
		try {
			validateHex(authKey);
		} catch (BadTastingException e) {
			throw new RuntimeException("Bug! auth key is invalid hex?", e);
		}
		val.append(authKey);
		Cookie cookie = new Cookie(COOKIE_NAME, val.toString());
		cookie.setMaxAge(5 * 365 * 24 * 60 * 60);
		return cookie;
	}

	public LoginCookie(HippoAccount acct, Client client) {
		personId = acct.getOwner().getId();
		authKey = client.getAuthKey();
	}

	public Cookie getCookie() {
		if (cachedCookie == null) {
			cachedCookie = computeCookie();
		}
		return cachedCookie;
	}

	public LoginCookie(Cookie cookie) throws BadTastingException {
		computePersonIdLogin(cookie);
	}

	private void computePersonIdLogin(Cookie cookie) throws BadTastingException {
		String val = cookie.getValue();
		int slashIdx = val.indexOf('/');
		if (slashIdx < 0)
			throw new IllegalArgumentException("invalid cookie value");
		personId = val.substring(0, slashIdx);
		validateHex(personId);
		authKey = val.substring(slashIdx + 1);
		validateHex(authKey);
	}

	public String getAuthKey() {
		return authKey;
	}

	public String getPersonId() {
		return personId;
	}
	
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof LoginCookie))
			return false;
		LoginCookie otherCookie = (LoginCookie) other;
		if (!authKey.equals(otherCookie.authKey))
			return false;
		if (!personId.equals(otherCookie.personId))
			return false;
		return true;
	}
	
	public int hashCode() {
		int result = authKey.hashCode();
		result = 37 * result + personId.hashCode();
		return result;
	}
	
	/**
	 * Look for login cookie and find corresponding account; return null 
	 * if none is found.
	 * 
	 * @param request the http request
	 * @return account or null
	 * @throws BadTastingException 
	 */
	static public HippoAccount attemptLogin(AccountSystem accountSystem, HttpServletRequest request) throws BadTastingException {
		LoginCookie loginCookie = null;
		Cookie[] cookies = request.getCookies();
		for (Cookie c : cookies) {
			if (c.getName().equals(COOKIE_NAME)) {
				loginCookie = new LoginCookie(c);
				break;
			}
		}
		
		if (loginCookie == null)
			return null;
		
		HippoAccount account = accountSystem.lookupAccountByPersonId(loginCookie.getPersonId());
		
		if (account.checkClientCookie(loginCookie.getAuthKey()))
			return account;
		
		return null;
	}
}
