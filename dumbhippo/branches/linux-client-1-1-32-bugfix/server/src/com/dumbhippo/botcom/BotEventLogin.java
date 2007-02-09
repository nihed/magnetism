package com.dumbhippo.botcom;

/**
 * Encapsulates a request for user login relayed from the bot
 * to the main server.
 */
public class BotEventLogin extends BotEvent {
	private static final long serialVersionUID = 1L;

	private String aimName;
	
	public BotEventLogin(String botName, String aimName) {
		super(botName);
		this.aimName = aimName;
	}
	
	public String getAimName() {
		return aimName;
	}
	
	@Override
	public String toString() {
		return this.getClass().getName() + " from bot " + getBotName() + " from AIM user " + getAimName();
	}
}
