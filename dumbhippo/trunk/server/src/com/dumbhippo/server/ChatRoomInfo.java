/**
 * 
 */
package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ChatMessage;

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
	private Guid roomGuid;
	private String title;
	private List<? extends ChatMessage> history;
	
	public ChatRoomInfo(ChatRoomKind kind, Guid roomGuid, String postTitle, List<? extends ChatMessage> history, boolean world) {
		this.kind = kind;
		this.roomGuid = roomGuid;
		this.title = postTitle;
		this.history = history;
		this.worldAccessible = world;
	}
	
	public ChatRoomKind getKind() {
		return kind;
	}
	
	public Guid getChatId() {
		return roomGuid;
	}
	
	public String getTitle() {
		return title;
	}

	public List<? extends ChatMessage> getHistory() {
		return Collections.unmodifiableList(history);
	}

	public boolean isWorldAccessible() {
		return this.worldAccessible;
	}
}
