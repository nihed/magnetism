package com.dumbhippo.server.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import com.sun.syndication.io.XmlReaderException;

/**
 * Given an URL, try to identify the associated RSS/Atom/etc. feed.
 * 
 * @author Havoc Pennington
 */
public final class FeedScraper {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FeedScraper.class);
	
	private static final int TIMEOUT = 12000; 
	
	private URL feedSource;
	private boolean fromHtml;
	
	private boolean tryAsFeed(URL url, byte[] data, String contentType) {
		InputStream input = new ByteArrayInputStream(data);
		XmlReader reader;
		try {
			reader = new XmlReader(input, contentType, /* lenient= */ true);
		} catch (XmlReaderException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		try {
			SyndFeed feed = new SyndFeedInput().build(reader);
			if (feed.getTitle() != null) { // final sanity check, that we got a title field
				setFeedSource(url);
				return true;
			} else {
				return false;
			}
		} catch (IllegalArgumentException e) {
			return false;
		} catch (FeedException e) {
			return false;
		}
	}

	static private class FeedLinkExtractor extends EnumSaxHandler<FeedLinkExtractor.Element> {

		// The enum names should match the xml element names (including case)
		enum Element {
			html,
			head,
			body,
			link,
			IGNORED // an element we don't care about
		}
		
		private URL atomUrl;
		private URL rssUrl;
		
		public FeedLinkExtractor() {
			super(Element.class, Element.IGNORED);
		}
		
		@Override
		protected void closeElement(Element element) throws SAXException {
			if (element == Element.link && currentlyInside(Element.head)) {
				// see e.g. http://blogs.msdn.com/rssteam/archive/2005/08/03/446904.aspx
				// Multiple <link> are allowed in the same format, with different titles;
				// the idea is that users can choose among them by title.
				// For now we will just pick the first one in the format we want.
				Attributes attrs = currentAttributes();
				String rel = attrs.getValue("rel");
				String type = attrs.getValue("type");
				String href = attrs.getValue("href");
				if (rel != null && type != null && href != null && 
						rel.trim().equals("alternate")) {
					href = href.trim();
					type = type.trim();
					try {
						if (type.equals("application/rss+xml") && rssUrl == null) {
							rssUrl = new URL(href);
							if (!rssUrl.getProtocol().equals("http"))
								rssUrl = null;
						} else if (type.equals("application/atom+xml") && atomUrl == null) {
							atomUrl = new URL(href);
							if (!atomUrl.getProtocol().equals("http"))
								atomUrl = null;					
						} else if (type.equals("text/xml") && rssUrl == null) {
							// uhh... assume it's rss I guess
							rssUrl = new URL(href);
							if (!rssUrl.getProtocol().equals("http"))
								rssUrl = null;
						}
					} catch (MalformedURLException e) {
						
					}
				}
			}
		}

		public URL getAtomUrl() {
			return atomUrl;
		}

		public URL getRssUrl() {
			return rssUrl;
		};	
	}
	
	private boolean tryAsHtml(URL url, byte[] data) {
		XMLReader xmlReader = new Parser();
		FeedLinkExtractor extractor = new FeedLinkExtractor();
		xmlReader.setContentHandler(extractor);
		InputStream input = new ByteArrayInputStream(data);
		
		try {
			xmlReader.parse(new InputSource(input));
		} catch (IOException e) {
			return false;
		} catch (SAXException e) {
			return false;
		}
		
		// we could consider "validating" the feed url given in the html...
		// but FeedSystemBean probably needs to be able to handle that anyway,
		// so we'll just let it deal with it
		if (extractor.getRssUrl() != null) {
			setFeedSource(extractor.getRssUrl());
			return true;
		} else if (extractor.getAtomUrl() != null) {
			setFeedSource(extractor.getAtomUrl());
			return true;
		} else {
			return false;
		}
	}
	
	public boolean analzyeURL(URL url) throws IOException {	
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(TIMEOUT);
		connection.setReadTimeout(TIMEOUT);
		byte[] data = StreamUtils.readStreamBytes(connection.getInputStream(), StreamUtils.ONE_MEGABYTE);
		
		if (tryAsFeed(url, data, connection.getContentType())) {
			return true;
		} else if (tryAsHtml(url, data)) {
			fromHtml = true;
			return true;
		} else {
			return false;
		}
	}

	public URL getFeedSource() {
		return feedSource;
	}

	private void setFeedSource(URL feedSource) {
		this.feedSource = feedSource;
	}
	
	public boolean getFromHtml() {
		return fromHtml;
	}
	
	/* For testing */
	public static void main(String[] args) throws Exception {
		String[] urls = { "http://log.ometer.com/",
				"http://log.ometer.com/rss.xml",
				"http://www.boingboing.net/index.rdf",
				"http://boingboing.net",
				"http://slashdot.org" };
		for (String u : urls) {
			FeedScraper scraper = new FeedScraper();
			System.out.println("Trying url: " + u);
			scraper.analzyeURL(new URL(u));
			System.out.println("Got feed source: " + scraper.getFeedSource() + " (fromHtml = " + scraper.getFromHtml() + ")");
		}
		System.out.println("done");
	}
}
