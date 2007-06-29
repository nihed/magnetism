package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class UserExternalAccountChangedRevision extends UserRevision {
	
	// what site is the account on
	private ExternalAccountType accountType;
	// how do we feel about this site
	private Sentiment sentiment;
	// meaning of this varies by the where
	private String handle;
	// meaning of this also varies by the where
	// not always used...
	private String extra;
	// quip (right now only applies if sentiment == HATE)
	private String quip;
	// if the account has an associated feed, that goes here
	private Feed feed;
	
	public UserExternalAccountChangedRevision(User revisor, Date time) {
		super(RevisionType.USER_EXTERNAL_ACCOUNT_CHANGED, revisor, time);
	}

	protected UserExternalAccountChangedRevision() {
		
	}
	
	@Column(nullable=false)
	public ExternalAccountType getAccountType() {
		return accountType;
	}
	public void setAccountType(ExternalAccountType type) {
		this.accountType = type;
	}
	
	@Column(nullable=true)
	public String getHandle() {
		return handle;
	}
	public void setHandle(String handle) {
		this.handle = handle;
	}
	
	@Column(nullable=true)
	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}
	
	@Column(nullable=true)
	public String getQuip() {
		return quip;
	}
	public void setQuip(String quip) {
		this.quip = quip;
	}
	
	@Column(nullable=false)
	public Sentiment getSentiment() {
		return sentiment;
	}

	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}

	@ManyToOne
	@JoinColumn(nullable=true)	
	public Feed getFeed() {
		return feed;
	}

	public void setFeed(Feed feed) {
		this.feed = feed;
	}
}
