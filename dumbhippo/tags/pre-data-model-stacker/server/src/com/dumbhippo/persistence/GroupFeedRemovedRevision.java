package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class GroupFeedRemovedRevision extends GroupRevision {

	private Feed feed;
	
	public GroupFeedRemovedRevision(User revisor, Group target, Date time, Feed feed) {
		super(RevisionType.GROUP_FEED_REMOVED, revisor, target, time);
		this.feed = feed;
	}

	protected GroupFeedRemovedRevision() {
		
	}
	
	@JoinColumn(nullable=false)
	@ManyToOne
	public Feed getFeed() {
		return feed;
	}

	protected void setFeed(Feed feed) {
		this.feed = feed;
	}
}
