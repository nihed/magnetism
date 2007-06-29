package com.dumbhippo.persistence;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)	   
public class Feed extends DBUnique {
	private static final long serialVersionUID = 1L;

	private LinkResource link;
	private LinkResource source;
	private String title;
	private long lastFetched;
	private long lastFetchedSuccessfully;
	private Set<GroupFeed> groups;
	private Set<ExternalAccount> accounts;
	
	protected Feed() {
		this.groups = new HashSet<GroupFeed>();
		this.accounts = new HashSet<ExternalAccount>();
		lastFetched = -1;
		lastFetchedSuccessfully = -1;
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

	@Column(nullable = false)
	public Date getLastFetchedSuccessfully() {
		return new Date(lastFetchedSuccessfully);
	}
	
	public void setLastFetchedSuccessfully(Date lastFetchedSuccessfully) {
		this.lastFetchedSuccessfully = lastFetchedSuccessfully.getTime();
	}
	
	@Transient
	public boolean getLastFetchSucceeded() {
		return (lastFetched == lastFetchedSuccessfully);
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	@OneToMany(mappedBy="feed")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
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
	
	@ManyToMany(mappedBy="feeds")
	@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
	public Set<ExternalAccount> getAccounts() {
		return accounts;
	}
	
	/**
	 * Only hibernate should call this probably
	 * @param groups
	 */
	protected void setAccounts(Set<ExternalAccount> accounts) {
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

	@Transient
	public String getFavicon() {
		// FIXME need build stamp on this (well, dh:png usually fixes it)
		return "/favicons/feed/" + getSource().getId();
	}
}
