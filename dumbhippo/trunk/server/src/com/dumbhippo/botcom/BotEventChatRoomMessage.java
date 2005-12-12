package com.dumbhippo.botcom;

import java.util.Date;

/**
 * BotEventChatRoomMessage encapsulates a message sent in a chat room.
 * 
 * @author dff
 *
 */
public class BotEventChatRoomMessage extends BotEvent {
	private static final long serialVersionUID = 1L;

	private String chatRoomName;
	private String fromScreenName;
	private String messageText;
	private long timestamp;
	
	public BotEventChatRoomMessage(String botName, String chatRoomName, String fromScreenName, String messageText, Date timestamp) {
		super(botName);
		this.chatRoomName = chatRoomName;
		this.fromScreenName = fromScreenName;
		this.messageText = messageText;
		this.timestamp = timestamp != null ? timestamp.getTime() : -1;
		
	}
	
	@Override
	public String toString() {
		return this.getClass().getName() + " from bot " + getBotName() + " from user " + getFromScreenName() + " in chat room " + getChatRoomName() + " message is '" + getMessageText() + "'";
	}

	public String getChatRoomName() {
		return chatRoomName;
	}

	public String getFromScreenName() {
		return fromScreenName;
	}

	public String getMessageText() {
		return messageText;
	}

	public Date getTimestamp() {
		return timestamp >= 0 ? new Date(timestamp) : null;
	}
}
