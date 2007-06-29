package com.dumbhippo.jive.rooms;

/**
 * This enumeration tells whether a user is looking
 * at or chatting about a post, which is internally represented
 * by types of membership in the posts chat room.
 * 
 * @author otaylor
 */
public enum RoomUserStatus {
	NONMEMBER,     // Not looking at the post
	VISITOR,       // Viewing the post
	PARTICIPANT    // Chatting in the post's chat room
}
