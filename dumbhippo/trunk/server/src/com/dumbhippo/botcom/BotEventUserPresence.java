package com.dumbhippo.botcom;

import java.util.Map;

/**
 * BotEventUserPresence encapsultes an update from the bot to the server
 * with a list of screen names and their online/offline status.
 * 
 * @author dff
 */
public class BotEventUserPresence extends BotEvent {
	private static final long serialVersionUID = 1L;
	
	Map<String,Boolean> userOnlineMap;
	
	public BotEventUserPresence(String botName, Map<String,Boolean> userOnlineMap) {
		super(botName);
		this.userOnlineMap = userOnlineMap;
	}
	
	@Override
	public String toString() {
		return this.getClass().getName() + " from bot " + getBotName() + " with " + userOnlineMap;
	}

	public Map<String, Boolean> getUserOnlineMap() {
		return userOnlineMap;
	}
	
}
