package com.dumbhippo.server.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.persistence.BlockMessage;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.GroupMessage;
import com.dumbhippo.persistence.PostMessage;
import com.dumbhippo.persistence.TrackMessage;

public class ChatMessageKey implements DMKey {
	private static final long serialVersionUID = 4974136343974507951L;
	
	private long id;
	private ChatMessageType type;
	
	public ChatMessageKey(String keyString) throws BadIdException {
		int dot = keyString.indexOf(".");
		if (dot < 0)
			throw new BadIdException("Invalid format for chat message key");

		try {
			id = Long.parseLong(keyString.substring(0, dot));
		} catch (NumberFormatException e) {
			throw new BadIdException("Invalid message id in chat message key");
		}
		
		try {
			type = ChatMessageType.valueOf(keyString.substring(dot + 1));
		} catch (IllegalArgumentException e) {
			throw new BadIdException("Bad chat message type in ID", e);
		}
	}
	
	public ChatMessageKey(long id, ChatMessageType type) {
		this.id = id;
		this.type = type;
	}
	
	public ChatMessageKey(ChatMessage message) {
		this(message.getId(), getTypeForMessage(message));
	}
	
	private static ChatMessageType getTypeForMessage(ChatMessage message) {
		if (message instanceof BlockMessage)
			return ChatMessageType.BLOCK;
		else if (message instanceof GroupMessage)
			return ChatMessageType.GROUP;
		else if (message instanceof PostMessage)
			return ChatMessageType.POST;
		else if (message instanceof TrackMessage)
			return ChatMessageType.TRACK;
		else
			throw new RuntimeException("Unexpected message type: " + message.getClass().getName());
	}
	
	public long getId() {
		return id;
	}
	
	public ChatMessageType getType() {
		return type;
	}
		
	@Override
	public int hashCode() {
		return (int)(id * 23 + type.ordinal() * 29);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChatMessageKey))
			return false;
		
		ChatMessageKey other = (ChatMessageKey)o;
		return id == other.id && type == other.type;
	}
	
	@Override
	public ChatMessageKey clone() {
		return this;
	}
	
	@Override
	public String toString() {
		return id + "." + type.name();
	}
}
