package com.dumbhippo.botcom;

/**
 * Task requesting a bot to join the specified AIM chat room to keep
 * track of participants and activity there.
 * 
 * @author dff
 *
 */

public class BotTaskJoinRoom extends BotTask {
	
	private static final long serialVersionUID = 1L;

	private String chatRoomName;
	
	/**
	 * @param botName name of bot to use, null if not important
	 * @param chatRoomName name of the chat room to join
	 */
	public BotTaskJoinRoom(String botName, String chatRoomName) {
		super(botName);
		this.chatRoomName = chatRoomName;
	}
	
	public String getChatRoomName() {
		return chatRoomName;
	}
	
}
