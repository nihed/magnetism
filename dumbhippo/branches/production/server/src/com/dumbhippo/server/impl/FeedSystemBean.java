package com.dumbhippo.server.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.ThreadUtils;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.mbean.DynamicPollingSystem;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTask;
import com.dumbhippo.mbean.DynamicPollingSystem.PollingTaskFamily;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.GroupFeedAddedRevision;
import com.dumbhippo.persistence.GroupFeedRemovedRevision;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.PollingTaskEntry;
import com.dumbhippo.persistence.PollingTaskFamilyType;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.TrackFeedEntry;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.FeedSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.PollingTaskPersistence;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.RevisionControl;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.XmlMethodErrorCode;
import com.dumbhippo.server.XmlMethodException;
import com.dumbhippo.server.syndication.RhapModule;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.util.FeedScraper;
import com.dumbhippo.server.util.HtmlTextExtractor;
import com.dumbhippo.services.FeedFetcher;
import com.dumbhippo.services.FeedFetcher.FeedFetchResult;
import com.dumbhippo.services.FeedFetcher.FetchFailedException;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

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
	
	@EJB
	private PollingTaskPersistence pollingPersistence;
	
	@EJB
	private RevisionControl revisionControl;
	
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
		} catch (NoResultException e) {
			return null;
		}
	}
	
	private static FeedFetchResult getRawFeed(LinkResource source) throws FetchFailedException {
		URL url;
		try {
			url = new URL(source.getUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException("getFeed passed malformed URL object");
		}
		return FeedFetcher.getFeed(url);
	}	
	
	private FeedFetchResult getRawFeedChecked(LinkResource source) throws XmlMethodException {
		URL url;
		try {
			url = new URL(source.getUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException("getFeed passed malformed URL object");
		}

		try {
			return FeedFetcher.getFeed(url);
		} catch (FeedFetcher.FetchFailedException e) {
			// log this here since we lose the details in the
			// potentially-user-visible XmlMethodException
			logger.warn("Exception retrieving feed was {}: '{}' on url " + url, e.getClass().getName(), e.getMessage());
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR,
					"Error fetching feed " + url + ": " + e.getMessage());
		}
	}
	
	private static final Pattern externalWhitespace = Pattern.compile("(^[\r\n\t ]+)|(^[\r\n\t ]+?$)");
	private static final Pattern internalWhitespace = Pattern.compile("[\r\n\t ]+");

	private static final long FEED_UPDATE_TIME = 1000 * 60 * 10; // 10 minutes

	private static final long BAD_FEED_UPDATE_TIME = 1000 * 60 * 60 * 7; // 7 hours

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
			return prepPlainText(content.getValue()).trim();
		else
			return HtmlTextExtractor.extractText(content.getValue()).trim();
	}

	private URL entryURLFromSyndEntry(URL baseUrl, SyndEntry syndEntry) throws MalformedURLException {
		String entryLink = syndEntry.getLink();
		if (entryLink == null)
			throw new MalformedURLException("No link in synd entry");
		entryLink = entryLink.trim();
		
		return new URL(baseUrl, entryLink);
	}
	
	private static class NoEntryGuidException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	private String makeEntryGuid(SyndEntry syndEntry) throws NoEntryGuidException {
		String guid = syndEntry.getUri();
		// Null does occur in practice, if rarely, we just silently such entries
		// assuming that they won't be interesting things to post in any case. 
		if (guid == null) 
			throw new NoEntryGuidException();
		
		if (guid.length() <= FeedEntry.MAX_ENTRY_GUID_LENGTH)
			return guid;
		else
			return guid.substring(0, FeedEntry.MAX_ENTRY_GUID_LENGTH);
	}
	
	private FeedEntry createEntryFromSyndEntry(Feed feed, SyndFeed syndFeed, SyndEntry syndEntry) throws MalformedURLException {
		URL entryUrl = entryURLFromSyndEntry(new URL(feed.getLink().getUrl()), syndEntry);
		
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
			
		try {
			entry.setEntryGuid(makeEntryGuid(syndEntry));
		} catch (NoEntryGuidException e) {
			throw new RuntimeException(e); // We should have filtered such entries out before
		}

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
			logger.warn("Failed to retrieve date from feed entry on {}", feed.getSource().getUrl());
			entry.setDate(new Date());
		}
		
		entry.setLink(identitySpider.getLink(entryUrl));
		entry.setCurrent(true);
		
		return entry;
	}
	
	private void processFeedExternalAccounts(FeedEntry entry, int entryPosition) {
		// this is just a check for debugging purposes to make sure we handle all TrackFeedEntry objects well
		if ((entry instanceof TrackFeedEntry) && entry.getFeed().getAccounts().isEmpty()) {
			logger.warn("processFeedExternalAccounts called for TrackFeedEntry {}, but no accounts associated with feed", entry);
		}		
		
		logger.debug("Processing external accounts for new feed entry {}", entry);
		
		for (ExternalAccount external : entry.getFeed().getAccounts()) {
			
			logger.debug("External account {}", external);
			
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
	
	public static class FeedLinkUnknownException extends Exception {
		private static final long serialVersionUID = 1L;
		public FeedLinkUnknownException(String message) {
			super(message);
		}
	}
	
	private void setLinkFromSyndFeed(Feed feed, SyndFeed syndFeed) throws FeedLinkUnknownException {
		String link = syndFeed.getLink();
		if (link == null) {
			if (feed.getLink() != null) {
				// theory is that on an incremental update maybe this happens, we'll see
				logger.debug("Feed already has a link, new SyndFeed does not, sticking to the old one. {}", feed);
				return;
			} else {
				throw new FeedLinkUnknownException("Feed contains no link element or we failed to parse one at least");
			}
		} else {
			URL linkUrl;
			try {
				linkUrl = new URL(link);
			} catch (MalformedURLException e) {
				throw new FeedLinkUnknownException("Feed contains invalid link '" + link + "' " + e.getMessage());
			}
			feed.setLink(identitySpider.getLink(linkUrl));
		}
	}
	
	private void initializeFeedFromSyndFeed(Feed feed, SyndFeed syndFeed) throws FeedLinkUnknownException {
		feed.setTitle(syndFeed.getTitle());
		setLinkFromSyndFeed(feed, syndFeed);
		
		Set<String> foundGuids = new HashSet<String>();
		
		for (Object o : syndFeed.getEntries()) {
			SyndEntry syndEntry = (SyndEntry)o;
			
			String guid;
			try {
				guid = makeEntryGuid(syndEntry);
			} catch (NoEntryGuidException e1) {
				continue; // Silently ignore; its hard to log usefully
			}
			
			if (foundGuids.contains(guid))
				continue;
			
			if (!foundGuids.contains(guid)) {
				FeedEntry entry;
				try {
					entry = createEntryFromSyndEntry(feed, syndFeed, syndEntry);
					foundGuids.add(guid);
					em.persist(entry);
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
	
	private boolean updateFeedFromSyndFeed(Feed feed, SyndFeed syndFeed) throws FeedLinkUnknownException {
		feed.setTitle(syndFeed.getTitle());
		setLinkFromSyndFeed(feed, syndFeed);
		Map<String, FeedEntry> lastEntries = new HashMap<String, FeedEntry>();
		
		// We want to keep a complete set of old GUIDs in the database to
		// keep from reposting entries to feeds when something upstream of
		// us breaks, but loading them all up every time would be hideously
		// expensive, so what we do is only load up the entries we saw
		// last time, and then when we get something not in that set, 
		// explicitly check with a separate query to make sure that it isn't
		// an old resurrected GUID.
		
		for (FeedEntry entry : getCurrentEntries(feed))
			lastEntries.put(entry.getEntryGuid(), entry);
		
		final List<Long> newEntryIds = new ArrayList<Long>();
		Set<String> foundGuids = new HashSet<String>();
		
		int entryPosition = 0;
		for (Object o : syndFeed.getEntries()) {
			SyndEntry syndEntry = (SyndEntry)o;
			
			String guid;
			try {
				guid = makeEntryGuid(syndEntry);
			} catch (NoEntryGuidException e1) {
				continue;
			}
			
			if (foundGuids.contains(guid))
				continue;
			
			if (lastEntries.containsKey(guid)) {
				// We don't try to update old entries, because it is painful and expensive:
				// The most interesting thing to update is the description, and we only store the 
				// extracted version of the description, so we have to actually do the extraction 
				// before we can tell if the description has changed. We also have to deal with 
				// the fact that description is truncated based on database limits, when comparing 
				// the existing value with the new value.
				foundGuids.add(guid);
				continue;
			}
			
			// At this point we tentatively think that we have a new entry, but it
			// could also be a resurrected entry - one that was returned by the feed
			// earlier, vanished from the feed - then came back
			
			try {
				Query q = em.createQuery("SELECT fe FROM FeedEntry fe WHERE fe.feed = :feed AND fe.entryGuid = :guid");
				q.setParameter("feed", feed);
				q.setParameter("guid", guid);
				
				// It was resurrected
				FeedEntry old = (FeedEntry)q.getSingleResult();
				old.setCurrent(true);
				foundGuids.add(guid);
				continue;
				
			} catch (NoResultException e) {
				// not already there, we are good to go
			}
			
			long now = System.currentTimeMillis();
			try {
				FeedEntry entry = createEntryFromSyndEntry(feed, syndFeed, syndEntry);
				em.persist(entry);
				
				long age = now - entry.getDate().getTime();
				
				// never create/stack blocks for stuff older than 14 days, prevents 
				// a huge deluge of old crap if people change how their blog works 
				if (age < 1000 * 60 * 60 * 24 * 7 * 2) {
					// if this feed is associated with some external account(s), this function will take 
					// care of what needs to happen
					processFeedExternalAccounts(entry, entryPosition++);
					// these newEntryIds get posted to subscribed groups
					newEntryIds.add(entry.getId());
				} else {
					logger.debug("Feed entry {} too old - not stacking", entry, entry.getDate());
				}
				
				foundGuids.add(guid);
				
				logger.debug("  Found new Feed entry: {}", entry.getTitle());
			} catch (MalformedURLException e) {
				logger.debug("ignoring feed entry with bogus url on {}: {}", feed.getSource(), e.getMessage());
				continue;
			}
		}

		// Mark any old items as no longer current
		for (FeedEntry entry : lastEntries.values()) {
			if (!foundGuids.contains(entry.getEntryGuid()))
				entry.setCurrent(false);
		}

		feed.setLastFetched(new Date());
		feed.setLastFetchSucceeded(true);

		// The feed entries aren't visible to the notification service 
		// thread until after we commit this transaction, so don't submit them
		// to that thread until then.
		if (!newEntryIds.isEmpty()) {
			// Feeds generally have their entries in reverse order - with the most recent first
			// but things work better if we process in chronological order, so we reverse
			// first. (Another option would be to sort on FeedEntry.getDate())
			//
			// Note that this reversal doesn't apply to the call to processFeedExternalAccounts()
			// above. If that ever matters, we would need to preserve the entry index down
			// to this point, or otherwise restructure things.
			//
			Collections.reverse(newEntryIds);
			
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
			logger.debug("  No new entries to process for feed {}", feed.getSource());
			return false;
		}
		return true;
	}

	public Feed scrapeFeedFromUrl(URL url) throws XmlMethodException {
		FeedScraper scraper = new FeedScraper();
		try {
			// This downloads the url contents, and if it's already an RSS feed then FeedSystem will do it again 
			// if it's not cached... but since 1) usually we'll be downloading html and not rss here and 2) many feeds
			// will be cached, it's really not worth making a mess to move the downloaded bytes from FeedScraper to FeedSystem
			scraper.analyzeURL(url);
		} catch (IOException e) {
			logger.debug("IO error analyzing url {}: {}", url, e.getMessage());
			throw new XmlMethodException(XmlMethodErrorCode.NETWORK_ERROR, "Unable to contact the site (" + e.getMessage() + ")");
		}
		URL feedSource = scraper.getFeedSource();
		if (feedSource == null) {
			logger.debug("No feed found at url {}", url);
			throw new XmlMethodException(XmlMethodErrorCode.INVALID_URL, "Couldn't find a feed at " + url);
		}		
		LinkResource link = identitySpider.getLink(feedSource);
		Feed feed = getOrCreateFeed(link);
		return feed;
	}
	
	public Feed getExistingFeed(final LinkResource source) throws XmlMethodException {
		Feed feed = lookupExistingFeed(source);
		if (feed != null)
			return feed;
		else
			throw new XmlMethodException(XmlMethodErrorCode.NOT_FOUND, "No such feed: " + source.getUrl());
	}
	
	public Feed getOrCreateFeed(final LinkResource source) throws XmlMethodException {
		Feed feed = lookupExistingFeed(source);
		if (feed != null) {
			/* We want to ensure that we create a task for feeds which existed before
			 * the PollingTask system was implemented as well, which were not caught
			 * by migration because they were not in use when it was run.
			 */
			pollingPersistence.createTaskIdempotent(PollingTaskFamilyType.FEED, feed.getSource().getId());			
			return feed;
		}
		
		FeedFetchResult fetchResult = getRawFeedChecked(source);
		final SyndFeed syndFeed = fetchResult.getFeed();
		
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<Feed>() {
				
				public Feed call() throws XmlMethodException {
					Feed newFeed = lookupExistingFeed(source);
					if (newFeed != null) // Someone else already looked it up and stored it
						return newFeed;
					
					newFeed = new Feed(source);
					em.persist(newFeed);
										
					try {
						initializeFeedFromSyndFeed(newFeed, syndFeed);
					} catch (FeedLinkUnknownException e) {
						throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR,
								"The feed is missing a <link> field, " + e.getMessage());
					}
					
					pollingPersistence.createTask(PollingTaskFamilyType.FEED, newFeed.getSource().getId());
					
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
		private boolean modified;
		
		UpdateFeedContext(long feedId, SyndFeed syndFeed, boolean modified) {
			this.feedId = feedId;
			this.syndFeed = syndFeed;
			this.modified = modified;
		}

		public long getFeedId() {
			return feedId;
		}

		public SyndFeed getSyndFeed() {
			return syndFeed;
		}

		public boolean isModified() {
			return modified;
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean updateFeedNeedsFetch(Feed feed) {
		long updateTime = feed.getLastFetchSucceeded() ? FEED_UPDATE_TIME : BAD_FEED_UPDATE_TIME;
		
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
			final FeedFetchResult result = getRawFeedChecked(feed.getSource());
			if (result.isModified())
				logger.info("  HTTP request completed for feed {}, retrieved {} entries", feed.getSource(), result.getFeed().getEntries().size());
			else
				logger.info("  Feed {} retrieved from local cache", feed.getSource());				
			return new UpdateFeedContext(feed.getId(), result.getFeed(), result.isModified());		
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
				if (syndFeed.getLink() == null)
					throw new MalformedURLException("missing link for synd feed");
				entryUrl = entryURLFromSyndEntry(new URL(syndFeed.getLink()), syndEntry);
				identitySpider.getLink(entryUrl);
			} catch (MalformedURLException e) {
				// just ignore, it will break later on
			}
		}
	}
	
	public boolean storeRawUpdatedFeed(long feedId, SyndFeed syndFeed) throws FeedLinkUnknownException {
		Feed feed = em.find(Feed.class, feedId);
		logger.debug("  Saving feed update results in db for {}", feed.getSource());
		return updateFeedFromSyndFeed(feed, syndFeed);
	}	
	
	public void updateFeedStoreFeed(Object contextObject) throws XmlMethodException {
		UpdateFeedContext context = (UpdateFeedContext) contextObject;
		try {
			storeRawUpdatedFeed(context.getFeedId(), context.getSyndFeed());
		} catch (FeedLinkUnknownException e) {
			throw new XmlMethodException(XmlMethodErrorCode.PARSE_ERROR, "Couldn't find a link for this feed or it was invalid");			
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
		Query q = em.createQuery("SELECT fe FROM FeedEntry fe " +
				                 "WHERE fe.feed = :feed AND fe.current = 1" +
				                 "ORDER BY fe.date DESC");
		q.setParameter("feed", feed);
		
		return TypeUtils.castList(FeedEntry.class, q.getResultList());
	}
	
	public FeedEntry getLastEntry(Feed feed) throws NoFeedEntryException {
		// this could get smarter if we want to return the entry that was added last,
		// which might not necessarily be the same one that has the latest publishing date
		// (for example, if some blogs allow back dating entries)
		// this could also be change to return a given number of recent entries
		Query q = em.createQuery("SELECT fe FROM FeedEntry fe " +
                "WHERE fe.feed = :feed " +
                "ORDER BY fe.date DESC");
		q.setParameter("feed", feed);
		q.setMaxResults(1);
		try {
			return (FeedEntry) q.getSingleResult();
		} catch (NoResultException e) {
			throw new NoFeedEntryException(e);
		}
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

	public void addGroupFeed(User adder, Group group, Feed feed) {
		for (GroupFeed old : group.getFeeds()) {
			if (old.getFeed().equals(feed)) {
				if (old.isRemoved()) {
					old.setRemoved(false);
					revisionControl.persistRevision(new GroupFeedAddedRevision(adder, group, new Date(), feed));
				}
				return;
			}
		}
		EJBUtil.forceInitialization(feed.getGroups());
		GroupFeed groupFeed = new GroupFeed(group, feed);
		em.persist(groupFeed);
		group.getFeeds().add(groupFeed);
		feed.getGroups().add(groupFeed);
		
		revisionControl.persistRevision(new GroupFeedAddedRevision(adder, group, new Date(), feed));
	}
	
	public void removeGroupFeed(User remover, Group group, Feed feed) {
		for (GroupFeed old : group.getFeeds()) {
			if (old.getFeed().equals(feed)) {
				if (!old.isRemoved()) {
					old.setRemoved(true);
					revisionControl.persistRevision(new GroupFeedRemovedRevision(remover, group, new Date(), feed));
				}
				return;
			}
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
	
	private static class FeedTaskFamily implements PollingTaskFamily {

		public long getDefaultPeriodicity() {
			return 5 * 60 * 1000; // 5 minutes
		}

		public long getMaxOutstanding() {
			return 100;
		}

		public long getMaxPerSecond() {
			return -1;
		}

		public String getName() {
			return PollingTaskFamilyType.FEED.name();
		}
	}
	
	private static PollingTaskFamily family = new FeedTaskFamily();
	
	private static class FeedTask extends PollingTask {
		private Feed feed;  // detached
		private LinkResource source; // detatched
		private Guid linkId;
		
		public FeedTask(Feed feed, Guid id) {
			this.feed = feed;
			this.source = feed.getSource();
			this.linkId = id;
		}

		@Override
		protected PollResult execute() throws Exception {
			boolean changed = false;
			final FeedSystem feedSystem = EJBUtil.defaultLookup(FeedSystem.class);
			final FeedFetchResult result;			
			try {
				result = FeedSystemBean.getRawFeed(source);
			} catch (FeedFetcher.FetchFailedException e) {
				throw new DynamicPollingSystem.PollingTaskNormalExecutionException("Feed " + feed.getSource() + " fetch failed: " + e.getMessage(), e);
			}
			final TransactionRunner runner = EJBUtil.defaultLookup(TransactionRunner.class);
			changed = runner.runTaskRetryingOnDuplicateEntry(new Callable<Boolean>() {
				public Boolean call() throws DynamicPollingSystem.PollingTaskNormalExecutionException {
					try {
						return feedSystem.storeRawUpdatedFeed(feed.getId(), result.getFeed());
					} catch (FeedLinkUnknownException e) {
						throw new DynamicPollingSystem.PollingTaskNormalExecutionException("Feed " + feed.getSource() + " link unknown: " + e.getMessage(), e);						
					}
				}
			});
			if (changed && !result.isModified()) {
				logger.warn("Feed fetcher returned not modified but feed system found new entries for feed: {}", feed.getSource());
			}
			
			return new PollResult(changed, false);
		}

		@Override
		public PollingTaskFamily getFamily() {
			return family;
		}

		@Override
		public String getIdentifier() {
			return linkId.toString();
		}
	}

	public PollingTask loadTask(PollingTaskEntry entry) throws NotFoundException {
		String linkId = entry.getTaskId();
		LinkResource link = em.find(LinkResource.class, linkId);
		try {
			Feed feed = (Feed) em.createQuery("select feed from Feed feed where feed.source = :source").setParameter("source", link).getSingleResult();
			return new FeedTask(feed, link.getGuid());
		} catch (NoResultException e) {
			throw new NotFoundException("Feed from link " + linkId + " was deleted");
		}
	}

	public void migrateTasks() {
		for (Feed feed : getInUseFeeds()) {
			pollingPersistence.createTaskIdempotent(PollingTaskFamilyType.FEED, feed.getSource().getId());
		}
	}
}
