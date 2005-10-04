/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.identity20.Guid;

/**
 * A Resource is some object that has an associated GUID.
 * @author hp
 *
 */
@Entity
public abstract class Resource extends GuidPersistable {
	
	public Resource() {
	}
	
	public Resource(Guid guid) {
		super(guid);
	}

	/**
	 * Return a human-readable string form of this object.
	 * @return a readable string, or null if none
	 */
	@Transient
	public abstract String getHumanReadableString();
}
