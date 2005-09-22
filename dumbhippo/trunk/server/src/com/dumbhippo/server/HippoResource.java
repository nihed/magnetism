/**
 * 
 */
package com.dumbhippo.server;

/**
 * Resource representing a Hippo account, probably 
 * a combination email address and Jive Messenger JID?
 * Or just a leet nick?
 * 
 * @author hp
 *
 */
class HippoResource extends Resource {
	private String hippoHandle;

	String getHippoHandle() {
		return hippoHandle;
	}

	void setHippoHandle(String hippoHandle) {
		this.hippoHandle = hippoHandle;
	}
	
}
