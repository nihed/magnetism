package com.dumbhippo.web;

import javax.servlet.http.Cookie;

/**
 * Represents the persistent login information stored on a client for a
 * particular user.
 * 
 * @author walters
 * 
 */
public class LoginCookie {
	
	static final String COOKIE_NAME = "auth";
	static private final String COOKIE_NAME_HEADER = "name=";
	static private final String COOKIE_PASSWORD_HEADER = "password=";
	
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
	static public class BadTastingException extends Exception {
		private static final long serialVersionUID = 0L;

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

	public LoginCookie(String personId, String authKey) {
		this.personId = personId;
		this.authKey = authKey;
	}
	
	private void computePersonIdLogin(Cookie cookie) throws BadTastingException {
		String val = cookie.getValue();
		if (!cookie.getName().equals(COOKIE_NAME)) {
			throw new BadTastingException("invalid cookie name \"" + cookie.getName() 
					+ "\", expected \"" + COOKIE_NAME + "\"");
		}
		int ampIdx = val.indexOf('&');
		if (ampIdx < 0)
			throw new BadTastingException("invalid cookie value, missing '&'");
		String personIdStr = val.substring(0, ampIdx);
		if (!personIdStr.startsWith(COOKIE_NAME_HEADER))
			throw new BadTastingException("invalid cookie value, missing " + COOKIE_NAME_HEADER);
		personId = personIdStr.substring(COOKIE_NAME_HEADER.length());
		validateHex(personId);
		String authKeyStr = val.substring(ampIdx + 1);
		if (!authKeyStr.startsWith(COOKIE_PASSWORD_HEADER))
			throw new BadTastingException("invalid cookie value, missing " + COOKIE_PASSWORD_HEADER);		
		authKey = authKeyStr.substring(COOKIE_PASSWORD_HEADER.length());
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
}
