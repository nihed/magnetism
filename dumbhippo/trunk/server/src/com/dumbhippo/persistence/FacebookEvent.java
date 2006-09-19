package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
public class FacebookEvent extends DBUnique {
	private static final long serialVersionUID = 1L;
	
	private FacebookEventType eventType;
	private FacebookAccount facebookAccount;
	private int count;  
	private long eventTimestamp;

	protected FacebookEvent() {}
	
	public FacebookEvent(FacebookAccount facebookAccount, FacebookEventType eventType, int count, long eventTimestamp) {
	    this.facebookAccount = facebookAccount;
	    this.eventType = eventType;
	    this.count = count;
	    this.eventTimestamp = eventTimestamp;
	}
	
	@ManyToOne
	@JoinColumn(nullable = false)
	public FacebookAccount getFacebookAccount() {
		return facebookAccount;
	}
	
	protected void setFacebookAccount(FacebookAccount facebookAccount) {
		this.facebookAccount = facebookAccount;
	}

	@Column(nullable=false)
	public FacebookEventType getEventType() {
		return eventType;
	}

	public void setEventType(FacebookEventType eventType) {
		this.eventType = eventType;
	}
	
	@Column(nullable=false)
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Column(nullable=false)
	public Date getEventTimestamp() {
		return new Date(eventTimestamp);
	}

	@Transient
	public long getEventTimestampAsLong() {
		return eventTimestamp;
	}
	
	public void setEventTimestamp(Date eventTimestamp) {
		this.eventTimestamp = eventTimestamp.getTime();
	}
	
	public void setEventTimestampAsLong(long eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}
}
