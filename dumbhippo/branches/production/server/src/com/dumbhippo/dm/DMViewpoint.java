package com.dumbhippo.dm;

/**
 * The DMViewpoint interface represents the entity that is viewing or modifying the
 * data in a DMSession.
 * 
 * The DataModel interacts with the DMViewpoint via the methods that are invoked on
 * the viewpoint by filters. These methods are looked up by name when then filters are
 * compiled at data model startup, and thus are not defined in this interface. 
 * 
 * @author otaylor
 */
public interface DMViewpoint {
	
	/**
	 * Called to bind a DMViewpoint to a particular data model session. For some types
	 * of viewpoints (the system viewpoint in particular) this will be a no-op. The environment
	 * is responsible for making sure that a DMViewpoint where setSession() has an effect
	 * is not shared between multiple sessions at the same time.
	 * 
	 * @param session the session for the viewpoint
	 */
	public void setSession(DMSession session);
}
