package com.dumbhippo.dav;

public class DavHttpStatusException extends Exception {

	private static final long serialVersionUID = 1L;

	private DavStatusCode code;
	
	public DavHttpStatusException(DavStatusCode code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}
	
	public DavHttpStatusException(DavStatusCode code, String message) {
		super(message);
		this.code = code;
	}
	
	public DavStatusCode getCode() {
		return code;
	}
}
