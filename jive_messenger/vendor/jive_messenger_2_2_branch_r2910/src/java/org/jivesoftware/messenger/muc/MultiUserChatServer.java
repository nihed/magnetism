/**
 * $RCSfile$
 * $Revision: 956 $
 * $Date: 2005-02-08 16:39:58 -0500 (Tue, 08 Feb 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.muc;

import java.util.List;
import java.util.Collection;

import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.jivesoftware.messenger.user.UserNotFoundException;
import org.xmpp.packet.Message;
import org.xmpp.packet.JID;
import org.xmpp.component.Component;

/**
 * Manages groupchat conversations, chatrooms, and users. This class is designed to operate
 * independently from the rest of the Jive server infrastruture. This theoretically allows
 * deployment of the groupchat on a separate server from the main IM server.
 * 
 * @author Gaston Dombiak
 */
public interface MultiUserChatServer extends Component {

    /**
     * Returns the fully-qualifed domain name of this chat service.
     * The domain is composed by the service name and the
     * name of the XMPP server where the service is running.
     * 
     * @return the chat server domain (service name + host name).
     */
    String getServiceDomain();

    /**
     * Returns the subdomain of the chat service.
     *
     * @return the subdomain of the chat service.
     */
    String getServiceName();

    /**
     * Set the name of this chat service. The new name won't go into effect until the server is
     * restarted.
     * 
     * @param name The chat service name (host name).
     */
    void setServiceName(String name);

    /**
     * Returns the collection of JIDs that are system administrators of the MUC service. A sysadmin has
     * the same permissions as a room owner. 
     * 
     * @return a list of bare JIDs.
     */
    Collection<String> getSysadmins();

    /**
     * Adds a new system administrator of the MUC service. A sysadmin has the same permissions as 
     * a room owner. 
     * 
     * @param userJID the bare JID of the new user to add as a system administrator.
     */
    void addSysadmin(String userJID);

    /**
     * Removes a system administrator of the MUC service.
     * 
     * @param userJID the bare JID of the user to remove from the list.
     */
    void removeSysadmin(String userJID);

    /**
     * Returns false if anyone can create rooms or true if only the returned JIDs in
     * <code>getUsersAllowedToCreate</code> are allowed to create rooms.
     *
     * @return true if only some JIDs are allowed to create rooms.
     */
    boolean isRoomCreationRestricted();

    /**
     * Sets if anyone can create rooms or if only the returned JIDs in
     * <code>getUsersAllowedToCreate</code> are allowed to create rooms.
     *
     * @param roomCreationRestricted whether anyone can create rooms or not.
     */
    void setRoomCreationRestricted(boolean roomCreationRestricted);

    /**
     * Returns the collection of JIDs that are allowed to create MUC rooms. An empty list means that
     * anyone can create a room. 
     * 
     * @return a list of bare JIDs.
     */
    Collection<String> getUsersAllowedToCreate();

    /**
     * Adds a new user to the list of JIDs that are allowed to create MUC rooms.
     * 
     * @param userJID the bare JID of the new user to add to list.
     */
    void addUserAllowedToCreate(String userJID);

    /**
     * Removes a user from list of JIDs that are allowed to create MUC rooms.
     * 
     * @param userJID the bare JID of the user to remove from the list.
     */
    void removeUserAllowedToCreate(String userJID);

    /**
     * Sets the time to elapse between clearing of idle chat users. A <code>TimerTask</code> will be
     * added to a <code>Timer</code> scheduled for repeated fixed-delay execution whose main
     * responsibility is to kick users that have been idle for a certain time. A user is considered
     * idle if he/she didn't send any message to any group chat room for a certain amount of time.
     * See {@link #setUserIdleTime(int)}.
     *
     * @param timeout the time to elapse between clearing of idle chat users.
     */
    void setKickIdleUsersTimeout(int timeout);

    /**
     * Returns the time to elapse between clearing of idle chat users. A user is considered
     * idle if he/she didn't send any message to any group chat room for a certain amount of time.
     * See {@link #getUserIdleTime()}.
     *
     * @return the time to elapse between clearing of idle chat users.
     */
    int getKickIdleUsersTimeout();

    /**
     * Sets the number of milliseconds a user must be idle before he/she gets kicked from all
     * the rooms. By idle we mean that the user didn't send any message to any group chat room.
     *
     * @param idle the amount of time to wait before considering a user idle.
     */
    void setUserIdleTime(int idle);

    /**
     * Returns the number of milliseconds a user must be idle before he/she gets kicked from all
     * the rooms. By idle we mean that the user didn't send any message to any group chat room.
     *
     * @return the amount of time to wait before considering a user idle.
     */
    int getUserIdleTime();

    /**
     * Sets the time to elapse between logging the room conversations. A <code>TimerTask</code> will
     * be added to a <code>Timer</code> scheduled for repeated fixed-delay execution whose main
     * responsibility is to log queued rooms conversations. The number of queued conversations to
     * save on each run can be configured. See {@link #setLogConversationBatchSize(int)}.
     *
     * @param timeout the time to elapse between logging the room conversations.
     */
    void setLogConversationsTimeout(int timeout);

    /**
     * Returns the time to elapse between logging the room conversations. A <code>TimerTask</code>
     * will be added to a <code>Timer</code> scheduled for repeated fixed-delay execution whose main
     * responsibility is to log queued rooms conversations. The number of queued conversations to
     * save on each run can be configured. See {@link #getLogConversationBatchSize()}.
     *
     * @return the time to elapse between logging the room conversations.
     */
    int getLogConversationsTimeout();

    /**
     * Sets the number of messages to save to the database on each run of the logging process.
     * Even though the saving of queued conversations takes place in another thread it is not
     * recommended specifying a big number.
     *
     * @param size the number of messages to save to the database on each run of the logging process.
     */
    void setLogConversationBatchSize(int size);

    /**
     * Returns the number of messages to save to the database on each run of the logging process.
     *
     * @return the number of messages to save to the database on each run of the logging process.
     */
    int getLogConversationBatchSize();

    /**
     * Obtain the server-wide default message history settings.
     * 
     * @return The message history strategy defaults for the server.
     */
    HistoryStrategy getHistoryStrategy();

    /**
     * Obtains a chatroom by name. A chatroom is created for that name if none exists and the user
     * has permission. The user that asked for the chatroom will be the room's owner if the chatroom
     * was created.
     * 
     * @param roomName Name of the room to get.
     * @param userjid The user's normal jid, not the chat nickname jid.
     * @return The chatroom for the given name.
     * @throws UnauthorizedException If the caller doesn't have permission to create a new room.
     */
    MUCRoom getChatRoom(String roomName, JID userjid) throws UnauthorizedException;

    /**
     * Obtains a chatroom by name. If the chatroom does not exists then null will be returned.
     * 
     * @param roomName Name of the room to get.
     * @return The chatroom for the given name or null if the room does not exists.
     */
    MUCRoom getChatRoom(String roomName);

    /**
     * Retuns a list with a snapshot of all the rooms in the server (i.e. persistent or not,
     * in memory or not).
     *
     * @return a list with a snapshot of all the rooms.
     */
    List<MUCRoom> getChatRooms();

    /**
     * Returns true if the server includes a chatroom with the requested name.
     * 
     * @param roomName the name of the chatroom to check.
     * @return true if the server includes a chatroom with the requested name.
     */
    boolean hasChatRoom(String roomName);

    /**
     * Removes the room associated with the given name.
     * 
     * @param roomName The room to remove.
     */
    void removeChatRoom(String roomName);

    /**
     * Removes a user from all chat rooms.
     * 
     * @param jabberID The user's normal jid, not the chat nickname jid.
     */
    void removeUser(JID jabberID);

    /**
     * Obtain a chat user by XMPPAddress.
     * 
     * @param userjid The XMPPAddress of the user.
     * @return The chatuser corresponding to that XMPPAddress.
     * @throws UserNotFoundException If the user is not found and can't be auto-created.
     */
    MUCUser getChatUser(JID userjid) throws UserNotFoundException;

    /**
     * Broadcast a given message to all members of this chat room. The sender is always set to be
     * the chatroom.
     * 
     * @param msg The message to broadcast.
     */
    void serverBroadcast(String msg) throws UnauthorizedException;

    /**
     * Returns the total chat time of all rooms combined.
     * 
     * @return total chat time in milliseconds.
     */
    public long getTotalChatTime();

    /**
     * Logs that a given message was sent to a room as part of a conversation. Every message sent
     * to the room that is allowed to be broadcasted and that was sent either from the room itself 
     * or from an occupant will be logged.<p>
     * 
     * Note: For performane reasons, the logged message won't be immediately saved. Instead we keep
     * the logged messages in memory until the logging process saves them to the database. It's 
     * possible to configure the logging process to run every X milliseconds and also the number 
     * of messages to log on each execution. 
     * @see org.jivesoftware.messenger.muc.spi.MultiUserChatServerImpl#initialize(org.jivesoftware.messenger.XMPPServer)
     * 
     * @param room the room that received the message.
     * @param message the message to log as part of the conversation in the room.
     * @param sender the real XMPPAddress of the sender (e.g. john@example.org). 
     */
    void logConversation(MUCRoom room, Message message, JID sender);
}