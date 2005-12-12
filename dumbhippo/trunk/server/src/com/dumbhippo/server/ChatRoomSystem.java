package com.dumbhippo.server;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ChatRoom;
import com.dumbhippo.persistence.ValidationException;

@Local
public interface ChatRoomSystem {
	/**
	 * Finds the unique ChatRoom for a chat room name.
	 * 
	 * @param chatRoomName the chat room name to look up
	 * @return the corresponding ChatRoom object, or null if none
	 */
	public ChatRoom lookupChatRoom(String chatRoomName);
	
	/**
	 * Finds or creates a ChatRoom for the specified Post ID, and links it up to the Post.
	 * 
	 * @param postId String with the Guid of the Post chat room is needed for
	 * @return the corresponding ChatRoom object, or null if it couldn't be found/created
	 */
	public ChatRoom getChatRoom(Guid postId) throws ValidationException ;
	
	/**
	 * This is an internal detail used in implementing getChatRoom().
	 * 
	 * @param chatRoomName the chat room name to look up
	 * @return the corresponding ChatRoom object, or null if none
	 */
	public ChatRoom findOrCreateChatRoom(Guid postId) throws ValidationException;
	
	public void addChatRoomMessage(ChatRoom chatRoom, String fromScreenName, String messageText, Date timestamp);
	public void doAddChatRoomMessage(ChatRoom chatRoom, String fromScreenName, String messageText, Date timestamp);

	public void updateChatRoomRoster(ChatRoom chatRoom, List<String> chatRoomRoster);
	public void doUpdateChatRoomRoster(ChatRoom chatRoom, List<String> chatRoomRoster);
	
}