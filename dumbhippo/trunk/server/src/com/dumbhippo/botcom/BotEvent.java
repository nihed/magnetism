package com.dumbhippo.botcom;

public class BotEvent {
	private String botName;
	
	public BotEvent(String botName) {
		this.botName = botName;
	}
	
	public String getBotName() {
		return botName;
	}
	
	@Override
	public String toString() {
		return "BotEvent from bot " + getBotName();
	}
}
