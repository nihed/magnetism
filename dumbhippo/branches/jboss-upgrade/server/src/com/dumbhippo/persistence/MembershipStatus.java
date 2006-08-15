package com.dumbhippo.persistence;

/**
 * This enum is used in database persistence, so changing it affects the schema.
 * We rely on the ordinal values in a couple of cases:
 *   - database queries with "status >= INVITED" or "status >= REMOVED" for whether someone
 *     can see or modify the group, etc.
 *   - when an account ends up with multiple GroupMember for the same group, we 
 *     save the highest status when merging them
 * 
 * @author otaylor
 */
public enum MembershipStatus {
	NONMEMBER(false),         // 0 - This shouldn't be in the database; it may be used elsewhere
	                          //     to indicate that there was no entry in the database
	INVITED_TO_FOLLOW(true),  // 1 - Invited to be a follower (by another follower)
	FOLLOWER(true),           // 2 - You read the group but can't post, invite people, etc.
	REMOVED(false),           // 3 - Was removed (probably by themself), can choose to rejoin
	INVITED(true),            // 4 - Invited to group, hasn't indicated acceptance
	ACTIVE(true);             // 5 - Normal member
	
	private boolean receivesPosts;
	
	private MembershipStatus(boolean receivesPosts) {
		this.receivesPosts = receivesPosts;  
	}
	
	/**
	 * 
	 * @return true if this status should get new posts to the group
	 */
	public boolean getReceivesPosts() {
		return receivesPosts;
	}
}
