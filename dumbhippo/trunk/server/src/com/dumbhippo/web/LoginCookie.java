package com.dumbhippo.web;

import javax.servlet.http.Cookie;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;

/**
 * Represents the persistent login information stored on a client for a
 * particular user.
 * 
 * @author walters
 * 
 */
public class LoginCookie {
	private Cookie cookie;

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

	private void computeCookie(HippoAccount acct, Client client) {
		StringBuilder val;
		val = new StringBuilder();
		String ownerId = acct.getOwner().getId();
		val.append(ownerId);
		try {
			validateHex(ownerId);
		} catch (BadTastingException e) {
			throw new RuntimeException(e);
		}
		val.append('/');
		String authKey = client.getAuthKey();
		try {
			validateHex(authKey);
		} catch (BadTastingException e) {
			throw new RuntimeException(e);
		}
		val.append(authKey);
		cookie = new Cookie("auth", val.toString());
		cookie.setMaxAge(5 * 365 * 24 * 60 * 60);
	}

	public LoginCookie(HippoAccount acct, Client client) {
		computeCookie(acct, client);
	}

	public Cookie getCookie() {
		return cookie;
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
}
