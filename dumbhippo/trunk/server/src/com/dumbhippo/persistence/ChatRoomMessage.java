package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Stores an individual message in a chat room associated with a post.
 * 
 * @author dff
 */

@Entity
public class ChatRoomMessage extends DBUnique {

	private static final long serialVersionUID = 1L;
	
	private ChatRoom chatRoom;
	private String fromScreenName;
	private String messageText;
	private long timestamp;
	
	public ChatRoomMessage() {
		this(null,null,null,null);
	}

	public ChatRoomMessage(ChatRoom chatRoom, String fromScreenName, String messageText, Date timestamp) {
		super();
		this.chatRoom = chatRoom;
		this.fromScreenName = fromScreenName;
		this.messageText = messageText;
		this.setTimestamp(timestamp);
	}
	
	@ManyToOne
	@JoinColumn(nullable=false)
	public ChatRoom getChatRoom() {
		return chatRoom;
	}

	public void setChatRoom(ChatRoom chatRoom) {
		this.chatRoom = chatRoom;
	}

	@Column(nullable=false)
	public String getFromScreenName() {
		return fromScreenName;
	}

	public void setFromScreenName(String fromScreenName) {
		this.fromScreenName = fromScreenName;
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