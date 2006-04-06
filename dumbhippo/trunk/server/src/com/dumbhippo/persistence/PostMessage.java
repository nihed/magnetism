package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Stores an individual message associated with a post.
 * 
 * @author dff
 * @author otaylor
 */

@Entity
public class PostMessage extends DBUnique {

	private static final long serialVersionUID = 1L;
	
	private Post post;
	private User fromUser;
	private int messageSerial;
	private String messageText;
	private long timestamp;
	
	public PostMessage() {
		// we use serial = -1 in other places in the system to designate a message that contains
		// the post description, but we never add this type of message to the database
		this(null, null, null, null, -1);
	}

	public PostMessage(Post post, User fromUser, String messageText, Date timestamp, int messageSerial) {
		super();
		this.post = post;
		this.fromUser = fromUser;
		this.messageText = messageText;
		this.setTimestamp(timestamp);
		this.messageSerial = messageSerial;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Post getPost() {
		return post;
	}

	public void setPost(Post post) {
		this.post = post;
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
	public int getMessageSerial() {
		return messageSerial;
	}

	public void setMessageSerial(int messageSerial) {
		this.messageSerial = messageSerial;
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

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp != null ? timestamp.getTime() : -1;
	}
	
}