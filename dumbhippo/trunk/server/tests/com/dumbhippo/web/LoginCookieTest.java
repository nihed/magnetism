package com.dumbhippo.web;

import javax.servlet.http.Cookie;

import com.dumbhippo.persistence.Client;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;

import junit.framework.TestCase;

public class LoginCookieTest extends TestCase {

	public void testCookieRoundtrip() throws Exception {
		Client c = new Client();
		Person p = new Person();
		HippoAccount acct = new HippoAccount(p, c);
		String personId = p.getId();
		String authKey = c.getAuthKey();
		LoginCookie loginCookie = new LoginCookie(acct, c);
		assertEquals(personId, loginCookie.getPersonId());
		assertEquals(authKey, loginCookie.getAuthKey());
		
		Cookie cookie = loginCookie.getCookie();
		assertNotNull(cookie);
		
		LoginCookie loginCookie2 = new LoginCookie(cookie);
		assertEquals(personId, loginCookie2.getPersonId());
		assertEquals(authKey, loginCookie2.getAuthKey());
	}
	
}
