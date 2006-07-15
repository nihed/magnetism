package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.LinkResource;

@Local
public interface FeedSystem {
	Feed getFeed(LinkResource link) throws XmlMethodException;
	void updateFeed(Feed feed);
	List<FeedEntry> getCurrentEntries(Feed feed);
	
	List<Feed> getInUseFeeds();
	
	void addGroupFeed(Group group, Feed feed);
	void removeGroupFeed(Group group, Feed feed);

	void addAccountFeed(Account account, Feed feed);
	void removeAccountFeed(Account account, Feed feed);
	
	/*
	 * Called (by FeedSystem itself) when a new FeedEntry is found
	 * 
	 *  @param entryId the ID of the entry that changed (a ID rather than
	 *     a FeedEntry is used here because this is called asynchronously) 
	 */
	void handleNewEntryNotification(long entryId);	
}
