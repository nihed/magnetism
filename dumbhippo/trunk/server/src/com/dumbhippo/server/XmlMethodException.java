package com.dumbhippo.server;

public class XmlMethodException extends Exception {
	private static final long serialVersionUID = 0L;

	private String code;
	
	public XmlMethodException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
