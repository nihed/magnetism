package com.dumbhippo.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements a cache of information about AIM chat room
 * status.
 */

public class ChatRoomStatusCache {
	
	private static Map<String,List<String>> chatRoomStatusMap = new HashMap<String,List<String>>();
	
	public static void putChatRoomStatus(String key, List<String> value) {
		chatRoomStatusMap.put(key, value);
	}
	
	public static List<String> getChatRoomStatus(String key) {
		return chatRoomStatusMap.get(key);
	}
}
