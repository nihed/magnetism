/**
 * 
 */
package com.dumbhippo.persistence;

import com.dumbhippo.identity20.Guid;

/**
 * A Resource is some object that has an associated GUID.
 * @author hp
 *
 */
public abstract class Resource implements GuidPersistable {
	protected Guid guid;
	
	public Resource() {
		guid = Guid.createNew();
	}
	
	public Resource(Guid guid) {
		this.guid = guid;
	}
	
	public Guid getGuid() {
		assert guid != null;
		return guid;
	}

	/** 
	 * For hibernate to use as the ID column. 
	 * Should return guid.toString() generally.
	 * 
	 * @return the hex string form of the GUID
	 */
	public String getId() {
		String s = getGuid().toString();
		assert s.length() == Guid.STRING_LENGTH;
		return s;		
	}

	/** 
	 * If anyone other than Hibernate calls this it's 
	 * probably bad and evil.
	 * 
	 * @param hexId the hex GUID to set
	 */
	public void setId(String hexId) {
		guid = new Guid(hexId);
	}

	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Resource))
			return false;
		return ((Resource) arg0).guid.equals(guid);
	}
	
}
