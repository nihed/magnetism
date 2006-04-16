/**
 * 
 */
package com.dumbhippo.server.util;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.NotFoundException;

class GuidNotFoundException extends NotFoundException {
	private static final long serialVersionUID = 0L;
	String guid;
	public GuidNotFoundException(Guid guid) {
		super("Guid " + guid + " was not in the database");
		this.guid = guid.toString();
	}
	public GuidNotFoundException(String guidString) {
		super("Guid " + guidString + " was not in the database");
		this.guid = guidString;
	}
	public String getGuid() {
		return guid;
	}
}
