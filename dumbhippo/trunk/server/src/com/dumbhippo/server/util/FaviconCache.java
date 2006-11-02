package com.dumbhippo.server.util;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.UniqueTaskExecutor;

public final class FaviconCache {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FaviconCache.class);

	private static boolean shutdown = false;
	private static FaviconCache instance;	
	
	private UniqueTaskExecutor<String,Icon> scrapeExecutor;     // map a page URL's external form to a favicon
	private Map<String,Icon> cache;                             // map a page URL's external form to its favicon data
	
	private FaviconCache() {
		this.scrapeExecutor = new UniqueTaskExecutor<String,Icon>("favicon scraper");
		this.cache = new HashMap<String,Icon>();
	}

	// protected by the class lock in the static shutdown() method
	private void shutdownInstance() {
		scrapeExecutor.shutdownAndAwaitTermination();
		scrapeExecutor = null;
	}
	
	public static FaviconCache getInstance() {
		synchronized (FaviconCache.class) {
			if (shutdown)
				throw new RuntimeException("attempt to use FaviconCache after shutdown");
			if (instance == null) {
				logger.debug("Starting up favicon cache");
				instance = new FaviconCache();
			}
		}
		return instance;
	}
	
	public static void shutdown() {
		synchronized (FaviconCache.class) {
			shutdown = true;

			if (instance != null) {
				logger.debug("Shutting down favicon cache");
				instance.shutdownInstance();
				instance = null;
			}
		}
	}
	
	// this class is intended to be immutable, with the technical problem 
	// that arrays can't be and we don't copy the byte[] when we return it
	// for efficiency reasons; just don't mutate the byte[] or we'll 
	// consider you in the wrong
	public static class Icon {
		private boolean loaded; // false if this is a negative result and all fields are null		
		private String remoteUrl;   // remote url of the favicon, null if loadFailed
		private byte[] iconData;    // favicon data loaded from remoteUrl, null if loadFailed
		private String contentType; // content type of favicon data, null if loadFailed
		
		private Icon(boolean loaded, String remoteUrl, byte[] iconData, String contentType) {
			this.loaded = loaded;
			this.remoteUrl = remoteUrl;
			this.iconData = iconData;
			this.contentType = contentType;
		}
		
		// not public for now since we shouldn't really be using it for anything
		String getRemoteUrl() {
			return remoteUrl;
		}
		
		public boolean getLoaded() {
			return loaded;
		}
		
		public byte[] getIconData() {
			return iconData;
		}
		
		public String getContentType() {
			return contentType;
		}
	}

	private synchronized Icon cacheResult(String pageUrl, Icon icon) {
		Icon old = cache.get(pageUrl);
		if (old != null && !icon.getLoaded()) {
			// don't replace a usable load with a failed one
			return old;
		} else {
			// replace old result with latest info
			cache.put(pageUrl, icon);
			return icon;
		}
	}
	
	private synchronized Icon checkCache(String pageUrl) {
		return cache.get(pageUrl);
	}
	
	// this is not supposed to be synchronized on the FaviconCache while doing
	// the fetching
	private Future<Icon> fetch(final String pageUrl) {
		Future<Icon> futureIcon = scrapeExecutor.execute(pageUrl,
			new Callable<Icon>() {
				public Icon call() throws Exception {
					logger.debug("Fetching favicon for {}", pageUrl);
					
					FaviconScraper scraper = new FaviconScraper();
					
					Icon icon;
					
					if (scraper.fetchURL(new URL(pageUrl))) {
						logger.debug(" fetched favicon from {} for {}", scraper.getFaviconUrl(), pageUrl);
						icon = new Icon(true, scraper.getFaviconUrl().toExternalForm(),
								scraper.getIconData(), scraper.getMimeType());
					} else {
						logger.debug("Failed to get favicon for {}", pageUrl);
						icon = new Icon(false, null, null, null);
					}
					
					return FaviconCache.this.cacheResult(pageUrl, icon);
				}
			});
	
		return futureIcon;
	}
	
	// Exporting a clear() method for use from /admin if needed
	public synchronized void clear() {
		logger.debug("Clearing favicon cache which had {} items in it", cache.size());
		cache.clear();
	}

	// start a favicon loading, only useful if you're going to do it for several of them in parallel
	public synchronized void preloadIconForPage(String pageUrl) {
		Icon icon = cache.get(pageUrl);
		if (icon != null)
			fetch(pageUrl); // fire off the async fetch
	}
	
	// for now the cache never expires, just figure we'll restart the server often enough,
	// or call clear() from /admin
	public Icon loadIconForPage(String pageUrl) {
		Icon icon = checkCache(pageUrl);
		if (icon != null)
			return icon;
		
		return ThreadUtils.getFutureResult(fetch(pageUrl));
	}

	// The name peek is a little misleading, since this might block;
	// the point is that it won't start a new task if there isn't a task 
	// already.
	public Icon peekIconForPage(String pageUrl) {
		Icon icon = checkCache(pageUrl);
		if (icon != null)
			return icon;
		
		Future<Icon> futureIcon = scrapeExecutor.peek(pageUrl);
		if (futureIcon != null)
			return ThreadUtils.getFutureResult(futureIcon);
		else
			return null;
	}
}
