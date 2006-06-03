package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Stores an individual message associated with a post.
 * 
 * @author dff
 * @author otaylor
 */

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class PostMessage extends EmbeddedMessage {

	private static final long serialVersionUID = 1L;
	
	private Post post;
	
	public PostMessage() {
		// we use serial = -1 in other places in the system to designate a message that contains
		// the post description, but we never add this type of message to the database
		this(null, null, null, null, -1);
	}

	public PostMessage(Post post, User fromUser, String messageText, Date timestamp, int messageSerial) {
		super(fromUser, messageText, timestamp, messageSerial);
		this.post = post;
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Post getPost() {
		return post;
	}

	public void setPost(Post post) {
		this.post = post;
	}
}
