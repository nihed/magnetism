package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public class FeedEntry extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private Feed feed;
	private String entryGuid;
	private String title;
	private String description;
	private LinkResource link;
	private long date;
	private boolean current;
	
	protected FeedEntry() {
	}
	
	public FeedEntry(Feed feed) {
		this.feed = feed;
	}
	
	@ManyToOne
	@JoinColumn(nullable = false)
	public Feed getFeed() {
		return feed;
	}
	
	protected void setFeed(Feed feed) {
		this.feed = feed;
	}

	@Column(nullable = false)
	public Date getDate() {
		return new Date(date);
	}
	
	public void setDate(Date date) {
		this.date = date.getTime();
	}

	public String getEntryGuid() {
		return entryGuid;
	}
	
	public void setEntryGuid(String entryGuid) {
		this.entryGuid = entryGuid;
	}
	
	@ManyToOne
	public LinkResource getLink() {
		return link;
	}
	
	public void setLink(LinkResource link) {
		this.link = link;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isCurrent() {
		return current;
	}
	
	@Column(nullable = false)
	public void setCurrent(boolean visible) {
		this.current = visible;
	}
	
	@Override
	public String toString() {
		return "{FeedEntry feed = " + getFeed() + " title = " + getTitle() + " date = " + getDate() + "}";
	}
}
