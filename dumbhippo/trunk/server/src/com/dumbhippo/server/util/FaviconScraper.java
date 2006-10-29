package com.dumbhippo.server.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
import com.dumbhippo.persistence.ExternalAccountType;

/**
 * Given an URL, download it as HTML and try to extract 
 * the favicon from a link element.
 * 
 * @author Havoc Pennington
 */
public final class FaviconScraper {
	
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FaviconScraper.class);
	
	private static final int TIMEOUT = 12000; 
	
	private URL faviconUrl;
	private String mimeType;
	
	static private final String mimeTypes[] = {
		"image/x-icon",
		"image/ico",
		"image/vnd.microsoft.icon",
		"image/gif",
		"image/png"
	};
	
	static private boolean knownIconType(String type) {
		for (String m : mimeTypes) {
			if (m.equals(type))
				return true;
		}
		return false;
	}
	
	static private class FaviconLinkExtractor extends EnumSaxHandler<FaviconLinkExtractor.Element> {

		// The enum names should match the xml element names (including case)
		enum Element {
			html,
			head,
			body,
			link,
			IGNORED // an element we don't care about
		}
		
		private String faviconUri;
		private String mimeType;
		
		public FaviconLinkExtractor() {
			super(Element.class, Element.IGNORED);
		}
		
		private boolean relIsIcon(String rel) {
			// See http://en.wikipedia.org/wiki/Favicon
			// The rel attribute is supposed to contain 
			// a space-separated list. For favicons,
			// old IE or something expects "shortcut icon" which 
			// is supposed to be two items "shortcut" and "icon" 
			// and most browsers treat it that way, but old IE 
			// treats it as one item. Correct browsers understand
			// just rel="icon" or rel="anything anything icon anything"

			// the two common cases
			if (rel.equals("icon") || rel.equals("shortcut icon"))
				return true;
			
			// attempt to be standards-compliant
			if (rel.contains("icon")) {
				String[] words = rel.split("\\s");
				for (String w : words) {
					if (w.equals("icon"))
						return true;
				}
			}
			
			return false;
		}		
		
		@Override
		protected void closeElement(Element element) throws SAXException {
			if (faviconUri == null &&
					element == Element.link && currentlyInside(Element.head)) {
				// See http://en.wikipedia.org/wiki/Favicon
				Attributes attrs = currentAttributes();
				String rel = attrs.getValue("rel");
				String type = attrs.getValue("type");
				String href = attrs.getValue("href");

				// logger.debug("rel = " + rel + " type = " + type + " href = " + href);
				
				if (rel != null && href != null &&
						(type == null || knownIconType(type)) &&
						relIsIcon(rel)) {
					faviconUri = href.trim();
					mimeType = type;
					if (mimeType == null) {
						if (href.endsWith((".ico")))
							mimeType = "image/ico";
						else if (href.endsWith((".png")))
							mimeType = "image/png";
						else if (href.endsWith((".gif")))
							mimeType = "image/gif";
						else 
							mimeType = "image/ico"; // who knows
					}
				}
			}
		}

		public String getUri() {
			return faviconUri;
		}
		
		public String getMimeType() {
			return mimeType;
		}
	}
	
	private boolean tryAsHtml(URL url, byte[] data) {
		XMLReader xmlReader = new Parser();
		FaviconLinkExtractor extractor = new FaviconLinkExtractor();
		xmlReader.setContentHandler(extractor);
		InputStream input = new ByteArrayInputStream(data);
		
		try {
			xmlReader.parse(new InputSource(input));
		} catch (IOException e) {
			return false;
		} catch (SAXException e) {
			return false;
		}
		
		// we could consider "validating" the favicon url given in the html...
		// but FaviconSystemBean probably needs to be able to handle that anyway,
		// so we'll just let it deal with it
		if (extractor.getUri() != null) {
			URL faviconUrl;
			try {
				faviconUrl = new URL(url, extractor.getUri());
			} catch (MalformedURLException e) {
				logger.warn("Malformed favicon URI: '" + extractor.getUri() + "' on site " + url);
				return false;
			}
			setFaviconUrl(faviconUrl);
			setMimeType(extractor.getMimeType());
			return true;
		} else {
			return false;
		}
	}
	
	public boolean tryRootDirectory(URL url) {
		URL faviconInRoot;
		try {
			faviconInRoot = new URL(url, "/favicon.ico");
		} catch (MalformedURLException e) {
			logger.warn("Could not construct root/favicon.ico from URL: " + url);
			return false;
		}
		URLConnection connection;
		try {
			connection = faviconInRoot.openConnection();

			connection.setConnectTimeout(TIMEOUT);
			connection.setReadTimeout(TIMEOUT);
			
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			httpConnection.setRequestMethod("HEAD");
			
			String contentType = connection.getContentType();
			
			// Flickr for example returned an ico as "text/plain" when 
			// this code was written; no doubt other sites are screwy too.
			// So basically if we don't throw an IO exception, we assume
			// we have an icon, and we force the MIME type to one 
			// we understand. 
			if (contentType == null || !knownIconType(contentType)) {
				contentType = "image/ico";
			}
			
			setFaviconUrl(faviconInRoot);
			setMimeType(contentType);
			return true;
		} catch (IOException e) {
			logger.debug("io exception fetching favicon at {}: {} ", faviconInRoot, e.getMessage());
			return false;
		}
	}
	
	public boolean analzyeURL(URL url) {	
		URLConnection connection;
		try {
			connection = url.openConnection();

			connection.setConnectTimeout(TIMEOUT);
			connection.setReadTimeout(TIMEOUT);
			byte[] data = StreamUtils.readStreamBytes(connection.getInputStream(), StreamUtils.ONE_MEGABYTE);
		
			if (tryAsHtml(url, data)) {
				return true;
			} 
		} catch (IOException e) {
			return false;
		}
		
		return tryRootDirectory(url);
	}

	public URL getFaviconUrl() {
		return faviconUrl;
	}

	public String getMimeType() {
		return mimeType;
	}
	
	private void setFaviconUrl(URL faviconUrl) {
		this.faviconUrl = faviconUrl;
	}
	
	private void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	/* For testing */
	public static void main(String[] args) throws Exception {
		for (ExternalAccountType e : ExternalAccountType.values()) {
			FaviconScraper scraper = new FaviconScraper();
			String u = e.getSiteLink();
			if (u.length() > 0) {
				System.out.println("Trying external account " + e + " url: " + u);
				scraper.analzyeURL(new URL(u));
				System.out.println("Got favicon: " + scraper.getFaviconUrl() + " mime type " + scraper.getMimeType());
			}
		}
		System.out.println("done");
	}
}
