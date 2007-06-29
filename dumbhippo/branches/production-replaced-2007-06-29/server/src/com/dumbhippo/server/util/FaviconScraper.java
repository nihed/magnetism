package com.dumbhippo.server.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.dumbhippo.EnumSaxHandler;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;
import com.dumbhippo.URLUtils;
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
	private byte[] iconData;
	
	// different names for .ico format
	static private final String icoMimeTypes[] = {
		"image/x-icon",
		"image/ico",
		"image/vnd.microsoft.icon"		
	};
	
	// should be a superset of the above. This is 
	// our whitelist of all known image formats we'll accept.
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
	
	static private boolean knownIcoMimeName(String type) {
		for (String m : icoMimeTypes) {
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
	
	/**
	 * When using the IE DirectX filter as our dh:png does, .ico files
	 * won't show up. So we try to convert to PNG if possible. We pretty
	 * much rely on this working.
	 * @return true on success
	 */
	private boolean tryConvertToPng() {
		try {
			InputStream in = new ByteArrayInputStream(getIconData());
			//logger.debug("formats: {}", Arrays.toString(ImageIO.getReaderFormatNames()));
			
			// the ImageIO.read() convenience function barfs on the .ico format, 
			// my two theories are that a) it gets confused by multiple images
			// in the stream or b) it tries to use the WBMP loader instead of 
			// our ICOReader jar. b) is my favorite.
			// The below code would fix both of those theories, since we 
			// get the ico loader by hand and then iterate over each image
			// in the stream.
			
			ImageReader reader = ImageIO.getImageReadersByFormatName("ico").next();
			reader.setInput(new MemoryCacheImageInputStream(in));
			for (int i = 0; true; ++i) {
				BufferedImage image;
				try {
					image = reader.read(i);
				} catch (IndexOutOfBoundsException e) {
					logger.debug("  no more frames in .ico");
					break;
				}
				
				// The ICO ImageReader apparently returns null images for unsupported bit depths and
				// prints something directly to stderr.  Thankfully it didn't do something
				// rash and unheard of such as throw an exception.
				if (image != null && (image.getWidth() == 16 && image.getHeight() == 16)) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ImageIO.write(image, "png", out);
					setIconData(out.toByteArray());
					setMimeType("image/png");
					return true;
				} else {
					if (image == null) {
						logger.debug("ICO reader barfed on image");
					} else {
						logger.debug("  ignoring {}x{} image in .ico data",
								image.getWidth(), image.getHeight());
					}
				}
			}
			logger.debug(" ico data contained no 16x16 image");
			return false;
		} catch (IOException e) {
			logger.warn(" failed to convert icon to PNG data: {}", e.getMessage());
			return false;
		}
	}
	
	public boolean downloadIcon(URL url, boolean withData) {
		try {
			URLConnection connection = URLUtils.openConnection(url);

			connection.setConnectTimeout(TIMEOUT);
			connection.setReadTimeout(TIMEOUT);
			
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			
			byte[] data = null;
			if (withData)
				data = StreamUtils.readStreamBytes(connection.getInputStream(), StreamUtils.ONE_KILOBYTE * 64);
			else
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
			
			setFaviconUrl(url);
			setMimeType(contentType);
			if (withData) {
				setIconData(data);
				if (knownIcoMimeName(contentType)) {
					if (tryConvertToPng()) {
						logger.debug("  successfully converted favicon to PNG format");
					}
				}
			}
			return true;
		} catch (IOException e) {
			logger.debug("io exception fetching favicon at {}: {} ", url, e.getMessage());
			return false;
		}
	}
	
	private boolean tryRootDirectory(URL url, boolean withData) {
		URL faviconInRoot;
		try {
			faviconInRoot = new URL(url.getProtocol(), url.getHost(), url.getPort(), "/favicon.ico");
		} catch (MalformedURLException e) {
			logger.warn("Could not construct root/favicon.ico from URL: " + url);
			return false;
		}
		return downloadIcon(faviconInRoot, withData);
	}
	
	private boolean scrapeURL(URL url) {
		try {
			URLConnection connection = URLUtils.openConnection(url);

			connection.setConnectTimeout(TIMEOUT);
			connection.setReadTimeout(TIMEOUT);
			byte[] data = StreamUtils.readStreamBytes(connection.getInputStream(), StreamUtils.ONE_MEGABYTE);
		
			if (tryAsHtml(url, data)) {
				return true;
			} 
		} catch (IOException e) {
			logger.debug("Scraping favicon {} failed: {}", url, e.getMessage());
			return false;
		}
		return false;
	}
	
	public boolean analyzeURL(URL url) {	
		if (scrapeURL(url))
			return true;
		else
			return tryRootDirectory(url, false);
	}

	// this is private because using it is probably always broken; 
	// it means you can't cache the image data per-site, only per-page
	private boolean fetchURL(URL url) {
		if (scrapeURL(url))
			return downloadIcon(getFaviconUrl(), true);
		else
			return tryRootDirectory(url, true);
	}	
	
	public URL getFaviconUrl() {
		return faviconUrl;
	}

	public String getMimeType() {
		return mimeType;
	}
	
	public byte[] getIconData() {
		return iconData;
	}
	
	private void setFaviconUrl(URL faviconUrl) {
		this.faviconUrl = faviconUrl;
	}
	
	private void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	private void setIconData(byte[] iconData) {
		this.iconData = iconData;
	}
	
	/* For testing */
	public static void main(String[] args) throws Exception {
		for (ExternalAccountType e : ExternalAccountType.values()) {
			FaviconScraper scraper = new FaviconScraper();
			String u = e.getSiteLink();
			if (u.length() > 0) {
				System.out.println("Trying external account " + e + " url: " + u);
				scraper.analyzeURL(new URL(u));
				System.out.println("Got favicon: " + scraper.getFaviconUrl() + " mime type " + scraper.getMimeType());
			}
		}
		for (ExternalAccountType e : ExternalAccountType.values()) {
			FaviconScraper scraper = new FaviconScraper();
			String u = e.getSiteLink();
			if (u.length() > 0) {
				System.out.println("Trying external account " + e + " url: " + u);
				scraper.fetchURL(new URL(u));
				System.out.println("Got favicon: " + scraper.getFaviconUrl() + " mime type " + scraper.getMimeType() + " kbytes of data " + scraper.getIconData().length/1024.0);
			}
		}
		System.out.println("done");
	}
}
