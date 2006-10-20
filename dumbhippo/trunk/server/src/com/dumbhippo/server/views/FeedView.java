package com.dumbhippo.server.views;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.GroupFeed;
import com.dumbhippo.persistence.VersionedEntity;

/**
 * Represents a user's view of an RSS Feed. This object wraps a GroupFeed
 * rather than a Feed because a user only sees a feed as it has been added
 * to a group (the headshot for the feed is the headshot for the group,
 * for example.)
 *   
 * @author otaylor
 */
public class FeedView extends EntityView {
	private GroupFeed feed;
	
	public FeedView(GroupFeed feed) {
		this.feed = feed;
	}

	@Override
	protected VersionedEntity getVersionedEntity() {
		// I don't think this is actually used anywhere
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return feed.getFeed().getTitle();
	}
	
	@Override
	public String getHomeUrl() {
		return feed.getFeed().getLink().getUrl();
	}
	
	@Override
	public String getPhotoUrl(int size) {
		return feed.getGroup().getPhotoUrl(size);
	}

	@Override
	public Guid getIdentifyingGuid() {
		return feed.getGuid();
	}

	@Override
	public void writeToXmlBuilderOld(XmlBuilder builder) {
		builder.appendTextNode("feed", "", 
				               "id", feed.getId(), 
							   "name", feed.getFeed().getTitle(),
							   "homeUrl", feed.getFeed().getLink().getUrl(),
							   "smallPhotoUrl", getPhotoUrl());		
	}

	public void writeToXmlBuilder(XmlBuilder builder) {
		builder.appendTextNode("feed", "", 
				               "id", feed.getId(), 
							   "name", feed.getFeed().getTitle(),
							   "homeUrl", feed.getFeed().getLink().getUrl(),
							   "photoUrl", getPhotoUrl());		
	}
	
	@Override
	public String toIdentifyingXml() {
		XmlBuilder builder = new XmlBuilder();
		builder.appendEmptyNode("feed", 
				                "id", feed.getId()); 
		return builder.toString();		
	}

}
