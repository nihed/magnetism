package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * A chat message, we have subclasses that put the chat in a context
 * e.g. about a Post
 * 
 * @author dff
 * @author otaylor
 */

@MappedSuperclass
public abstract class EmbeddedMessage extends DBUnique implements ChatMessage {

	private static final long serialVersionUID = 1L;
	
	private User fromUser;
	private String messageText;
	private Sentiment sentiment;
	private long timestamp;
	
	protected EmbeddedMessage() {
		this(null, null, null, Sentiment.INDIFFERENT);
	}

	protected EmbeddedMessage(User fromUser, String messageText, Date timestamp, Sentiment sentiment) {
		super();
		this.fromUser = fromUser;
		this.messageText = messageText;
		this.setTimestamp(timestamp);
		this.sentiment = sentiment;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public User getFromUser() {
		return fromUser;
	}

	public void setFromUser(User fromUser) {
		this.fromUser = fromUser;
	}

	@Column(nullable=false)
	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	@Column(nullable=false)
	public Date getTimestamp() {
		return timestamp >= 0 ? new Date(timestamp) : null;
	}
	
	@Transient
	public long getTimestampAsLong() {
		return timestamp;
	}

	// The database only stores timestamps at second-resolution. Things become more reliable
	// if we round here, rather than rounding when storing into the database. (If we switch
	// to a database with high-resolution timestamps, this should be removed.)
	private long roundTimestamp(long timestamp) {
		// any thing < 0 is just a flag value
		if (timestamp < 0)
			return -1000;
		else
			return (timestamp / 1000) * 1000;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp != null ? roundTimestamp(timestamp.getTime()) : -1000;
	}	
	
	@Column(nullable = false)
	public Sentiment getSentiment() {
		return sentiment;
	}
	
	public void setSentiment(Sentiment sentiment) {
		this.sentiment = sentiment;
	}
}
