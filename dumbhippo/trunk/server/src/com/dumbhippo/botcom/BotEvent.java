package com.dumbhippo.botcom;

import java.io.Serializable;

public class BotEvent implements Serializable {
	private static final long serialVersionUID = 1L;

	private String botName;

	static public final String QUEUE = "IncomingAimQueue";
	
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
