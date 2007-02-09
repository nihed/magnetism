package com.dumbhippo.server;

public class XmlMethodException extends Exception {
	private static final long serialVersionUID = 0L;

	private XmlMethodErrorCode code;
	
	public XmlMethodException(XmlMethodErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	public XmlMethodErrorCode getCode() {
		return code;
	}
	
	public String getCodeString() {
		return code.name();
	}
}
