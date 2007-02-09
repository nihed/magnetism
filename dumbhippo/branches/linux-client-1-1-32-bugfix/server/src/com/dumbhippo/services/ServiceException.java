package com.dumbhippo.services;

import org.xml.sax.SAXException;

/**
 * This exception is thrown when a web service returns us an error 
 * message. i.e. the XML parses fine, but contains an error instead
 * of the information we asked for. We want to distinguish this from 
 * cases where we just get bad XML or don't get the XML we expected.
 *
 */
public class ServiceException extends SAXException {
	private static final long serialVersionUID = 1L;

	private boolean unexpected;
	
	public ServiceException(boolean unexpected, String message) {
		super(message);
	}
	
	/**
	 *  true if we should warn about this error because we wouldn't 
	 *  "normally" get it
	 */ 
	public boolean isUnexpected() {
		return unexpected;
	}
	
	public boolean isSpanishInquisition() {
		return isUnexpected();
	}
}
