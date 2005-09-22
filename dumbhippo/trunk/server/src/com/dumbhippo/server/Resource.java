/**
 * 
 */
package com.dumbhippo.server;

/**
 * A Resource is some object that the system knows about.
 *  
 * @author hp
 *
 */
abstract class Resource {
	private Guid guid;

	Resource() {
		guid = Guid.createNew();
	}
	
	Resource(Guid guid) {
		this.guid = guid;
	}
	
	Guid getGuid() {
		return guid;
	}
	
	/**
	 * This is private so only Hibernate can change the GUID
	 * 
	 * @param g the new guid
	 */
	private void setGuid(Guid guid) {
		this.guid = guid;
	}
}
