package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.LinkResource;

@Local
public interface FeedSystem {
	Feed getFeed(LinkResource link) throws XmlMethodException;
	
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
	 * Mark a feed as failed after an unsuccessful update.
	 * @param feed
	 */
	void markFeedFailedLastUpdate(Feed feed);
	
	List<FeedEntry> getCurrentEntries(Feed feed);
	
	List<Feed> getInUseFeeds();
	
	void addGroupFeed(Group group, Feed feed);
	void removeGroupFeed(Group group, Feed feed);
	
	/*
	 * Called (by FeedSystem itself) when a new FeedEntry is found
	 * 
	 *  @param entryId the ID of the entry that changed (a ID rather than
	 *     a FeedEntry is used here because this is called asynchronously) 
	 */
	void handleNewEntryNotification(long entryId);	
}
