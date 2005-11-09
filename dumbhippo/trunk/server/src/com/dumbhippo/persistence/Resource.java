/**
 * 
 */
package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.identity20.Guid;

/**
 * A Resource is some object that has an associated GUID
 * and everyone can have a different opinion about its owner.
 * i.e. make something a Resource rather than GuidPersistable
 * if you want it to be subject to ResourceOwnershipClaim
 * (see also PersonView)
 * 
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
	
	@Override
	public String toString() {
		// this is for debug spew, getHumanReadableString() is for showing to humans
		// (the quotes are really just so we notice if we mess that up)
		return "'" + getHumanReadableString() + "'";
	}
}
