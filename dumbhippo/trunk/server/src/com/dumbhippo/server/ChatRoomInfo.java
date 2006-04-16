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
	private String roomName;
	private String title;
	private List<ChatRoomUser> allowedUsers;
	private List<ChatRoomMessage> history;
	
	public ChatRoomInfo(ChatRoomKind kind, String roomName, String postTitle, List<ChatRoomUser> allowedUsers, List<ChatRoomMessage> history) {
		this.kind = kind;
		this.roomName = roomName;
		this.title = postTitle;
		this.allowedUsers = allowedUsers; // Don't copy for efficiency, assume the caller won't
		                                  // subsequently modify
		this.history = history;
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
	
	public List<ChatRoomUser> getAllowedUsers() {
		return Collections.unmodifiableList(allowedUsers);
	}

	public List<ChatRoomMessage> getHistory() {
		return Collections.unmodifiableList(history);
	}
}
