package com.dumbhippo.botcom;

import java.util.List;

/**
 * BotEventChatRoomRoster encapsulates a change in the roster, or current members
 * of, an AIM chat room.
 * 
 * @author dff
 *
 */
public class BotEventChatRoomRoster extends BotEvent {
	private static final long serialVersionUID = 1L;

	private String chatRoomName;
	private List<String> chatRoomRoster;	
	
	public BotEventChatRoomRoster(String botName, String chatRoomName, List<String> chatRoomRoster) {
		super(botName);
		this.chatRoomName = chatRoomName;
		this.chatRoomRoster = chatRoomRoster;
	}
	
	@Override
	public String toString() {
		return this.getClass().getName() + " from bot " + getBotName() + " from chat room " + getChatRoomName() + " roster is " + getChatRoomRoster();
	}

	public String getChatRoomName() {
		return chatRoomName;
	}

	public List<String> getChatRoomRoster() {
		return chatRoomRoster;
	}
}
