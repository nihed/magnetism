/**
 * 
 */
package com.dumbhippo.server;

import java.io.Serializable;

/**
 * Object representing a chat room user 
 * for transfer to the web tier or to Jive
 * 
 * @author Havoc Pennington
 */
public class ChatRoomUser implements Serializable {
	private static final long serialVersionUID = 0L;
	
	private String username;
	private int version;
	private String name;
	
	public ChatRoomUser(String username, int version, String name) {
		this.username = username;
		this.version = version;
		this.name = name;
	}
	
	public String getUsername() {
		return username;
	}
	
	public int getVersion() {
		return version;
	}
	
	public String getName() {
		return name;
	}
}