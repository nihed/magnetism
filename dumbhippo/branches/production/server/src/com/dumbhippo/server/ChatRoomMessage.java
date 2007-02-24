/**
 * 
 */
package com.dumbhippo.server;

import java.io.Serializable;
import java.util.Date;

import com.dumbhippo.persistence.Sentiment;

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
	private long serial;
	private Date timestamp;
	private String text;
	private Sentiment sentiment;
	
	public ChatRoomMessage(String fromUsername, String text, Sentiment sentiment, Date timestamp, long serial) {
		this.fromUsername = fromUsername;
		this.timestamp = timestamp;
		this.text = text;
		this.sentiment = sentiment;
		this.serial = serial;
	}
	
	public String getFromUsername() {
		return fromUsername;
	}

	public long getSerial() {
		return serial;
	}

	public String getText() {
		return text;
	}


	public Sentiment getSentiment() {
		return sentiment;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
}