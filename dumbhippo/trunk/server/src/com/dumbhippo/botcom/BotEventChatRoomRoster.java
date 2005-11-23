package com.dumbhippo.botcom;

import java.util.ArrayList;

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
	private String chatRoomId;
	private ArrayList<String> chatRoomRoster;	
	
	public BotEventChatRoomRoster(String botName, String chatRoomName, String chatRoomId, ArrayList<String> chatRoomRoster) {
		super(botName);
		this.chatRoomName = chatRoomName;
		this.chatRoomId = chatRoomId;
		this.chatRoomRoster = chatRoomRoster;
	}
	
	@Override
	public String toString() {
		return "BotEventChatRoomRoster from bot " + getBotName() + " from chat room " + getChatRoomId() + "/" + getChatRoomName() + " roster is " + getChatRoomRoster();
	}

	public String getChatRoomId() {
		return chatRoomId;
	}

	public String getChatRoomName() {
		return chatRoomName;
	}

	public ArrayList getChatRoomRoster() {
		return chatRoomRoster;
	}
}
