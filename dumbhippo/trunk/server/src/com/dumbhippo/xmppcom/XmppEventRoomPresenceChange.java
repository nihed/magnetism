package com.dumbhippo.xmppcom;


public class XmppEventRoomPresenceChange extends XmppEvent {
	private static final long serialVersionUID = 1L;
	
	private String roomName;
	private String username;
	private boolean isPresent;
	
	public XmppEventRoomPresenceChange(String roomName, String username, boolean isPresent) {
		this.roomName = roomName;
		this.username = username;
		this.isPresent = isPresent;
	}

	public boolean isPresent() {
		return isPresent;
	}

	public String getRoomName() {
		return roomName;
	}

	public String getUsername() {
		return username;
	}
}
