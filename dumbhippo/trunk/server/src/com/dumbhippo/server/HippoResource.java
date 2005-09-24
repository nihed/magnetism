/**
 * 
 */
package com.dumbhippo.server;

import com.dumbhippo.persistence.Resource;

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

	public String getHippoHandle() {
		return hippoHandle;
	}

	public void setHippoHandle(String hippoHandle) {
		this.hippoHandle = hippoHandle;
	}
	
}
