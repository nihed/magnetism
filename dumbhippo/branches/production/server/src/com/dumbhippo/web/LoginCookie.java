package com.dumbhippo.web;

import javax.servlet.http.Cookie;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

/**
 * Represents the persistent login information stored on a client for a
 * particular user.
 * 
 * @author walters
 * 
 */
public class LoginCookie {
	
	static final String COOKIE_NAME = "auth";
	static private final String COOKIE_HOST_HEADER = "host=";
	static private final String COOKIE_NAME_HEADER = "name=";
	static private final String COOKIE_PASSWORD_HEADER = "password=";
	
	private transient Cookie cachedCookie = null;
	
	private String host;
	
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
	static public class BadTastingException extends Exception {
		private static final long serialVersionUID = 0L;

		public BadTastingException(String string) {
			super(string);
		}
	};

	private void validateHex(String hexStr) throws BadTastingException {
		if (hexStr.length() % 2 != 0)
			throw new BadTastingException("invalid hex string length " + hexStr.length());
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
		
		if (host == null) {
			throw new RuntimeException("Trying to write a cookie without setting the host");
		}

		val.append(COOKIE_HOST_HEADER);
		val.append(host);
		
		val.append('&');

		try {
			Guid.validate(personId);
		} catch (ParseException e) {
			throw new RuntimeException("Bug! ID of account owner is invalid", e);
		}
		val.append(COOKIE_NAME_HEADER);
		val.append(personId);
		
		val.append('&');
		
		try {
			validateHex(authKey);
		} catch (BadTastingException e) {
			throw new RuntimeException("Bug! auth key is invalid hex?", e);
		}
		val.append(COOKIE_PASSWORD_HEADER);
		val.append(authKey);
		
		// Empty value, we set it later
		Cookie cookie = new Cookie(COOKIE_NAME, "");
		// We have some characters in our cookie like '=' that are
		// theoretically only allowed in Version 1 cookies, but 
        // experimentally IE can't (???) handle the Version 1 Max-Age, 
		// parameter so we stick to Version 0 cookies with Expires and 
        // assume that all the clients we care about will parse them
        // correctly even with the suspicious characters.
		//
		// cookie.setVersion(1);
		cookie.setValue(val.toString());
		cookie.setPath("/");
		// 5 years
		cookie.setMaxAge(5 * 365 * 24 * 60 * 60);
		return cookie;
	}

	public Cookie getCookie() {
		if (cachedCookie == null) {
			cachedCookie = computeCookie();
		}
		return cachedCookie;
	}

	static public Cookie newDeleteCookie() {
		Cookie cookie = new Cookie(COOKIE_NAME, "deleted");
		// 0 max age means nuke the cookie
		cookie.setMaxAge(0);
		cookie.setPath("/");
		return cookie;
	}
	
	public LoginCookie(Cookie cookie) throws BadTastingException {
		computePersonIdLogin(cookie);
	}

	public LoginCookie(String host, String personId, String authKey) {
		this.host = host;
		this.personId = personId;
		this.authKey = authKey;
	}
	
	private void computePersonIdLogin(Cookie cookie) throws BadTastingException {
		if (!cookie.getName().equals(COOKIE_NAME)) {
			throw new BadTastingException("invalid cookie name \"" + cookie.getName() 
					+ "\", expected \"" + COOKIE_NAME + "\"");
		}
		
		String val = cookie.getValue();
		
		host = null;
		personId = null;
		authKey = null;

		int pos = 0;
		while (pos < val.length()) {
			int nextIndex = val.indexOf('&', pos);
			if (nextIndex < 0)
				nextIndex = val.length();
			
			if (val.startsWith(COOKIE_HOST_HEADER, pos)) {
				host = val.substring(pos + COOKIE_HOST_HEADER.length(), nextIndex);
			} else if (val.startsWith(COOKIE_NAME_HEADER, pos)) {
				personId = val.substring(pos + COOKIE_NAME_HEADER.length(), nextIndex);
				try {
					Guid.validate(personId);
				} catch (ParseException e) {
					throw new BadTastingException("Bad personId");
				}
			} else if (val.startsWith(COOKIE_PASSWORD_HEADER, pos)) {
				authKey = val.substring(pos + COOKIE_PASSWORD_HEADER.length(), nextIndex);
				validateHex(authKey);
			}
			
			pos = nextIndex + 1;
		}
			
		if (personId == null)
			throw new BadTastingException("invalid cookie value, missing " + COOKIE_NAME_HEADER);

		if (authKey == null)
			throw new BadTastingException("invalid cookie value, missing " + COOKIE_PASSWORD_HEADER);		
	}

	public String getHost() {
		return host;
	}
	
	public String getAuthKey() {
		return authKey;
	}

	public String getPersonId() {
		return personId;
	}
	
	@Override
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
	
	@Override
	public int hashCode() {
		int result = authKey.hashCode();
		result = 37 * result + personId.hashCode();
		return result;
	}
}
