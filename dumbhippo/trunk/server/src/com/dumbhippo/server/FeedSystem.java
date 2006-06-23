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
	void updateFeed(Feed feed) throws XmlMethodException;
	List<FeedEntry> getCurrentEntries(Feed feed);
	
	void addGroupFeed(Group group, Feed feed);
	void removeGroupFeed(Group group, Feed feed);
}
