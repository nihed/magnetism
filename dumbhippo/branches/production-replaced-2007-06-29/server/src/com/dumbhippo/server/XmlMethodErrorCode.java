package com.dumbhippo.server;

/**
 * If you change these, you need to look for the corresponding strings
 * in all javascript files and change those also.
 * 
 */
public enum XmlMethodErrorCode {
	INTERNAL_SERVER_ERROR(false),
	FAILED(false),
	UNKNOWN_GROUP(false),
	INVALID_URL(true),
	NETWORK_ERROR(true),
	PARSE_ERROR(true),
	NOT_FOUND(true),
	NOT_READY(false),
	INVALID_ARGUMENT(true),
	NOT_LOGGED_IN(true),
	FORBIDDEN(true);
	
	private XmlMethodErrorCode(boolean expected) {
		this.expected = expected;
	}
	
	/*
	 * Whether or not this exception code should happen under
	 * "normal" conditions.  Network errors and invalid input
	 * from web services fall into this category.  Parsing
	 * *trusted* data or writing to the database though for example
	 * would not.  This distinction is usually used to determine
	 * whether or not to log the full exception with backtrace.
	 * 
	 * Really this difference should have simply been reflected
	 * by only using XmlMethodException for the former case, and
	 * RuntimeException for the latter, but that would be a 
	 * significantly more invasive change.
	 */
	private boolean expected;
	
	public boolean isExpected() { return expected; }
}
