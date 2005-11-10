package com.dumbhippo.aimbot;

public class BotTaskFailedException extends Exception {

	private static final long serialVersionUID = 1L;

	BotTaskFailedException(Throwable cause) {
		super(cause);
	}
	BotTaskFailedException(String message, Throwable cause) {
		super(message, cause);
	}
	BotTaskFailedException(String message) {
		super(message);
	}
}
