/**
 * 
 */
package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.GuidPersistable;

/**
 * A Resource is some object that has an associated GUID.
 * @author hp
 *
 */
public abstract class Resource extends GuidPersistable {
	
	public Resource() {
	}
	
	public Resource(Guid guid) {
		super(guid);
	}

	/* TODO EJB - Should be @Synthetic or whatever */
	/**
	 * Return a human-readable string form of this object.
	 * @return a readable string, or null if none
	 */
	public abstract String getHumanReadableString();
}
