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
	private String smallPhotoUrl;	
	
	public ChatRoomUser(String username, int version, String name, String smallPhotoUrl) {
		this.username = username;
		this.version = version;
		this.name = name;
		this.smallPhotoUrl = smallPhotoUrl;		
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
	
	public String getSmallPhotoUrl() {
		return smallPhotoUrl;
	}
}