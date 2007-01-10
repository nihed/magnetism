package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.PollingTaskPersistence.PollingTaskLoader;
import com.dumbhippo.server.impl.FeedSystemBean.FeedLinkUnknownException;
import com.sun.syndication.feed.synd.SyndFeed;

@Local
public interface FeedSystem extends PollingTaskLoader {
	
	public Feed getExistingFeed(final LinkResource source) throws XmlMethodException;
	
	public Feed getOrCreateFeed(LinkResource link) throws XmlMethodException;
	
	/**
	 * Does both halves of updating a feed in a single transaction, which can be 
	 * sketchy. Probably is always sketchy in fact. FIXME Make HttpMethodsBean not do it.
	 * @param feed the feed to update
	 */
	void updateFeed(Feed feed);
	
	/** 
	 * Does "step zero" of updating a feed - see if we need to update it.
	 * 
	 * @param feed the feed (can be detached)
	 * @return true if updateFeedFetchFeed is needed.
	 */
	boolean updateFeedNeedsFetch(Feed feed);
	
	/**
	 * Does "step one" of updating a feed, goes out on the net to get the 
	 * feed. Best done in a separate transaction from storing the feed.
	 *  Returns null if nothing new to do.
	 * @param feed the feed (can be detached).
	 * @return an opaque context object, or null if nothing to be stored.
	 */
	Object updateFeedFetchFeed(Feed feed) throws XmlMethodException;
	/**
	 * Does "step two" of updating a feed, stores the results 
	 * of fetching the feed.
	 * @param context the opaque context return from updateFeedFetchFeed
	 * @throws XmlMethodException 
	 */
	void updateFeedStoreFeed(Object context) throws XmlMethodException;
	
	/**
	 * Stores a raw feed.  This method should generally not be called
	 * by methods outside of FeedSystem itself.
	 * 
	 * @param result the result of a feed fetch
	 * @throws FeedLinkUnknownException 
	 */
	void storeRawUpdatedFeed(long feedId, SyndFeed feed) throws FeedLinkUnknownException;
	
	/**
	 * Mark a feed as failed after an unsuccessful update.
	 * @param feed
	 */
	void markFeedFailedLastUpdate(Feed feed);
	
	/**
	 * Returns feed entries with sorted by publishing date, with the latest entry first.
	 * 
	 * @param feed
	 * @return feed entries
	 */
	List<FeedEntry> getCurrentEntries(Feed feed);
	
	public class NoFeedEntryException extends Exception {
		public NoFeedEntryException(Exception e) {
			super(e);
		}

		private static final long serialVersionUID = 1L;	
	}
	
	/**
	 * Returns the latest entry for a feed.
	 * 
	 * @param feed
	 * @return the latest entry for a feed
	 */
	FeedEntry getLastEntry(Feed feed) throws NoFeedEntryException;
	
	List<Feed> getInUseFeeds();
	
	void addGroupFeed(User adder, Group group, Feed feed);
	void removeGroupFeed(User remover, Group group, Feed feed);
	
	/*
	 * Called (by FeedSystem itself) when a new FeedEntry is found
	 * 
	 *  @param entryId the ID of the entry that changed (a ID rather than
	 *     a FeedEntry is used here because this is called asynchronously) 
	 */
	void handleNewEntryNotification(long entryId);
	
	String getUrlToScrapeFaviconFrom(String untrustedFeedSourceUrl) throws NotFoundException;
	
	String getUrlToScrapeFaviconFrom(Feed feed) throws NotFoundException;
}
