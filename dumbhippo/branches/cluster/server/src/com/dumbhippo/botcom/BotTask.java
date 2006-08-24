package com.dumbhippo.botcom;

import java.io.Serializable;

public abstract class BotTask implements Serializable {

	private String botName;
	static public final String QUEUE = "OutgoingAimQueue";

	public BotTask(String botName) {
		this.botName = botName;
	}

	/**
	 * Bot to use, or null if it doesn't matter.
	 * @return name of the bot to use or null
	 */
	public String getBotName() {
		return botName;
	}
}
