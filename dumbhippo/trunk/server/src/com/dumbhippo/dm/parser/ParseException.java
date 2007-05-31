package com.dumbhippo.dm.parser;

import antlr.RecognitionException;
import antlr.TokenStreamException;

public class ParseException extends Exception {
	private static final long serialVersionUID = 1L;

	public ParseException(RecognitionException e) {
		super(e.line + ":" + e.column + ": " + e.getMessage(), e);
	}
	
	public ParseException(TokenStreamException e) {
		super(e.getMessage(), e);
	}
}