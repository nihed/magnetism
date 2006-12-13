package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name="GroupFeed", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"group_id", "feed_id"})}
		   )
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)	   
public class GroupFeed extends EmbeddedGuidPersistable {
	private static final long serialVersionUID = 0L;
	
	private Feed feed;
	private Group group;
	private boolean removed;

	public GroupFeed() {
		
	}
	
	public GroupFeed(Group group, Feed feed) {
		this.group = group;
		this.feed = feed;
	}

	@Override
	public String toString() {
		return "{GroupFeed " + "guid = " + getId() + " group = " + getGroup() + " feed = " + getFeed() + "}";
	}

	@ManyToOne
	@JoinColumn(nullable=false)	
	public Feed getFeed() {
		return feed;
	}

	public void setFeed(Feed feed) {
		this.feed = feed;
	}

	@ManyToOne
	@JoinColumn(nullable=false)
	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	@Column(nullable=false)
	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
}
