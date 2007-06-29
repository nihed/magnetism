package com.dumbhippo.web;

import javax.servlet.http.Cookie;

import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.Account;

import junit.framework.TestCase;

public class LoginCookieTest extends TestCase {

	public void testCookieRoundtrip() throws Exception {
		User h = new User();
		Account acct = new Account(h);
		Client c = new Client(acct);
		acct.authorizeNewClient(c);
		String host = "test.mugshot.org";
		String personId = h.getId();
		String authKey = c.getAuthKey();
		LoginCookie loginCookie = new LoginCookie(host, personId, authKey);
		assertEquals(personId, loginCookie.getPersonId());
		assertEquals(authKey, loginCookie.getAuthKey());
		assertEquals(host, loginCookie.getHost());
		
		Cookie cookie = loginCookie.getCookie();
		assertNotNull(cookie);
		
		LoginCookie loginCookie2 = new LoginCookie(cookie);
		assertEquals(personId, loginCookie2.getPersonId());
		assertEquals(authKey, loginCookie2.getAuthKey());
		assertEquals(host, loginCookie.getHost());
	}
	
}
