package com.dumbhippo.botcom;

public class BotTaskFailedException extends Exception {

	private static final long serialVersionUID = 1L;

	public BotTaskFailedException(Throwable cause) {
		super(cause);
	}
	public BotTaskFailedException(String message, Throwable cause) {
		super(message, cause);
	}
	public BotTaskFailedException(String message) {
		super(message);
	}
}
