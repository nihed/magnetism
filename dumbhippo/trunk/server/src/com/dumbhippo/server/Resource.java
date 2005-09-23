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
abstract class Resource implements GuidPersistable {
	private Guid guid;
	private boolean newlyCreated;

	public Resource() {
		guid = Guid.createNew();
		newlyCreated = true;
	}
	
	public Resource(Guid guid) {
		this.guid = guid;
		// probably false in this case, but paranoia
		newlyCreated = true;
	}
	
	public Guid getGuid() {
		assert guid != null;
		return guid;
	}
	
	/** 
	 * For hibernate
	 * @return the hex string form of the GUID
	 */
	public String getId() {
		String s = guid.toString();
		assert s.length() == Guid.STRING_LENGTH;
		return s;
	}
	
	public void setId(String hexGuid) {
		guid = new Guid(hexGuid);
	}
	
	/** 
	 * Whether the object was loaded from the database
	 * (or in general, whether we believe it already exists).
	 * If we believe it's newly created and it exists, 
	 * we need to throw an error as we have a guid collision
	 * that could be a security issue.
	 * 
	 * @return true if newly created
	 */
	public boolean getNewlyCreated() {
		return newlyCreated;
	}
	
	public void setNewlyCreated(boolean newlyCreated) {
		this.newlyCreated = newlyCreated;
	}
}
