package com.dumbhippo.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.URLUtils;

public class MySpaceScraper {

	static private final Logger logger = GlobalSetup.getLogger(MySpaceScraper.class);

	static private final int REQUEST_TIMEOUT = 1000 * 12;

	private static String scrapeFriendID(String html) {
		Pattern p = Pattern.compile("/profileHitCounter.cfm\\?friendID=([0-9]+)");
		Matcher m = p.matcher(html);
		if (m.find())
			return m.group(1);
		p = Pattern.compile("\\?fuseaction=user.viewfriends&friendID=([0-9]+)");
		m = p.matcher(html);
		if (!m.find())
			return null;
		return m.group(1);
	}	
	
	public static String getFriendId(String mySpaceName) throws IOException {
		URL u;
		try {
			u = new URL("http://myspace.com/" + mySpaceName);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		URLConnection connection;
		logger.debug("opening connection to {}", u);		
		connection = URLUtils.openConnection(u);
		connection.setConnectTimeout(REQUEST_TIMEOUT);
		connection.setReadTimeout(REQUEST_TIMEOUT);
		connection.setAllowUserInteraction(false);
		
		String html = StreamUtils.readStreamUTF8(connection.getInputStream(), StreamUtils.ONE_MEGACHAR);		
		return scrapeFriendID(html);
	}
	
	static public final void main(String[] args) throws IOException {
		String friendID = MySpaceScraper.getFriendId("cgwalters");
		System.out.println(friendID);
	}
}
