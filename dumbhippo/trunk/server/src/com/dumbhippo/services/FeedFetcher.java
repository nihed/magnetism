package com.dumbhippo.services;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;

import com.dumbhippo.DateUtils;
import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;

/**
 * Simple wrapper class for HttpURLFeedFetcher which uses a custom memory cache,
 * and can return a status which tells us whether or not the feed was modified.
 */
public class FeedFetcher {
	
	public interface FeedFetchResult {
		public SyndFeed getFeed();
		public boolean isModified();
	}
	
	private static class FeedFetchResultImpl implements FeedFetchResult {
		private SyndFeed feed;
		private boolean modified;
		
		public FeedFetchResultImpl(SyndFeed feed, boolean modified) {
			this.feed = feed;
			this.modified = modified;
		}

		public SyndFeed getFeed() {
			return feed;
		}

		public boolean isModified() {
			return modified;
		}
	}	
	
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
	
	private static class RomeAPIDesignerInsanityException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public RomeAPIDesignerInsanityException(String message) {
			super(message);
		}
	}
	
	private static Date getFeedModifiedDate(SyndFeedInfo info) {
		// Clearly, they thought "Well, we don't have a class handy to parse HTTP dates.  Let's
		// just punt this to our users.  And to add to the pain, we'll make the method return
		// Object so they have to go digging in the source to find out what it will return!  Exccccellent."
		Object modifiedObj = info.getLastModified();
		if (modifiedObj instanceof Long)
			return new Date(((Long) modifiedObj).longValue());
		if (!(modifiedObj instanceof String))
			throw new RomeAPIDesignerInsanityException("last modified object isn't a Long or String; giving up");
		return new Date(DateUtils.parseHttpDate((String) modifiedObj));		
	}
	
	public static FeedFetchResult getFeed(URL url) throws FetchFailedException {
		// FIXME unfortunately the timeout on the feed fetcher http download is 
		// way too long - but there's no way to fix without hacking on ROME.
		// Doing the HTTP GET by hand is not really desirable since the feed fetcher
		// is smarter than that (e.g. uses some "get new stuff only" protocols, checks
		// whether the data has changed, etc.)
		
		// See if we have the feed cached
		Date lastModified = null;
		SyndFeedInfo info = getCache().getFeedInfo(url);
		if (info != null)
			lastModified = getFeedModifiedDate(info);
		
		try {
			SyndFeed feed = new HttpURLFeedFetcher(getCache()).retrieveFeed(url);
			// Now check whether the feed changed
			info = getCache().getFeedInfo(url);			
			Date currentModified = getFeedModifiedDate(info);
			return new FeedFetchResultImpl(feed, lastModified == null ? true : currentModified.after(lastModified));
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
	
	public static void main(String[] args) {
		org.apache.log4j.Logger log4jRoot = org.apache.log4j.Logger.getRootLogger();
		ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%d %-5p [%c] (%t): %m%n"));
		log4jRoot.addAppender(appender);
		log4jRoot.setLevel(Level.DEBUG);

		FeedFetchResult result;
		try {
			result = FeedFetcher.getFeed(new URL("http://planet.classpath.org/rss20.xml"));
		} catch (MalformedURLException e) {
			logger.error("Malformed url {}", e.getMessage());
			System.exit(1);
			return;
		} catch (FetchFailedException e) {
			logger.error("Fetch failed {}", ExceptionUtils.getRootCause(e).getMessage());
			System.exit(1);
			return;
		}
		
		logger.debug("Result 1: modified = {}, {}", result.isModified(), result.getFeed());
		
		try {
			result = FeedFetcher.getFeed(new URL("http://planet.classpath.org/rss20.xml"));
		} catch (MalformedURLException e) {
			logger.error("Malformed url {}", e.getMessage());
			System.exit(1);
			return;
		} catch (FetchFailedException e) {
			logger.error("Fetch failed {}", ExceptionUtils.getRootCause(e).getMessage());
			System.exit(1);
			return;
		}

		logger.debug("Result 2: modified = {}, {}", result.isModified(), result.getFeed());
	}
}
