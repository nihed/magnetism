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
	
	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Resource))
			return false;
		return ((Resource) arg0).getGuid().equals(getGuid());
	}
}
