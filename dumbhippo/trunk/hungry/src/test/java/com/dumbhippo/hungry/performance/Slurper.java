package com.dumbhippo.hungry.performance;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.Config;
import com.dumbhippo.hungry.util.ConfigValue;

/**
 * Handles the interaction of a logged-in user with the server for
 * performance measurement purposes. The slurpUrl() method times
 * how long it takes the user to retrieve a single page - it 
 * makes a request for the page and discards the result.
 * 
 * @author otaylor
 */
public class Slurper {
	String userEmail;
	String userId;
	String sessionCookie;
	String authKey;
	
	public Slurper(String userEmail) {
		this.userEmail = userEmail;
		CheatSheet cs = CheatSheet.getReadOnly();
		
		userId = cs.getUserId(userEmail);
		if (userId == null) {
			throw new RuntimeException("Can't find user for email " + userEmail);
		}
		
		authKey = cs.getUserAuthKey(userId);
		if (authKey == null) {
			throw new RuntimeException("userId " + userId + " appears to have no auth keys, can't sign in");
		}
		
		GetSession gs = new GetSession(userId);
		gs.setUp();
		sessionCookie = gs.getSessionCookie();
		if (sessionCookie == null) {
			throw new RuntimeException("Failed to start session");
		}
	}
	
	void slurpUrl(String urlString) {
		// t.getTestContext().addCookie("auth", "name=" + userId + "&password=" + authKey);

		URL url;
		try {
			String baseUrlString = Config.getDefault().getValue(ConfigValue.BASEURL);
			URL baseUrl = new URL(baseUrlString);
			url = new URL(baseUrl, urlString);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error parsing URL", e);
		}
		
		try {
			URLConnection connection = (URLConnection)url.openConnection();
			String authCookieString = "auth=name=" + userId + "&password=" + authKey;
			String sesssionCookieString = "JSESSIONID=" + sessionCookie;
			connection.setRequestProperty("Cookie", authCookieString + "; " + sesssionCookieString);
			
			connection.connect();
			InputStream istream = connection.getInputStream();
			byte[] buffer = new byte[8192];
			while (true) {
				int count = istream.read(buffer);
				if (count == -1)
					break;
			}
		} catch (IOException e) {
			throw new RuntimeException("Error fetching " + urlString, e);
		}
	}
}
