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
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class PostMessage extends EmbeddedMessage {

	private static final long serialVersionUID = 1L;
	
	private Post post;
	
	public PostMessage() {
		this(null, null, null, null);
	}

	public PostMessage(Post post, User fromUser, String messageText, Date timestamp) {
		super(fromUser, messageText, timestamp);
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
