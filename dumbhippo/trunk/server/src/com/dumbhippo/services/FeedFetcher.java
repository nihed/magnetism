package com.dumbhippo.services;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
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
import com.dumbhippo.server.NotFoundException;
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
	
	private static Date getFeedModifiedDate(SyndFeedInfo info) throws NotFoundException {
		// Clearly, they thought "Well, we don't have a class handy to parse HTTP dates.  Let's
		// just punt this to our users.  And to add to the pain, we'll make the method return
		// Object so they have to go digging in the source to find out what it will return!  Exccccellent."
		Object modifiedObj = info.getLastModified();
		Date date;
		if (modifiedObj == null) {
			throw new NotFoundException("Modification date not present in feed");
		} else if (modifiedObj instanceof Long) {
			long l = ((Long) modifiedObj).longValue();
			if (l <= 0)
				throw new NotFoundException("Modification date not known");
			date = new Date(l);
		} else if (!(modifiedObj instanceof String)) {
			throw new RomeAPIDesignerInsanityException("last modified object isn't a Long or String; giving up");
		} else {
			try {
				date = new Date(DateUtils.parseHttpDate((String) modifiedObj));
			} catch (ParseException e) {
				throw new NotFoundException("Failed to parse date '" + modifiedObj + "'", e);
			}
		}
		
		return date;
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
		if (info != null) {
			try {
				lastModified = getFeedModifiedDate(info);
			} catch (NotFoundException e1) {
				lastModified = null;
			}
		}
		
		try {
			SyndFeed feed = new HttpURLFeedFetcher(getCache()).retrieveFeed(url);
			// Now check whether the feed changed
			info = getCache().getFeedInfo(url);			
			Date currentModified;
			try {
				currentModified = getFeedModifiedDate(info);
			} catch (NotFoundException e1) {
				logger.debug("Failed to parse a current-modified date for feed {}: {}", url, e1.getMessage());
				currentModified = new Date();
			}
			
			try {
				if (lastModified != null) {
					long now = System.currentTimeMillis();
					
					/* Print some debug logging if this looks fishy since we do some bizarre modified date parsing */
					long lastBeforeNow = now - lastModified.getTime();
					
					if (lastBeforeNow > 1000 * 60 * 60 * 48 || lastBeforeNow < - 1000 * 60 * 60 * 24) {
						logger.debug("Possibly suspicious last modified date {} on {}", lastModified, url.toExternalForm());
						logger.debug("{} hours before now", lastBeforeNow / 1000.0 / 60.0 / 60.0);
					}
					
					long currentBeforeNow = now - currentModified.getTime();
					if (currentBeforeNow > 1000 * 60 * 60 * 36 || currentBeforeNow < - 1000 * 60 * 60 * 24) {
						logger.debug("Possibly suspicious current modified date {} on {}", currentModified, url.toExternalForm());
						logger.debug("Raw current '{}'", info.getLastModified());
						logger.debug("{} hours before now", currentBeforeNow / 1000.0 / 60.0 / 60.0);
					}
					
					long currentAfterLast = currentModified.getTime() - lastModified.getTime();
					
					if (currentAfterLast < 0 || currentAfterLast > 1000 * 60 * 60 * 48) {
						logger.debug("Possibly suspicious current modified date {} vs. last modified date {} on feed: " + url.toExternalForm(),
								currentModified, lastModified);
						logger.debug("Raw current '{}'", info.getLastModified());
						logger.debug("current is {} hours after last", currentAfterLast / 1000.0 / 60.0 / 60.0);
					}
				}
			} catch (Exception e) {
				// don't want the above code to break anything
				logger.warn("Feed date sanity-checking code is broken, but continuing anyway: {}", e.getMessage());
			}
			
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
		
		String urlStr = "http://planet.classpath.org/rss20.xml";
		URL url;
		if (args.length == 1) {
			urlStr = args[0];
		}
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			logger.error("Malformed input url: {}", e);
			System.exit(1);
			return;
		}

		FeedFetchResult result;
		try {
			result = FeedFetcher.getFeed(url);
		} catch (FetchFailedException e) {
			logger.error("Fetch failed {}", ExceptionUtils.getRootCause(e).getMessage());
			System.exit(1);
			return;
		}
		
		logger.debug("Result 1: modified = {}", result.isModified());
		
		try {
			result = FeedFetcher.getFeed(url);
		} catch (FetchFailedException e) {
			logger.error("Fetch failed {}", ExceptionUtils.getRootCause(e).getMessage());
			System.exit(1);
			return;
		}

		logger.debug("Result 2: modified = {}", result.isModified());
	}
}
