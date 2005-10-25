package com.dumbhippo.server;

import javax.ejb.ApplicationException;

@ApplicationException
public class RedirectException extends Exception {

	private static final long serialVersionUID = 0L;

	public RedirectException(String htmlMessage, Throwable cause) {
		super(htmlMessage, cause);
	}

	public RedirectException(String htmlMessage) {
		super(htmlMessage);
	}
	
	// this is just to remind you about the escaping
	public String getHtmlMessage() {
		return getMessage();
	}
}
