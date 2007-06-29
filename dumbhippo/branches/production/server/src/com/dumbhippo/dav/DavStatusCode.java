package com.dumbhippo.dav;

import javax.servlet.http.HttpServletResponse;

public enum DavStatusCode {
	
	/* Dav-specific codes from
	 * http://www.webdav.org/specs/rfc2518.html#status.code.extensions.to.http11 
	 */
	
	PROCESSING(102),
	
	MULTI_STATUS(207),
	
	UNPROCESSABLE_ENTITY(402),
	
	CONFLICT(409), // required if you try to create a file in a nonexistent directory
	
	LOCKED(423),
	
	FAILED_DEPENDENCY(424),
	
	INSUFFICIENT_STORAGE(507),
	
	/* normal HTTP codes */ 
	
	OK(HttpServletResponse.SC_OK),
	
	// This means the form submitted OK but there's no reply data
	NO_CONTENT(HttpServletResponse.SC_NO_CONTENT),
	
	BAD_REQUEST(HttpServletResponse.SC_BAD_REQUEST),
	
	MOVED_PERMANENTLY(HttpServletResponse.SC_MOVED_PERMANENTLY),
	
	NOT_FOUND(HttpServletResponse.SC_NOT_FOUND),
	
	INTERNAL_SERVER_ERROR(HttpServletResponse.SC_INTERNAL_SERVER_ERROR),

	FORBIDDEN(HttpServletResponse.SC_FORBIDDEN),
	
	// this means "we are hosed for a minute, try back in a bit"
	// There's a Retry-After header we could set...
	SERVICE_UNAVAILABLE(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
	
	private int code;
	
	DavStatusCode(int code) {
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}
