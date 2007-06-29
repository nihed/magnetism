package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

public class ChatRoomEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;

	public enum Detail {
		MESSAGES_CHANGED
	}

	private Detail detail;
	private Guid chatRoomId;
	
	public ChatRoomEvent(Guid chatRoomId, Detail detail) {
		this.chatRoomId = chatRoomId;
		this.detail = detail;
	}
	
	public Guid getChatRoomId() {
		return chatRoomId;
	}
	
	public Detail getDetail() {
		return detail;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		// TODO Auto-generated method stub
		return null;
	};
}
