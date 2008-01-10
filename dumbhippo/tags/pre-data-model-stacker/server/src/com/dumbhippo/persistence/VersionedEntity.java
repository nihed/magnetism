package com.dumbhippo.persistence;

/* Interface for accessing objects which have both an ID and
 * a version.
 * 
 * @author walters
 */
public interface VersionedEntity {
	public String getId();
	
	public int getVersion();
	
	public void setVersion(int version);
}
