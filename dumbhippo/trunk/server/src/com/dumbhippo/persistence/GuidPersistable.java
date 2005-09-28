
package com.dumbhippo.persistence;

import com.dumbhippo.identity20.Guid;

public abstract class GuidPersistable {
	private Guid guid;
	
	protected GuidPersistable() {
		setGuid(Guid.createNew());
	}
	
	protected GuidPersistable(Guid guid) {
		setGuid(guid);
	}
	
	protected void setGuid(Guid guid) {
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
		setGuid(new Guid(hexId));
	}

}