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
	
	public static URL getBlogURLFromFriendId(String friendId) {
		try {
			return new URL("http://blog.myspace.com/blog/rss.cfm?friendId=" + friendId);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}	
	
	private static String scrapeFriendID(String html) {
		Pattern p = Pattern.compile("FriendsView.aspx?friendID=([0-9]+)");
		Matcher m = p.matcher(html);
		if (m.find())
			return m.group(1);
		p = Pattern.compile("fuseaction=user.viewPicture&amp;friendID=([0-9]+)");
		m = p.matcher(html);
		if (!m.find())
			return null;
		return m.group(1);
	}	
	
	public static String getFriendId(String mySpaceName) throws TransientServiceException {
		URL u;
		try {
			u = new URL("http://myspace.com/" + mySpaceName);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		URLConnection connection;
		logger.debug("opening connection to {}", u);		
		try {
			connection = URLUtils.openConnection(u);
		} catch (IOException e) {
			throw new TransientServiceException(e);
		}
		connection.setConnectTimeout(REQUEST_TIMEOUT);
		connection.setReadTimeout(REQUEST_TIMEOUT);
		connection.setAllowUserInteraction(false);
		
		String html;
		try {
			html = StreamUtils.readStreamUTF8(connection.getInputStream(), StreamUtils.ONE_MEGACHAR);
		} catch (IOException e) {
			throw new TransientServiceException(e);
		}
		String id = scrapeFriendID(html);
		if (id != null)
			return id;
		throw new TransientServiceException("Couldn't scrape MySpace friendId");
	}
	
	static public final void main(String[] args) throws TransientServiceException {
		String friendID = MySpaceScraper.getFriendId("cgwalters");
		System.out.println(friendID);
	}
}
