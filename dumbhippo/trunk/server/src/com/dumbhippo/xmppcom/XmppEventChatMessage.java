package com.dumbhippo.xmppcom;

import java.util.Date;

import com.dumbhippo.server.ChatRoomKind;

public class XmppEventChatMessage extends XmppEvent {
	private static final long serialVersionUID = 1L;
	
	private String roomName;
	private String fromUsername;
	private String text;
	private Date timestamp;
	private int serial;
	private ChatRoomKind kind;
	
	public XmppEventChatMessage(String roomName, ChatRoomKind kind, String fromUsername, String text, Date timestamp, int serial) {
		this.roomName = roomName;
		this.kind = kind;
		this.fromUsername = fromUsername;
		this.text = text;
		this.timestamp = timestamp;
		this.serial = serial;
	}
	
	public String getRoomName() {
		return roomName;
	}
	
	public ChatRoomKind getKind() {
		return kind;
	}
	
	public String getFromUsername() {
		return fromUsername;
	}
	
	public int getSerial() {
		return serial;
	}

	public String getText() {
		return text;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
}
