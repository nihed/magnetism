/**
 * 
 */
package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Object representing a chat room for transfer to the web tier or to Jive.
 * 
 * @author Havoc Pennington
 *
 */
public class ChatRoomInfo implements Serializable {
	private static final long serialVersionUID = 0L;
	
	private ChatRoomKind kind;
	private boolean worldAccessible;
	private String roomName;
	private String title;
	private List<ChatRoomMessage> history;
	
	public ChatRoomInfo(ChatRoomKind kind, String roomName, String postTitle, List<ChatRoomMessage> history, boolean world) {
		this.kind = kind;
		this.roomName = roomName;
		this.title = postTitle;
		this.history = history;
		this.worldAccessible = world;
	}
	
	public ChatRoomKind getKind() {
		return kind;
	}
	
	public String getChatId() {
		return roomName;
	}
	
	public String getTitle() {
		return title;
	}

	public List<ChatRoomMessage> getHistory() {
		return Collections.unmodifiableList(history);
	}

	public boolean isWorldAccessible() {
		return this.worldAccessible;
	}
}
