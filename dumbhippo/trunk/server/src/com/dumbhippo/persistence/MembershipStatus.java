package com.dumbhippo.persistence;

/**
 * This enum is used in database persistence, so changing it affects the schema.
 * 
 * @author otaylor
 */
public enum MembershipStatus {
	NONMEMBER, // This shouldn't be in the database; it may be used elsewhere
	           // to indicate that there was no entry in the database
	REMOVED,   // Was removed (probably by themself), can choose to rejoin
	INVITED,   // Invited to group, hasn't indicated acceptance
	ACTIVE;     // Normal member
	
	public boolean isMember() {
		return ordinal() >= INVITED.ordinal();
	}
}
