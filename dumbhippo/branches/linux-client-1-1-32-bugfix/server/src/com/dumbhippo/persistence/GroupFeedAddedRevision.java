package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class GroupFeedAddedRevision extends GroupRevision {

	private Feed feed;

	public GroupFeedAddedRevision(User revisor, Group target, Date time, Feed feed) {
		super(RevisionType.GROUP_FEED_ADDED, revisor, target, time);
		this.feed = feed;
	}
	
	protected GroupFeedAddedRevision() {
		
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
