package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)	   
public class Feed extends DBUnique {
	private static final long serialVersionUID = 1L;

	private LinkResource link;
	private LinkResource source;
	private String title;
	private long lastFetched;
	private boolean lastFetchSucceeded;
	private Set<FeedEntry> entries;
	private Set<GroupFeed> groups;
	private Set<AccountFeed> accounts;
	
	protected Feed() {
		this.entries = new HashSet<FeedEntry>();
		this.groups = new HashSet<GroupFeed>();
	}
	
	public Feed(LinkResource source) {
		this();
		this.source = source;
	}

	@OneToOne
	@JoinColumn(nullable = false)	
	public LinkResource getSource() {
		return source;
	}
	
	protected void setSource(LinkResource source) {
		this.source = source;
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
	
	@OneToMany(mappedBy="feed")
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	public Set<GroupFeed> getGroups() {
		return groups;
	}
	
	/**
	 * Only hibernate should call this probably
	 * @param groups
	 */
	protected void setGroups(Set<GroupFeed> groups) {
		this.groups = groups;
	}
	
	@OneToMany(mappedBy="feed")
	@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
	public Set<AccountFeed> getAccounts() {
		return accounts;
	}
	
	/**
	 * Only hibernate should call this probably
	 * @param groups
	 */
	protected void setAccounts(Set<AccountFeed> accounts) {
		this.accounts = accounts;
	}
	
	@Override
	public String toString() {
		return "{Feed url = " + getSource().getUrl() + "}";
	}

	/**
	 * This is ManyToOne since it's the human-readable web site 
	 * link, not the RSS link. There may be multiple rss/atom feeds
	 * from one site.
	 * @return the site the feed is for
	 */
	@ManyToOne
	public LinkResource getLink() {
		return link;
	}

	public void setLink(LinkResource link) {
		this.link = link;
	}
}
