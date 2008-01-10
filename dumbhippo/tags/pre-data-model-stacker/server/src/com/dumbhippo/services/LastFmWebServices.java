package com.dumbhippo.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dumbhippo.StringUtils;
import com.dumbhippo.URLUtils;

public class LastFmWebServices {
	
	public static final int MAX_USERNAME_LENGTH = 16;	
	
	protected static class LastFmWebServiceTrack implements LastFmTrack {
		private String artist;
		private String title;
		private long listenTime;
		private String url;
		
		public LastFmWebServiceTrack(String artist, String title, long listenTime, String url) {
			this.artist = artist;
			this.title = title;
			this.listenTime = listenTime;
			this.url = url;
		}
		
		public String getArtist() {
			return artist;
		}
		
		public long getListenTime() {
			return listenTime;
		}
		
		public String getTitle() {
			return title;
		}
		
		public String getUrl() {
			return url;
		}
	}

	
	private static URL getFeedUrl(String username) {
		try {
			return new URL("http://ws.audioscrobbler.com/1.0/user/" + StringUtils.urlEncode(username) + "/recenttracks.xml");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<LastFmTrack> getTracksForUser(String username) throws TransientServiceException {
		Document doc = null;
		File temp = null;
		List<LastFmTrack> tracks = new ArrayList<LastFmTrack>();			
		try {
			try {
				URLConnection connection = URLUtils
						.openConnection(getFeedUrl(username));
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				doc = factory.newDocumentBuilder().parse(connection.getInputStream());
			} catch (IOException e) {
				throw new TransientServiceException(e);
			} catch (SAXException e) {
				throw new TransientServiceException(e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			}

			XPath xpath = XPathFactory.newInstance().newXPath();
			try {
				NodeList trackNodes = (NodeList) xpath.evaluate("/recenttracks/track", doc, XPathConstants.NODESET);
				for (int i = 0; i < trackNodes.getLength(); i++) {
					Node node = trackNodes.item(i);
					long date;
					try {
						date = Long.parseLong(xpath.evaluate("date/attribute::uts", node));
					} catch (NumberFormatException e) {
						throw new TransientServiceException(e);
					}
					tracks.add(new LastFmWebServiceTrack(
							xpath.evaluate("artist", node), 
							xpath.evaluate("name", node),
							date, 
							xpath.evaluate("url", node)));
				}
			} catch (XPathExpressionException e) {
				throw new TransientServiceException(e);
			}
		} finally {
			if (temp != null)
				temp.delete();
		}
		return tracks;
	}
	
	// The way that last.fm builds URLs for tracks is distinctly funky - the components of
	// the URL are URL-decoded *twice*. It would be safe to simply encode twice always, 
	// but that would give weird URLs ... "The%2BBeatles" instead of "The+Beatles". 
	// So, last.fm only double-encodes components with certain 'dangerous' characters
	// in them, and we try to match that. We also do our URL encoding by hand rather than
	// using URLEncoder.encode(), to match other slight differences in the way that
	// last.fm does it (in particular, they encode '*' while Java doesn't), though it's
	// not clear that there is a lot of point in being that exact a match.
	
	// If a path component contains any of these, it needs to be URL-encoded *twice* 
	static final Pattern NEEDS_DOUBLE = Pattern.compile("[%&/+#;]");
	
	// Any of the following characters need to be encoded as hex
	static final Pattern NEEDS_ENCODING = Pattern.compile("[^A-Za-z0-9_.-]+");
	
	static private String encodeComponent(String str) {
		if (NEEDS_DOUBLE.matcher(str).find()) {
			return encodeOnce(encodeOnce(str));
		} else if (NEEDS_ENCODING.matcher(str).find()) {
			return encodeOnce(str);
		} else {
			return str;
		}
	}

	private static String encodeOnce(String str) {
					StringBuffer sb = new StringBuffer();
		Matcher m = NEEDS_ENCODING.matcher(str);
		
		while (m.find()) {
			m.appendReplacement(sb, "");
			encodeCharacter(sb, m.group());
		}
		m.appendTail(sb);
		
		return sb.toString();
	}
	
	private static void encodeCharacter(StringBuffer sb, String s) {
		if (" ".equals(s))
			sb.append("+");
		else {
			ByteBuffer bytes = Charset.forName("UTF-8").encode(s);
			while (bytes.hasRemaining()) {
				int b = bytes.get() & 0xFF;
				sb.append('%');
				sb.append(Integer.toHexString(b));
			}
		}
	}

	/**
	 * Returns the URL of the last.fm web page for a artist/song title pair. No check is done
	 * that the page actually exists.  
	 * 
	 * @param artist the artist
	 * @param title the song title
	 * @return the appropriate form for the link on last.fm
	 */
	public static String makeWebLink(String artist, String title) {
		return "http://last.fm/music/" + encodeComponent(artist) + "/_/" + encodeComponent(title);

	}
	
	static public final void main(String[] args) {
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);
		
		List<LastFmTrack> tracks;
		try {
			tracks = getTracksForUser("RJ");
		} catch (TransientServiceException e) {
			e.printStackTrace();
			return;
		}
		for (LastFmTrack track : tracks) {
			System.out.println("track: " + track.getTitle() + " - " + track.getArtist() + " " + track.getListenTime() + " " + track.getUrl());
		}
	}
}
