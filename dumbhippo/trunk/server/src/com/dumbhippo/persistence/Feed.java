package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class Feed extends DBUnique {
	private static final long serialVersionUID = 1L;

	private LinkResource link;
	private String title;
	private long lastFetched;
	private boolean lastFetchSucceeded;
	private Set<FeedEntry> entries;
	
	protected Feed() {
		this.entries = new HashSet<FeedEntry>();
	}
	
	public Feed(LinkResource link) {
		this();
		this.link = link;
	}

	@OneToOne
	public LinkResource getLink() {
		return link;
	}
	
	protected void setLink(LinkResource link) {
		this.link = link;
	}
	
	@Column(nullable = false)
	public Date getLastFetched() {
		return new Date(lastFetched);
	}
	
	public void setLastFetched(Date lastFetched) {
		this.lastFetched = lastFetched.getTime();
	}
	
	public boolean getLastFetchSucceeded() {
		return lastFetchSucceeded;
	}
	
	public void setLastFetchSucceeded(boolean lastFetchSucceeded) {
		this.lastFetchSucceeded = lastFetchSucceeded;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	@OneToMany(mappedBy="feed")
	public Set<FeedEntry> getEntries() {
		return entries;
	}
	
	public void setEntries(Set<FeedEntry> entries) {
		this.entries = entries;
	}
}
