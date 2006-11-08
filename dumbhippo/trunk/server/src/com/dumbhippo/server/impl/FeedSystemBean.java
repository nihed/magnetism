package com.dumbhippo.server.impl;

import java.io.IOException;
import java.io.Serializable;
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
import java.util.regex.Pattern;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.mbean.FeedUpdaterPeriodicJob;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.TrackFeedEntry;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.syndication.RhapModule;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.HtmlTextExtractor;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.FetcherException;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;
import com.sun.syndication.fetcher.impl.SyndFeedInfo;
import com.sun.syndication.io.FeedException;

@Stateless
public class FeedSystemBean implements FeedSystem {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(FeedSystemBean.class);	
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;	
	
	@EJB
	private IdentitySpider identitySpider;
	
	@EJB
	private PostingBoard postingBoard;

	@EJB
	private Notifier notifier;
	
	private static FeedFetcherCache cache = null;
	private static ExecutorService notificationService;
	private static boolean shutdown = false;
	
	private static synchronized FeedFetcherCache getCache() {
		if (cache == null)
			cache = new FeedCache();
		return cache;
	}
	
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
		} catch (NoResultException e) {
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
		
		// FIXME unfortunately the timeout on the feed fetcher http download is 
		// way too long - but there's no way to fix without hacking on ROME.
		// Doing the HTTP GET by hand is not really desirable since the feed fetcher
		// is smarter than that (e.g. uses some "get new stuff only" protocols, checks
		// whether the data has changed, etc.)
		
		FeedFetcher feedFetcher = new HttpURLFeedFetcher(getCache());
		try {
			return feedFetcher.retrieveFeed(url);
		} catch (IOException e) {
			// log this here since we lose the details in the potentially-user-visible XmlMethodException
			logger.warn("Network exception retrieving feed was {}: '{}' on url " + url, e.getClass().getName(), e.getMessage());
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Network error fetching feed " + url);
		} catch (FetcherException e) {
			// log this here since we lose the details in the potentially-user-visible XmlMethodException
			logger.warn("Fetcher exception retrieving feed was {}: '{}' on url " + url, e.getClass().getName(), e.getMessage());
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Error requesting feed from server " + url);
		} catch (FeedException e) {
			// log this here since we lose the details in the potentially-user-visible XmlMethodException
			logger.warn("Feed exception retrieving feed was {}: '{}' on url " + url, e.getClass().getName(), e.getMessage());
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Error parsing feed " + url);
		}
	}
	
	private static final Pattern externalWhitespace = Pattern.compile("(^[\r\n\t ]+)|(^[\r\n\t ]+?$)");
	private static final Pattern internalWhitespace = Pattern.compile("[\r\n\t ]+");

	// Some simple handling of plain text, removing irrelevant whitespace
	private String prepPlainText(String value) {
		String temp = externalWhitespace.matcher(value).replaceAll("");
		return internalWhitespace.matcher(temp).replaceAll(" ");
	}
	
	private String getContentAsText(SyndFeed feed, SyndContent content) {
		boolean isDefinitelyText = false;
		
		// The content of HTML entries is very poorly defined; RSS 1.0
		// defines that the contents must be plain text, but HTML content
		// is frequently used anyways. RSS 2.0 explicitely allows HTML
		// entities (without defining what is allowed in particular), but
		// Rome sets a type on content of text/plain, anyways :-(
		// 
		// We skip HTML tag stripping for a few cases where we have strong
		// evidence that text is required
		
		if (feed.getFeedType().equals("atom_0.3")) {
			if (content.getType().equals("text/plain"))
				isDefinitelyText = true;
		} else if (feed.getFeedType().equals("atom_1.0")) {
			if (content.getType().equals("text") || content.getType().equals("text/plain"))
				isDefinitelyText = true;
		}
		
		if (isDefinitelyText)
			return prepPlainText(content.getValue());
		else
			return HtmlTextExtractor.extractText(content.getValue());
	}

	private URL entryURLFromSyndEntry(SyndEntry syndEntry) throws MalformedURLException {
		String entryLink = syndEntry.getLink();
		if (entryLink == null)
			throw new MalformedURLException("No link in synd entry");
		entryLink = entryLink.trim();
		
		return new URL(entryLink);
	}
	
	private FeedEntry createEntryFromSyndEntry(Feed feed, SyndFeed syndFeed, SyndEntry syndEntry) throws MalformedURLException {
		URL entryUrl = entryURLFromSyndEntry(syndEntry);
		
		FeedEntry entry = null;
		
		List<Module> modules = TypeUtils.castList(Module.class, syndEntry.getModules());
		for (Module m : modules) {
			if (m instanceof RhapModule) {
				RhapModule module = (RhapModule)m;
				
				// logger.debug("RhapModule found for feedentry from feed {}", feed.getSource().getUrl());
				TrackFeedEntry trackFeedEntry = new TrackFeedEntry(feed);
				
				trackFeedEntry.setArtist(module.getArtist());
				trackFeedEntry.setArtistRCID(module.getArtistRCID());
				trackFeedEntry.setAlbum(module.getAlbum());
				trackFeedEntry.setAlbumRCID(module.getAlbumRCID());
				trackFeedEntry.setTrack(module.getTrack());
				trackFeedEntry.setTrackRCID(module.getTrackRCID());
				trackFeedEntry.setDuration(module.getDuration());
				trackFeedEntry.setPlayHref(module.getPlayHref());
				trackFeedEntry.setDataHref(module.getDataHref());
				trackFeedEntry.setAlbumArt(module.getAlbumArt());
				
				/*
				logger.debug("RhapModule artist: {}", module.getArtist());
				logger.debug("RhapModule album : {}", module.getAlbum());
				logger.debug("RhapModule track : {}", module.getTrack());
				logger.debug("RhapModule play  : {}", module.getPlayHref());
				logger.debug("RhapModule data  : {}", module.getDataHref());
				*/
				
				entry = trackFeedEntry;				
			}
		}
		
		if (entry == null) {
			// logger.debug("RhapModule not found for feedentry from feed {}", feed.getSource().getUrl());
			entry = new FeedEntry(feed);
		}
			
		entry.setEntryGuid(syndEntry.getUri());

		String title = syndEntry.getTitle();
		if (title != null) // probably never null, but who knows what rome does
			entry.setTitle(HtmlTextExtractor.extractText(title));
		else
			entry.setTitle("(untitled)");
		
		SyndContent content = syndEntry.getDescription();
		if (content != null)
			entry.setDescription(getContentAsText(syndFeed, content));
		else
			entry.setDescription("");

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
	
	private void processFeedExternalAccounts(FeedEntry entry, int entryPosition) {
		// this is just a check for debugging purposes to make sure we handle all TrackFeedEntry objects well
		if ((entry instanceof TrackFeedEntry) && entry.getFeed().getAccounts().isEmpty()) {
			logger.warn("processExternalAccountFeed called for TrackFeedEntry {}, but no accounts associated with feed", entry);
		}		
		
		for (ExternalAccount external : entry.getFeed().getAccounts()) {
			
			User owner = external.getAccount().getOwner();
			
			// FIXME this is kind of expensive, since we instantiate and invoke a callback 
			// on all session beans that care about any kind of external account feed 
			// entry, even though most of them only care about one kind.
			// The fix in the current framework would be to have 
			// separate Listener interfaces for each external account type; 
			// which might be the right fix, but would introduce a big switch() right 
			// here. Anyway, probably not a problem for now.
			notifier.onExternalAccountFeedEntry(owner, external, entry, entryPosition);
		}
	}
	
	private void setLinkFromSyndFeed(Feed feed, SyndFeed syndFeed) throws XmlMethodException {
		String link = syndFeed.getLink();
		if (link == null) {
			if (feed.getLink() != null) {
				// theory is that on an incremental update maybe this happens, we'll see
				logger.debug("Feed already has a link, new SyndFeed does not, sticking to the old one. {}", feed);
				return;
			} else {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Feed contains no link element or we failed to parse one at least");
			}
		} else {
			URL linkUrl;
			try {
				linkUrl = new URL(link);
			} catch (MalformedURLException e) {
				throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Feed contains invalid link '" + link + "' " + e.getMessage());
			}
			feed.setLink(identitySpider.getLink(linkUrl));
		}
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
					entry = createEntryFromSyndEntry(feed, syndFeed, syndEntry);
					foundGuids.add(guid);
					em.persist(entry);
					feed.getEntries().add(entry);
					// This won't work at the moment because the feed hasn't yet been associated with an account yet
					// Would also need to figure out what entryPosition should be
				    // processFeedExternalAccounts(entry, 0);
				} catch (MalformedURLException e) {
					logger.debug("ignoring feed entry with bogus url on {}: {}", feed.getSource(), e.getMessage());
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
		int entryPosition = 0;
		for (Object o : syndFeed.getEntries()) {
			SyndEntry syndEntry = (SyndEntry)o;
			
			String guid = syndEntry.getUri();
			if (foundGuids.contains(guid))
				continue;
			if (oldEntries.containsKey(guid)) {
				// We don't try to update old entries, because it is painful and expensive:
				// The most interesting thing to update is the description, and we only store the 
				// extracted version of the description, so we have to actually do the extraction 
				// before we can tell if the description has changed. We also have to deal with 
				// the fact that description is truncated based on database limits, when comparing 
				// the existing value with the new value.
				foundGuids.add(guid);
				continue;
			}
			
			if (!foundGuids.contains(guid)) {
				FeedEntry entry;
				try {
					entry = createEntryFromSyndEntry(feed, syndFeed, syndEntry);
					foundGuids.add(guid);
					em.persist(entry);
					feed.getEntries().add(entry);
					// if this feed is associated with some external account(s), this function will take 
					// care of what needs to happen
					processFeedExternalAccounts(entry, entryPosition++);
				} catch (MalformedURLException e) {
					logger.debug("ignoring feed entry with bogus url on {}: {}", feed.getSource(), e.getMessage());
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

		final List<Long> newEntryIds = new ArrayList<Long>();
		for (FeedEntry entry : feed.getEntries()) {
			if (foundGuids.contains(entry.getEntryGuid()) && !oldEntries.containsKey(entry.getEntryGuid())) {
				logger.debug("  Found new Feed entry: {}", entry.getTitle());
				
				newEntryIds.add(entry.getId());
			}
		}
		
		// The feed entries aren't visible to the notification service 
		// thread until after we commit this transaction, so don't submit them
		// to that thread until then.
		if (!newEntryIds.isEmpty()) {
			runner.runTaskOnTransactionCommit(new Runnable() {
				public void run() {
					logger.debug("  Transaction committed, running new entry notification for {} entries", newEntryIds.size());
					for (final long entryId : newEntryIds) {
						getNotificationService().execute(new Runnable() {
							public void run() {
								try {
									EJBUtil.defaultLookup(FeedSystem.class).handleNewEntryNotification(entryId);
								} catch (RuntimeException e) {
									logger.error("Exception handling feed entry notification", e);
								}
							}
						});
					}
				}
			});
		} else {
			logger.debug("  No new entries for feed {}", feed.getSource());
		}
	}
	
	public Feed getFeed(final LinkResource source) throws XmlMethodException {
		Feed feed = lookupExistingFeed(source);
		if (feed != null)
			return feed;
		
		final SyndFeed syndFeed = fetchFeedFromNet(source);
		
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<Feed>() {
				
				public Feed call() throws Exception {
					Feed newFeed = lookupExistingFeed(source);
					if (newFeed != null) // Someone else already looked it up and stored it
						return newFeed;
					
					newFeed = new Feed(source);
					em.persist(newFeed);
										
					initializeFeedFromSyndFeed(newFeed, syndFeed);
					
					return newFeed;
				}
			});
			
		} catch (Exception e) {
			if (e instanceof XmlMethodException)
				throw (XmlMethodException) e;
			else
				throw new RuntimeException("Error initializing feed from download result " + source.getUrl() + ": " + e.getMessage(), e);
		}
	}

	static class UpdateFeedContext {
		private long feedId;
		private SyndFeed syndFeed;
		
		UpdateFeedContext(long feedId, SyndFeed syndFeed) {
			this.feedId = feedId;
			this.syndFeed = syndFeed;
		}

		public long getFeedId() {
			return feedId;
		}

		public SyndFeed getSyndFeed() {
			return syndFeed;
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean updateFeedNeedsFetch(Feed feed) {
		long updateTime = feed.getLastFetchSucceeded() ? FeedUpdaterPeriodicJob.FEED_UPDATE_TIME : FeedUpdaterPeriodicJob.BAD_FEED_UPDATE_TIME;
		
		if ((System.currentTimeMillis() - feed.getLastFetched().getTime()) < updateTime) {
			//logger.debug("  Feed {} is already up-to-date", feed);
			return false; // Up-to-date, nothing to do
		} else {
			return true;
		}
	}
	
	// NOT_SUPPORTED would suspend the parent transaction; SUPPORTS means
	// we just kind of leave the parent transaction as-is, which seems most 
	// appropriate; we should not be using it.
	// Ultimately NEVER (prohibit a live transaction) would be most correct
	// here, but is some extra work since we can be called from HttpMethodsBean.
	// FIXME this is easy to repair now that HttpMethodsBean has a "no transaction"
	// annotation
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Object updateFeedFetchFeed(Feed feed) throws XmlMethodException {
		// The feed here need not be attached, and in fact we should not 
		// try to use the database in this method
		
		logger.debug("  Feed {} being fetched", feed.getSource());
		
		try {
			final SyndFeed syndFeed = fetchFeedFromNet(feed.getSource());
			logger.info("  HTTP request completed for feed {}, retrieved {} entries", feed.getSource(), feed.getEntries().size());
			return new UpdateFeedContext(feed.getId(), syndFeed);		
		} catch (XmlMethodException e) {
			logger.warn("Couldn't update feed {}: " + e.getCodeString() + ": {}", feed.getSource(), e.getMessage());
			throw e;
		}
	}

	// see docs in the interface for what's going on here.
	// This is purely to avoid logfile clutter; it isn't needed
	// for correctness.
	public void updateFeedInternLinks(Object contextObject) {
		UpdateFeedContext context = (UpdateFeedContext) contextObject;
		SyndFeed syndFeed = context.getSyndFeed();
		for (Object o : syndFeed.getEntries()) {
			SyndEntry syndEntry = (SyndEntry)o;
			URL entryUrl;
			try {
				entryUrl = entryURLFromSyndEntry(syndEntry);
				identitySpider.getLink(entryUrl);
			} catch (MalformedURLException e) {
				// just ignore, it will break later on
			}
		}
	}
	
	public void updateFeedStoreFeed(Object contextObject) throws XmlMethodException {
		UpdateFeedContext context = (UpdateFeedContext) contextObject;
		SyndFeed syndFeed = context.getSyndFeed();
		Feed feed = em.find(Feed.class, context.getFeedId());
		logger.debug("  Saving feed update results in db for {}", feed.getSource());
		try {
			updateFeedFromSyndFeed(feed, syndFeed);
		} catch (XmlMethodException e) {
			logger.warn("Couldn't store feed {}: " + e.getCodeString() + ": {}", feed.getSource(), e.getMessage());
			throw e;
		}
	}
	
	public void markFeedFailedLastUpdate(Feed feed) {
		logger.debug("  Marking feed {} as failed to update", feed.getSource());

		// Needed when called from the update thread		
		if (!em.contains(feed)) {
			feed = em.find(Feed.class, feed.getId());
		}
		
		feed.setLastFetched(new Date());
		feed.setLastFetchSucceeded(false);		
	}
	
	public void updateFeed(Feed feed) {
		if (!updateFeedNeedsFetch(feed)) {
			return;
		}
		
		try {
			Object o = updateFeedFetchFeed(feed);
			if (o != null)
				updateFeedStoreFeed(o);
		} catch (XmlMethodException e) {
			markFeedFailedLastUpdate(feed);
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
	
	public FeedEntry getLastEntry(Feed feed) {
		// this could get smarter if we want to return the entry that was added last,
		// which might not necessarily be the same one that has the latest publishing date
		// (for example, if some blogs allow back dating entries)
		// this could also be change to return a given number of recent entries
		List<FeedEntry> entries = getCurrentEntries(feed);
		if (entries.size() < 1)
			return null;
		
		return entries.get(0);
	}
	
	public List<Feed> getInUseFeeds() {
		List l = em.createQuery("SELECT f FROM Feed f WHERE EXISTS " + 
				" (SELECT gf FROM GroupFeed gf WHERE gf.feed = f AND gf.removed = 0) OR " +
				" EXISTS (SELECT ea FROM ExternalAccount ea WHERE ea.feed = f AND ea.sentiment = :loved)")
				.setParameter("loved", Sentiment.LOVE)
				.getResultList();
		return TypeUtils.castList(Feed.class, l);
	}
	
	public void handleNewEntryNotification(long entryId) {
		FeedEntry entry = em.find(FeedEntry.class, entryId);
		if (entry == null)
			throw new RuntimeException("database isolation problem (?): entry Id doesn't exist " + entryId);
		logger.debug("Processing feed entry: {}", entry);
		
		for (GroupFeed feed : entry.getFeed().getGroups()) {
			if (!feed.isRemoved()) {
				// FIXME the below comment is bogus because once a transaction 
				// is hosed you can't keep doing other stuff ... to do this 
				// each FeedPost needs creating in a new transaction or something.
				// And failed transactions should probably retry.
				
				// catch errors here so failure to post to one group 
				// won't break all other groups that want the same
				// entry; there's nobody to catch this exception anyhow.
				try {
					postingBoard.doFeedPost(feed, entry);
				} catch (RuntimeException e) {
					logger.error("Error posting feed entry {} to GroupFeed {}",
							entry, feed);
					logger.error("Exception posting feed", e);
				}
			}
		}
	}
	
	public synchronized static void startup() {
	}
	
	public synchronized static void shutdown() {
		shutdown = true;
		
		if (notificationService != null) {
			ThreadUtils.shutdownAndAwaitTermination(notificationService);
			notificationService = null;
		}
	}

	public void addGroupFeed(Group group, Feed feed) {
		for (GroupFeed old : group.getFeeds()) {
			if (old.getFeed().equals(feed)) {
				old.setRemoved(false);
				return;
			}
		}
		EJBUtil.forceInitialization(feed.getGroups());
		GroupFeed groupFeed = new GroupFeed(group, feed);
		em.persist(groupFeed);
		group.getFeeds().add(groupFeed);
		feed.getGroups().add(groupFeed);
	}
	
	public void removeGroupFeed(Group group, Feed feed) {
		for (GroupFeed old : group.getFeeds()) {
			if (old.getFeed().equals(feed)) {
				old.setRemoved(true);
				return;
			}
		}
	}
		
	/** 
	 * For now the only point of this vs. the one that comes with Rome is that
	 * we can do logging.
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
	
	public String getUrlToScrapeFaviconFrom(String untrustedFeedSourceId) throws NotFoundException {
		LinkResource link;
		try {
			link = identitySpider.lookupGuidString(LinkResource.class, untrustedFeedSourceId);
		} catch (ParseException e) {
			throw new NotFoundException("invalid guid", e);
		}
		
		Feed feed = lookupExistingFeed(link);
		if (feed == null)
			throw new NotFoundException("No known feed with the source link id '" + untrustedFeedSourceId + "'");
		
		return getUrlToScrapeFaviconFrom(feed);
	}
	
	public String getUrlToScrapeFaviconFrom(Feed feed) throws NotFoundException {
		if (feed.getLink() == null)
			throw new NotFoundException("No web site associated with feed " + feed);
		else
			return feed.getLink().getUrl();		
	}
	
	// FIXME is this used?
	public void create() throws Exception {
		logger.info("Creating FeedSystemBean");
	}

	// FIXME is this used?
	public void destroy() {
		logger.info("Destroying FeedSystemBean");
	}

	// FIXME is this used?
	public void start() throws Exception {
		logger.info("Starting FeedSystemBean");
	}

	// FIXME is this used?
	public void stop() {
		logger.info("Stopping FeedSystemBean");
	}
}
