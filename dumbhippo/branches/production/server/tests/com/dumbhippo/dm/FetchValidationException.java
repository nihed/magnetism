package com.dumbhippo.dm;


public class FetchValidationException extends Exception {
	private static final long serialVersionUID = 1L;

	// Formatting because hey, if we aren't convenient for tests, when are we convenient?
	public FetchValidationException(String message, Object... args) {
		super(String.format(message, args));
	}
	
	// Differs from the standard exception-after standard to avoid ambiguity with the above 
	public FetchValidationException(Exception cause, String message, Object... args) {
		super(String.format(message, args), cause);
	}
}
