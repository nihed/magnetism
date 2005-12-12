package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Stores the screen name of someone in a chat room.
 * 
 * @author dff
 */

@Entity
public class ChatRoomScreenName extends DBUnique {

	private static final long serialVersionUID = 1L;
	
	private ChatRoom chatRoom;
	private String screenName;
	
	public ChatRoomScreenName() {
		this(null,null);
	}

	public ChatRoomScreenName(ChatRoom chatRoom, String screenName) {
		super();
		this.chatRoom = chatRoom;
		this.screenName = screenName;
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
	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

}