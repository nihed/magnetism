package com.dumbhippo.xmppcom;

import java.util.Date;

public class XmppEventChatMessage extends XmppEvent {
	private static final long serialVersionUID = 1L;
	
	private String roomName;
	private String fromUsername;
	private String text;
	private Date timestamp;
	private int serial;
	
	public XmppEventChatMessage(String roomName, String fromUsername, String text, Date timestamp, int serial) {
		this.roomName = roomName;
		this.fromUsername = fromUsername;
		this.text = text;
		this.timestamp = timestamp;
		this.serial = serial;
	}
	
	public String getRoomName() {
		return roomName;
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
