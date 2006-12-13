package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Link between an Account and a Feed; used for example for Rhapsody history feeds.
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * This class is dead/obsolete; it's still here so we can use Java code to migrate 
 * the old AccountFeed to RHAPSODY ExternalAccount objects. But once that's done this
 * can be deleted. 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */

@Entity
@Table(name="AccountFeed", 
		   uniqueConstraints = 
			      {@UniqueConstraint(columnNames={"account_id", "feed_id"})}
		   )
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)	   
public class AccountFeed extends EmbeddedGuidPersistable {
	private static final long serialVersionUID = 0L;
	
	private Feed feed;
	private Account account;
	private boolean removed;

	public AccountFeed() {
		
	}
	
	public AccountFeed(Account account, Feed feed) {
		this.account = account;
		this.feed = feed;
	}

	@Override
	public String toString() {
		return "{AccountFeed " + "guid = " + getId() + " account = " + getAccount() + " feed = " + getFeed() + "}";
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
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	@Column(nullable=false)
	public boolean isRemoved() {
		return removed;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
}
