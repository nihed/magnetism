package com.dumbhippo.xmppcom;

import java.util.Date;

public class XmppEventChatMessage extends XmppEvent {
	private static final long serialVersionUID = 1L;
	
	private String roomName;
	private String fromUsername;
	private String text;
	private Date timestamp;
	
	public XmppEventChatMessage(String roomName, String fromUsername, String text, Date timestamp) {
		this.roomName = roomName;
		this.fromUsername = fromUsername;
		this.text = text;
		this.timestamp = timestamp;
	}
	
	public String getRoomName() {
		return roomName;
	}
	
	public String getFromUsername() {
		return fromUsername;
	}
	
	public String getText() {
		return text;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
}
