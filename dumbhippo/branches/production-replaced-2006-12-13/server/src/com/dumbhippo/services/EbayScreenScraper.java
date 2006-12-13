package com.dumbhippo.services;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StreamUtils;

public class EbayScreenScraper {
	static private final Logger logger = GlobalSetup.getLogger(EbayScreenScraper.class);
	
	private static class ScrapedItemData implements EbayItemData {

		private String pictureUrl;
		
		ScrapedItemData(String pictureUrl) {
			this.pictureUrl = pictureUrl;
		}
		
		public String getPictureUrl() {
			return pictureUrl;
		}

		public String getTimeLeft() {
			return null;
		}

		public String getBuyItNowPrice() {
			return null;
		}

		public String getStartPrice() {
			return null;
		}
	}

	private static class ScrapedImageCandidate {
		String url;
		int order;
		int score;
		
		ScrapedImageCandidate(String url, int order) {
			this.url = url;
			this.order = order;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ScrapedImageCandidate))
				return false;
			return url.equals(((ScrapedImageCandidate)obj).url);
		}

		@Override
		public int hashCode() {
			return url.hashCode();
		}

		@Override
		public String toString() {
			return "{url=" + url + " score=" + score + " order=" + order + "}";
		}
	}

	private int timeoutMilliseconds;
	
	public EbayScreenScraper(int timeoutMilliseconds) {
		this.timeoutMilliseconds = timeoutMilliseconds;
	}
	
	private String scrapePictureUrl(String html) {
		
		// Right now the main photo for an item is normally 
		// inside an <a href="#stockphoto"> or
		// <a href="#ebayphotohosting">. The other images
		// on a page are normally page layout stuff, 
		// usually .gif or files with no extension, and 
		// the giant free-form seller area where they can put
		// all kinds of random html.
		
		// this is done with a heuristic/scoring system so that
		// it will make some kind of effort even if the page
		// html changes someday
		
		List<String> allImages = new ArrayList<String>();
		Set<String> afterPhotoHostingImages = new HashSet<String>();
		Set<String> afterStockPhotoImages = new HashSet<String>();
		boolean afterPhotoHosting = false;
		boolean afterStockPhoto = false;
		Pattern p = Pattern.compile("(<img[^>]+src=\"([^\"]+)\"[^>]*)|(href=\"#ebayphotohosting)|(href=\"#stockphoto)");
		Matcher m = p.matcher(html);
		while (m.find()) {
			//logger.debug(m.group(0));
			boolean photoHosting = m.group(3) != null;
			boolean stockPhoto = m.group(4) != null;
			String imgSrc = m.group(2);
			//logger.debug("photoHosting = " + photoHosting + " stockPhoto = " + stockPhoto + " imgSrc = " + imgSrc);
			if (imgSrc != null) {
				allImages.add(imgSrc);
				if (afterPhotoHosting) {
					afterPhotoHostingImages.add(imgSrc);
					afterPhotoHosting = false;
				}
				if (afterStockPhoto) {
					afterStockPhotoImages.add(imgSrc);
					afterStockPhoto = false;
				}
			}
			if (photoHosting)
				afterPhotoHosting = true;
			if (stockPhoto)
				afterStockPhoto = true;
		}
		
		// FIXME we need to XML-unescape the URLs
		
		//logger.debug("allImages = " + allImages);
		//logger.debug("afterPhotoHostingImages = " + afterPhotoHostingImages);
		//logger.debug("afterStockPhotoImages = " + afterStockPhotoImages);
	
		Set<ScrapedImageCandidate> possible = new HashSet<ScrapedImageCandidate>();
		
		int order = 0;
		for (String s : allImages) {
			
			// First filter most of the images by skipping those we know 
			// to be ebay layout etc.
			// ebay gifs live here
			if (s.startsWith("http://pics.ebaystatic.com"))
				continue;
			// some known ebay gif names, just in case the above fails
			if (s.endsWith("/s.gif"))
				continue;
			if (s.endsWith("/s"))
				continue;
			if (s.endsWith("/x.gif"))
				continue;
			if (s.endsWith("/x"))
				continue;
			if (s.contains("aw/pics/navbar/"))
				continue;
			
			// the rest we'll heuristically consider
			
			ScrapedImageCandidate scraped = new ScrapedImageCandidate(s, order);
			
			// almost certainly a product picture if not a png/gif
			if (s.endsWith(".jpeg") || s.endsWith(".jpg") ||
					s.endsWith(".tif") || s.endsWith(".tiff") ||
					s.endsWith(".JPEG") || s.endsWith(".JPG") ||
					s.endsWith(".TIF") || s.endsWith(".TIFF"))
				scraped.score += 2;
			
			// but a png is more likely to be a picture than a gif
			if (s.endsWith(".png"))
				scraped.score += 1;
			
			// this narrows it down to only a few usually
			if (afterPhotoHostingImages.contains(s) ||
					afterStockPhotoImages.contains(s))
				scraped.score += 2;
			
			// ebay gifs often are called things like "iconRight_10x10"
			// so boost anything not called that
			if (!s.matches("[0-9]+x[0-9]+\\."))
				scraped.score += 1;
		
			// people use honesty.com/andale.com for hit counters,
			// stored at counters.honesty.com that I've seen
			if (!s.contains("counters."))
				scraped.score += 1;
			
			// ebay image hosting
			if (s.contains("ebayimg.com/") ||
					s.contains("photobucket.com/"))
				scraped.score += 1;
			
			possible.add(scraped);
			
			++order;
		}
		
		//logger.debug("possible={}", possible);
		ScrapedImageCandidate best = null;
		for (ScrapedImageCandidate scraped : possible) {
			if (best == null)
				best = scraped;
			else {
				if (scraped.score > best.score)
					best = scraped;
				else if (scraped.score == best.score && 
						scraped.order < best.order) {
					// the "real" image is normally earlier in the page
					best = scraped;
				}
			}
		}
		
		return best.url;
	}
	
	public EbayItemData getItem(String itemId) {
		String html;
		try {
			URL url = new URL("http://cgi.ebay.com/ws/eBayISAPI.dll?ViewItem&item=" + itemId);
			logger.debug("loading ebay url {}", url);

			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(timeoutMilliseconds);
			connection.setReadTimeout(timeoutMilliseconds);
			html = StreamUtils.readStreamUTF8(connection.getInputStream(), StreamUtils.ONE_MEGACHAR);
		
		} catch (IOException e) {
			logger.warn("http error getting ebay item: {}", e.getMessage());
			return null;
		}
		
		String pictureUrl = scrapePictureUrl(html);
		
		if (pictureUrl != null)
			return new ScrapedItemData(pictureUrl);
		else
			return null;
	}
	
	static public final void main(String[] args) {
		EbayScreenScraper scraper = new EbayScreenScraper(10000);
		
		//String itemId = "5059205542";
		//String itemId = "4799641609";	
		String itemId = "5606623458";
		
		EbayItemData itemData = scraper.getItem(itemId);
		if (itemData == null)
			System.out.println("Failed to get item");
		else
			System.out.println("URL is " + itemData.getPictureUrl());
	}
}
