package com.dumbhippo.server;

import javax.ejb.ApplicationException;

@ApplicationException
public class JabberUserNotFoundException extends Exception {

	private static final long serialVersionUID = 0L;

	public JabberUserNotFoundException() {
		super();
	}
	
	public JabberUserNotFoundException(String message) {
		super(message);
	}
	
	public JabberUserNotFoundException(String message, Throwable root) {
		super(message, root);
	}
	
}
