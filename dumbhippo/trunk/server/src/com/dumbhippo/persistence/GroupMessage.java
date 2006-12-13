package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Stores an individual message associated with a group chat.
 * 
 * @author hp
 */

@Entity
@Cache(usage=CacheConcurrencyStrategy.TRANSACTIONAL)
public class GroupMessage extends EmbeddedMessage {

	private static final long serialVersionUID = 1L;
	
	private Group group;
	
	public GroupMessage() {
		this(null, null, null, null);
	}

	public GroupMessage(Group group, User fromUser, String messageText, Date timestamp, Sentiment sentiment) {
		super(fromUser, messageText, timestamp, sentiment);
		this.group = group;
	}
	
	public GroupMessage(Group group, User fromUser, String messageText, Date timestamp) {
		this(group, fromUser, messageText, timestamp, Sentiment.INDIFFERENT);
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}
}
