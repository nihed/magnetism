package com.dumbhippo.server;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.ChatMessage;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.server.views.ChatMessageView;
import com.dumbhippo.server.views.Viewpoint;

@Local
public interface ChatSystem {
	/**
	 * Get all messages that were blocked in the chatroom about this block.
	 * 
	 * @param block the block the look up the messages for
	 * @param lastSeenSerial return only messages with serials greater than this
	 * @return the list of mesages, sorted by date (newest last)
	 */
	public List<? extends ChatMessage> getMessages(Block block, long lastSeenSerial);
	
	public List<? extends ChatMessage> getNewestMessages(Block block, int maxResults);

	/**
	 * Get the total count of messages that were sent to the chatroom about this
	 * block.
	 * 
	 * @param block the block to count the messages for
	 * @return the total number of messages about this block
	 */
	public int getMessageCount(Block block);
	
	public List<ChatMessageView> viewMessages(List<? extends ChatMessage> messages, Viewpoint viewpoint);

	/**
	 * Returns the information associated with the potential user of a chat room.
	 * 
	 * @param roomGuid the GUID for post (etc.) the chat is about
	 *        This must exist or an exception will be thrown
	 * @param kind the kind of the chat room
	 * @param userId the GUID of the user for whom we look up information
	 * @return blob of information about the user
	 */
	public ChatRoomUser getChatRoomUser(Guid roomGuid, ChatRoomKind kind, String userId);
	
	/**
	 * Get the information needed to manage a chatroom for a post.
	 * 
	 * @param roomName The GUID for the post (etc.) that the chat is about
	 * @return a blob of information about the chatroom.
	 */
	public ChatRoomInfo getChatRoomInfo(Guid roomGuid, boolean includeHistory) throws NotFoundException;
	
	/**
	 * Get messages that have been sent to the chatroom since the specified serial
	 * 
	 * @param roomGuid the GUID for post (etc.) the chat is about
	 *        This must exist or an exception will be thrown
	 * @param kind the kind of chatroom (group or post)
	 * @param lastSeenSerial retrieve only messages with serials greater than this (use -1
	 *        to get all messages)
	 * @return a list of chat room messages.
	 */
	List<? extends ChatMessage> getChatRoomMessages(Guid roomGuid, ChatRoomKind kind, long lastSeenSerial);
	
	public boolean canJoinChat(Guid roomGuid, ChatRoomKind kind, Guid userId);
	
	/**
	 * Adds a new message to a chatroom.
	 * 
	 * @param roomGuid the GUID for post (etc.) the chat is about
	 *        This must exist or an exception will be thrown
	 * @param kind the kind of chatroom (group or post)
	 * @param userId the GUID of the user posting the message
	 * @param text the text of the message
	 * @param timestamp the timestamp for the message
	 * @param sentiment the sentiment of the message INDIFFERENT=Chat LOVE/HATE=Quip
	 */
	public void addChatRoomMessage(Guid roomGuid, ChatRoomKind kind, Guid userId, String text, Sentiment sentiment, Date timestamp);
}
