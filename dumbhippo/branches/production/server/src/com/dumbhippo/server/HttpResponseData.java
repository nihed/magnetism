package com.dumbhippo.server;

/**
 * Kinds of data that an HTTP-exported method can return; given
 * as an argument to the HttpContentTypes annotation.
 * 
 * @author hp
 *
 */
public enum HttpResponseData {
	NONE(null),
	TEXT("text/plain"),
	XML("text/xml"),
	XMLMETHOD("text/xml");	
	
	private String mimeType;
	
	HttpResponseData(String mimeType) {
		this.mimeType = mimeType;
	}
	
	/**
	 * Return equivalent MIME type. Note returns 
	 * null for NONE.
	 * @return equivalent MIME type.
	 */
	public String getMimeType() {
		return mimeType;
	}
}
