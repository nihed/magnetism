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
	private String name;
	private String smallPhotoUrl;	
	
	public ChatRoomUser(String username, String name, String smallPhotoUrl) {
		this.username = username;
		this.name = name;
		this.smallPhotoUrl = smallPhotoUrl;		
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getName() {
		return name;
	}
	
	public String getSmallPhotoUrl() {
		return smallPhotoUrl;
	}

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof ChatRoomUser))
			return false;
		return ((ChatRoomUser) arg0).username.equals(username);
	}

	@Override
	public int hashCode() {
		return username.hashCode();
	}
	
}