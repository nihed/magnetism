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
	
    // tasks to map a page URL's external form to a favicon,
	// saving to pageCache and/or dataCache as a side effect
	private UniqueTaskExecutor<String,Icon> scrapeExecutor;
	// map a page URL's external form to its favicon data. May contain 
	// icons with loaded=false that are not in dataCache, if we didn't
	// find a favicon url for a page.
	private Map<String,Icon> pageCache;
    // map a favicon download URL's external form to its favicon data.
	// generally there will be one or more pageCache entries pointing to the 
	// same Icon objects.
	private Map<String,Icon> dataCache;                             
	
	private FaviconCache() {
		this.scrapeExecutor = new UniqueTaskExecutor<String,Icon>("favicon scraper");
		this.pageCache = new HashMap<String,Icon>();
		this.dataCache = new HashMap<String,Icon>();
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
	// consider you in the wrong.
	// An Icon can be shared among multiple PageCache entries.
	public static class Icon {
		private boolean loaded;     // false if this is a negative result and all fields are null except possibly remoteUrl		
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
		// outside this file
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
		
		@Override
		public String toString() {
			if (loaded)
				return "{Icon type=" + contentType + " " + iconData.length + " bytes remoteUrl=" + remoteUrl + "}";
			else
				return "{Icon loaded=false remoteUrl=" + remoteUrl + "}";				
		}
	}

	private synchronized Icon cachePageIcon(String pageUrl, Icon icon) {
		Icon old = pageCache.get(pageUrl);
		if (old != null && !icon.getLoaded()) {
			// don't replace a usable load with a failed one
			return old;
		} else {
			// replace old result with latest info
			pageCache.put(pageUrl, icon);
			return icon;
		}
	}
	
	private synchronized Icon cacheIconData(Icon icon) {
		String remoteUrl = icon.getRemoteUrl();
		Icon old = dataCache.get(remoteUrl);
		if (old != null && !icon.getLoaded()) {
			// don't replace a usable load with a failed one
			return old;
		} else {
			// replace old result with latest info
			dataCache.put(remoteUrl, icon);
			return icon;
		}
	}
	
	private synchronized Icon checkPageCache(String pageUrl) {
		return pageCache.get(pageUrl);
	}
	
	private synchronized Icon checkDataCache(String remoteUrl) {
		return dataCache.get(remoteUrl);
	}
	
	// this is not supposed to be synchronized on the FaviconCache while doing
	// the fetching. The task assumes that checkPageCache() was already called
	// so doesn't bother doing that
	private Future<Icon> fetch(final String pageUrl) {
		Future<Icon> futureIcon = scrapeExecutor.execute(pageUrl,
			new Callable<Icon>() {
				public Icon call() throws Exception {
					logger.debug("Fetching favicon for {}", pageUrl);
					
					FaviconScraper scraper = new FaviconScraper();
					
					Icon icon;
					
					if (scraper.analyzeURL(new URL(pageUrl))) {
						String remoteUrl = scraper.getFaviconUrl().toExternalForm();
						logger.debug(" favicon for {} is at url {}", pageUrl, remoteUrl);
						
						icon = checkDataCache(remoteUrl);
						
						if (icon != null) {
							logger.debug(" favicon {} already in cache", remoteUrl);
							; // nothing to do
						} else if (scraper.downloadIcon(scraper.getFaviconUrl(), true)) {
							logger.debug(" favicon downloaded successfully from {}", remoteUrl);
							icon = cacheIconData(new Icon(true, remoteUrl,
								scraper.getIconData(), scraper.getMimeType()));
						} else {
							logger.debug(" failed to download favicon from {}", remoteUrl);
							icon = cacheIconData(new Icon(false, remoteUrl, null, null));
						}
					} else {
						logger.debug(" failed to get favicon url for page {}", pageUrl);
						icon = new Icon(false, null, null, null);
					}
					
					return cachePageIcon(pageUrl, icon);
				}
			});
	
		return futureIcon;
	}
	
	// Exporting a clear() method for use from /admin if needed
	public synchronized void clear() {
		logger.debug("Clearing favicon cache which had {} pages and {} icons in it",
				pageCache.size(), 
				dataCache.size());
		dataCache.clear();
		pageCache.clear();
	}

	// start a favicon loading, only useful if you're going to do it for several of them in parallel
	public synchronized void preloadIconForPage(String pageUrl) {
		Icon icon = checkPageCache(pageUrl);
		if (icon == null)
			fetch(pageUrl); // fire off the async fetch
	}
	
	// for now the cache never expires, just figure we'll restart the server often enough,
	// or call clear() from /admin
	public Icon loadIconForPage(String pageUrl) {
		Icon icon = checkPageCache(pageUrl);
		if (icon != null) {
			logger.debug(" loading cached favicon for {}: {}", pageUrl, icon);
			return icon;
		}
		
		return ThreadUtils.getFutureResult(fetch(pageUrl));
	}

	// The name peek is a little misleading, since this might block;
	// the point is that it won't start a new task if there isn't a task 
	// already.
	public Icon peekIconForPage(String pageUrl) {
		Icon icon = checkPageCache(pageUrl);
		if (icon != null) {
			logger.debug(" peeking cached favicon for {}: {}", pageUrl, icon);
			return icon;
		}
		
		Future<Icon> futureIcon = scrapeExecutor.peek(pageUrl);
		if (futureIcon != null)
			return ThreadUtils.getFutureResult(futureIcon);
		else
			return null;
	}
}
