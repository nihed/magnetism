package com.dumbhippo.services;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;

/**
 * Simple wrapper class for HttpURLFeedFetcher which uses a custom memory cache.
 */
public class FeedFetcher {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FeedFetcher.class);

	/** 
	 * For now the only point of this vs. the one that comes with Rome is that
	 * we can do logging.  Later we should probably improve this to store
	 * feed info on disk instead of keeping everything in memory.
	 *
	 */
	private static class FeedCache implements FeedFetcherCache, Serializable {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings({"unused","hiding"})
		private static final Logger logger = GlobalSetup.getLogger(FeedCache.class);		
		
		@SuppressWarnings("hiding")
		private Map<String,SyndFeedInfo> cache;
		
		FeedCache() {
			cache = new HashMap<String,SyndFeedInfo>();
		}
		
		static private class InfoStringifier {
			private SyndFeedInfo info;
			InfoStringifier(SyndFeedInfo info) {
				this.info = info;
			}
			
			@Override
			public String toString() {
				return "{SyndFeedInfo id='" + info.getId() + "' + url='" + info.getUrl() + "' " +
				"feed has " + info.getSyndFeed().getEntries().size() + " entries}";
			}
		}
		
		public synchronized SyndFeedInfo getFeedInfo(URL url) {
			SyndFeedInfo info = cache.get(url.toExternalForm());
			if (info != null) {
				logger.debug(" getting cached feed for {}: {}", url, new InfoStringifier(info));
			}
			return info;
		}

		public synchronized void setFeedInfo(URL url, SyndFeedInfo info) {
			if (info != null) {
				logger.debug(" storing cached feed for {}: {}", url, new InfoStringifier(info));
			}
			cache.put(url.toExternalForm(), info);
		}
	}	
	
	private static FeedFetcherCache cache = null;	
	
	private static synchronized FeedFetcherCache getCache() {
		if (cache == null)
			cache = new FeedCache();
		return cache;
	}
	
	public static class FetchFailedException extends Exception {
		public FetchFailedException(Exception e) {
			super(e);
		}

		private static final long serialVersionUID = 1L;
	}
	
	public static SyndFeed getFeed(URL url) throws FetchFailedException {
		// FIXME unfortunately the timeout on the feed fetcher http download is 
		// way too long - but there's no way to fix without hacking on ROME.
		// Doing the HTTP GET by hand is not really desirable since the feed fetcher
		// is smarter than that (e.g. uses some "get new stuff only" protocols, checks
		// whether the data has changed, etc.)
		
		try {
			return new HttpURLFeedFetcher(getCache()).retrieveFeed(url);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new FetchFailedException(e); 
		} catch (FeedException e) {
			throw new FetchFailedException(e);			
		} catch (FetcherException e) {
			throw new FetchFailedException(e);			
		}
	}
}
