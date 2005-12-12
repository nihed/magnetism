package com.dumbhippo.botcom;

public class BotEventToken extends BotEvent {
	private static final long serialVersionUID = 1L;

	private String aimName;
	private String token;
	
	public BotEventToken(String botName, String aimName, String token) {
		super(botName);
		this.token = token;
		this.aimName = aimName;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getAimName() {
		return aimName;
	}
	
	@Override
	public String toString() {
		return this.getClass().getName() + " from bot " + getBotName() + " from AIM user " + getAimName() + " token is " + getToken();
	}
}
