/**
 * 
 */
package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Date;

/**
 * Object representing a chat room message 
 * for transfer to the web tier or to Jive
 * 
 * @author Havoc Pennington
 *
 */
public class ChatRoomMessage implements Serializable {
	private static final long serialVersionUID = 0L;
	
	private String fromUsername;
	private int serial;
	private Date timestamp;
	private String text;
	
	public ChatRoomMessage(String fromUsername, String text, Date timestamp, int serial) {
		this.fromUsername = fromUsername;
		this.timestamp = timestamp;
		this.text = text;
		this.serial = serial;
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