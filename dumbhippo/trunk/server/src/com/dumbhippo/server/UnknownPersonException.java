package com.dumbhippo.server;

public class UnknownPersonException extends Exception {
	private static final long serialVersionUID = 0L;

	private String personName;
	
	public UnknownPersonException(String message, String humanReadablePersonName) {
		super(message);
		personName = humanReadablePersonName;
	}
	
	public String getPersonName() {
		return personName;
	}
}
