package com.dumbhippo.server;

import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.persistence.Feed;
import com.dumbhippo.persistence.FeedEntry;
import com.dumbhippo.persistence.LinkResource;

@Local
public interface FeedSystem {
	Feed getFeed(LinkResource link);
	void updateFeed(Feed feed);
	List<FeedEntry> getCurrentEntries(Feed feed);
}
