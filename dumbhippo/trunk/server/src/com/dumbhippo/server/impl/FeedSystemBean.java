package com.dumbhippo.server.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.util.EJBUtil;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.sun.syndication.io.FeedException;

@Stateless
public class FeedSystemBean implements FeedSystem {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FeedSystemBean.class);
	
	// How old the feed data can be before we refetch
	static final long FEED_UPDATE_TIME = 10 * 60 * 1000; // 10 minutes
	
	// Interval at which we check all threads for needing update. This is shorter 
	// than FEED_UPDATE_TIME so that we don't just miss an update and wait 
	// an entire additional cycle
	static final long UPDATE_THREAD_TIME = FEED_UPDATE_TIME / 2;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	private static ExecutorService notificationService;
	private static boolean shutdown = false;
	
	private static synchronized ExecutorService getNotificationService() {
		if (shutdown)
			throw new RuntimeException("getNotificationService() called after shutdown");
		
		if (notificationService == null)
			notificationService = ThreadUtils.newSingleThreadExecutor("FeedSystem notification");
		
		return notificationService;
	}

	private Feed lookupExistingFeed(LinkResource source) {
		try {
			Feed feed = (Feed)em.createQuery("SELECT f FROM Feed f WHERE f.source = :source")
			  .setParameter("source", source)
			  .getSingleResult();
			
			return feed;
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	private SyndFeed fetchFeedFromNet(LinkResource source) throws XmlMethodException {
		URL url;
		try {
			url = new URL(source.getUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException("getFeed passed malformed URL object");
		}
		
		FeedFetcher feedFetcher = new HttpURLFeedFetcher(null);
		try {
			return feedFetcher.retrieveFeed(url);
		} catch (IOException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Network error fetching feed " + url);
		} catch (FetcherException e) {
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Error requesting feed from server " + url);
		} catch (FeedException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Error parsing feed " + url);
		}
	}
	
	private FeedEntry createEntryFromSyndEntry(Feed feed, SyndEntry syndEntry) throws MalformedURLException {
		URL entryUrl = new URL(syndEntry.getLink());
		
		FeedEntry entry = new FeedEntry(feed);
		entry.setEntryGuid(syndEntry.getUri());
		entry.setTitle(syndEntry.getTitle());
		
		SyndContent content = syndEntry.getDescription();
		
		// FIXME: we need to extract text out of HTML here, and so forth
		entry.setDescription(content.getValue());

		Date publishedDate = syndEntry.getPublishedDate();
		if (publishedDate != null)
			entry.setDate(publishedDate);
		else {
			logger.warn("Failed to retrieve date from feed {}", feed.getSource().getUrl());
			entry.setDate(new Date());
		}
		
		entry.setLink(identitySpider.getLink(entryUrl));
		entry.setCurrent(true);
		
		return entry;
	}
	
	private void updateEntryFromSyndEntry(FeedEntry entry, SyndEntry syndEntry) {
		entry.setTitle(syndEntry.getTitle());
		
		SyndContent content = syndEntry.getDescription();
		
		// FIXME: we need to extract text out of HTML here, and so forth
		entry.setDescription(content.getValue());

		Date publishedDate = syndEntry.getPublishedDate();
		if (publishedDate != null)
			entry.setDate(publishedDate);
		else
			entry.setDate(new Date());
		
		try {
			URL entryUrl = new URL(syndEntry.getLink());
			entry.setLink(identitySpider.getLink(entryUrl));
		} catch (MalformedURLException e) {
		}
	}

	private void setLinkFromSyndFeed(Feed feed, SyndFeed syndFeed) throws XmlMethodException {
		String link = syndFeed.getLink();
		URL linkUrl;
		try {
			linkUrl = new URL(link);
		} catch (MalformedURLException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Feed contains invalid link '" + link + "'");
		}
		feed.setLink(identitySpider.getLink(linkUrl));
	}
	
	private void initializeFeedFromSyndFeed(Feed feed, SyndFeed syndFeed) throws XmlMethodException {
		feed.setTitle(syndFeed.getTitle());
		setLinkFromSyndFeed(feed, syndFeed);
		
		Set<String> foundGuids = new HashSet<String>();
		
		for (Object o : syndFeed.getEntries()) {
			SyndEntry syndEntry = (SyndEntry)o;
			
			String guid = syndEntry.getUri();
			if (foundGuids.contains(guid))
				continue;
			
			if (!foundGuids.contains(guid)) {
				FeedEntry entry;
				try {
					entry = createEntryFromSyndEntry(feed, syndEntry);
					foundGuids.add(guid);
					em.persist(entry);
					feed.getEntries().add(entry);
				} catch (MalformedURLException e) {
					continue;
				}
			}
		}

		feed.setLastFetched(new Date());
		feed.setLastFetchSucceeded(true);
	}
	
	private void updateFeedFromSyndFeed(Feed feed, SyndFeed syndFeed) throws XmlMethodException {
		feed.setTitle(syndFeed.getTitle());
		setLinkFromSyndFeed(feed, syndFeed);
		
		Map<String, FeedEntry> oldEntries = new HashMap<String, FeedEntry>();
		
		for (FeedEntry entry : feed.getEntries()) {
			oldEntries.put(entry.getEntryGuid(), entry);
		}
		
		Set<String> foundGuids = new HashSet<String>();
		
		for (Object o : syndFeed.getEntries()) {
			SyndEntry syndEntry = (SyndEntry)o;
			
			String guid = syndEntry.getUri();
			if (foundGuids.contains(guid))
				continue;
			
			if (oldEntries.containsKey(guid)) {
				updateEntryFromSyndEntry(oldEntries.get(guid), syndEntry);
				foundGuids.add(guid);
				continue;
			}
			
			if (!foundGuids.contains(guid)) {
				FeedEntry entry;
				try {
					entry = createEntryFromSyndEntry(feed, syndEntry);
					foundGuids.add(guid);
					em.persist(entry);
					feed.getEntries().add(entry);
				} catch (MalformedURLException e) {
					continue;
				}
			}
		}
		
		for (FeedEntry entry : feed.getEntries()) {
			if (!foundGuids.contains(entry.getEntryGuid()))
				entry.setCurrent(false);
			else if (oldEntries.containsKey(entry))
				entry.setCurrent(true);
		}

		feed.setLastFetched(new Date());
		feed.setLastFetchSucceeded(true);

		for (final FeedEntry entry : feed.getEntries()) {
			if (foundGuids.contains(entry.getEntryGuid()) && !oldEntries.containsKey(entry.getEntryGuid())) {
				logger.debug("Found new Feed entry: {}", entry.getTitle());
				getNotificationService().submit(new Runnable() {
					public void run() {
						EJBUtil.defaultLookup(FeedSystem.class).handleNewEntryNotification(entry.getId());
					}
				});
			}
		}
	}
	
	public Feed getFeed(final LinkResource source) throws XmlMethodException {
		Feed feed = lookupExistingFeed(source);
		if (feed != null)
			return feed;
		
		final SyndFeed syndFeed = fetchFeedFromNet(source);
		
		try {
			Feed detached = runner.runTaskRetryingOnConstraintViolation(new Callable<Feed>() {
				
				public Feed call() throws Exception {
					Feed newFeed = lookupExistingFeed(source);
					if (newFeed != null) // Someone else already looked it up and stored it
						return newFeed;
					
					// source is not part of the session, but only it's ID is needed for this
					newFeed = new Feed(source);
					em.persist(newFeed);
										
					initializeFeedFromSyndFeed(newFeed, syndFeed);
					
					return newFeed;
				}
			});
			
			return em.find(Feed.class, detached.getId());
			
		} catch (Exception e) {
			if (e instanceof XmlMethodException)
				throw (XmlMethodException) e;
			else
				throw new RuntimeException("Error initializing feed from download result " + source.getUrl(), e);
		}
	}

	public void updateFeed(Feed feed) {
		// Needed when called from the update thread
		if (!em.contains(feed)) {
			feed = em.find(Feed.class, feed.getId());
		}
		
		if (System.currentTimeMillis() - feed.getLastFetched().getTime() < FEED_UPDATE_TIME)
			return; // Up-to-date, nothing to do

		logger.debug("Feed {} needs update", feed.getSource());
		
		try {
			final SyndFeed syndFeed = fetchFeedFromNet(feed.getSource());
			updateFeedFromSyndFeed(feed, syndFeed);		
		} catch (XmlMethodException e) {
			logger.warn("Couldn't update feed", e);
			
			feed.setLastFetched(new Date());
			feed.setLastFetchSucceeded(false);
		}
	}

	public List<FeedEntry> getCurrentEntries(Feed feed) {
		List<FeedEntry> result = new ArrayList<FeedEntry>();
		
		for (FeedEntry entry : feed.getEntries()) {
			if (entry.isCurrent())
				result.add(entry);
		}
		
		Collections.sort(result, new Comparator<FeedEntry>() {
			public int compare(FeedEntry a, FeedEntry b) {
				return - a.getDate().compareTo(b.getDate());
			}
		});
		
		return result;
	}
	
	public List<Feed> getInUseFeeds() {
		List l = em.createQuery("SELECT f FROM Feed f WHERE EXISTS (SELECT gf FROM GroupFeed gf WHERE gf.feed = f)").getResultList();
		return TypeUtils.castList(Feed.class, l);
	}
	
	public void handleNewEntryNotification(long entryId) {
		FeedEntry entry = em.find(FeedEntry.class, entryId);
		
		logger.debug("Processing feed entry: {}", entry.getTitle());
	}
	
	public synchronized static void startup() {
		FeedUpdater.getInstance().start();
	}
	
	public synchronized static void shutdown() {
		shutdown = true;

		FeedUpdater.getInstance().shutdown();
		if (notificationService != null) {
			notificationService.shutdown();
			notificationService = null;
		}
	}

	private static class FeedUpdater extends Thread {
		private static FeedUpdater instance;
		
		static synchronized FeedUpdater getInstance() {
			if (instance == null)
				instance = new FeedUpdater();
			
			return instance;
		}
		
		public FeedUpdater() {
			super("FeedUpdater");
		}
		
		@Override
		public void run() {
			// We start off by sleeping for our delay time to reduce the initial
			// server load on restart
			long lastUpdate = System.currentTimeMillis();
			
			while (true) {
				try {
					long sleepTime = lastUpdate + UPDATE_THREAD_TIME - System.currentTimeMillis();
					if (sleepTime < 0)
						sleepTime = 0;
					Thread.sleep(sleepTime);
					
					// We intentionally iterate here rather than inside a session
					// bean method to get a separate transaction for updating each
					// feed rather than holding a single transaction over the whole
					// process.
					FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);
					for (Feed feed : feedSystem.getInUseFeeds()) {
						feedSystem.updateFeed(feed);
					}
					
					lastUpdate = System.currentTimeMillis();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		
		public void shutdown() {
			interrupt();
			
			try {
				join();
				logger.info("Successfully stopped FeedUpdater thread");
			} catch (InterruptedException e) {
				// Shouldn't happen, just ignore
			}
		}
	}

	public void addGroupFeed(Group group, Feed feed) {
		for (GroupFeed old : group.getFeeds()) {
			if (old.getFeed().equals(feed)) {
				return;
			}
		}
		GroupFeed groupFeed = new GroupFeed(group, feed);
		em.persist(groupFeed);
		group.getFeeds().add(groupFeed);
	}
	
	public void removeGroupFeed(Group group, Feed feed) {
		for (GroupFeed old : group.getFeeds()) {
			if (old.getFeed().equals(feed)) {
				group.getFeeds().remove(old);				
				em.remove(old);
				return;
			}
		}
	}
}
