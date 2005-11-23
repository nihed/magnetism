package com.dumbhippo.server;

import java.util.HashMap;

/**
 * This class implements a cache of information about AIM chat room
 * status.
 */

public class ChatRoomStatusCache {
	
	private static HashMap chatRoomStatusMap = new HashMap();
	
	public static void putChatRoomStatus(Object key, Object value) {
		chatRoomStatusMap.put(key, value);
	}
	
	public static Object getChatRoomStatus(Object key) {
		return chatRoomStatusMap.get(key);
	}
}