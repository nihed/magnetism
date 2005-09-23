
package com.dumbhippo.server;




interface GuidPersistable {

	/** 
	 * For hibernate to use as the ID column. 
	 * Should return guid.toString() generally.
	 * 
	 * @return the hex string form of the GUID
	 */
	public String getId();

	/** 
	 * If anyone other than Hibernate calls this it's 
	 * probably bad and evil.
	 * 
	 * @param hexId the hex GUID to set
	 */
	public void setId(String hexId);
	
	/** 
	 * Whether the object was loaded from the database
	 * (or in general, whether we believe it already exists).
	 * If we believe it's newly created and it exists, 
	 * we need to throw an error as we have a guid collision
	 * that could be a security issue. So this flag lets
	 * us do that.
	 * 
	 * @return true if newly created
	 */
	public boolean getNewlyCreated();

	public void setNewlyCreated(boolean newlyCreated);	
}