package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Stores an individual message associated with a track play
 */

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class TrackMessage extends EmbeddedMessage {

	private static final long serialVersionUID = 1L;
	
	private TrackHistory trackHistory;
	
	public TrackMessage() {
		this(null, null, null, null, Sentiment.INDIFFERENT);
	}

	public TrackMessage(TrackHistory trackHistory, User fromUser, String messageText, Date timestamp, Sentiment sentiment) {
		super(fromUser, messageText, timestamp, sentiment);
		this.trackHistory = trackHistory;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public TrackHistory getTrackHistory() {
		return trackHistory;
	}

	public void setTrackHistory(TrackHistory trackHistory) {
		this.trackHistory = trackHistory;
	}
}
