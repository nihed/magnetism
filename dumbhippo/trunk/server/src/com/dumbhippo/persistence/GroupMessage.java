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
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class GroupMessage extends EmbeddedMessage {

	private static final long serialVersionUID = 1L;
	
	private Group group;
	
	public GroupMessage() {
		// serial of -1 is just plain invalid for GroupMessages (we should not store 
		// this in the db)
		this(null, null, null, null, -1);
	}

	public GroupMessage(Group group, User fromUser, String messageText, Date timestamp, int messageSerial) {
		super(fromUser, messageText, timestamp, messageSerial);
		this.group = group;
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
