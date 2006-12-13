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
	NONMEMBER        (false, false, false), // 0 - This shouldn't be in the database; it may be used elsewhere
	                                        //     to indicate that there was no entry in the database
	INVITED_TO_FOLLOW(true,  false, false), // 1 - Invited to be a follower (by another follower)
	FOLLOWER         (true,  false, false), // 2 - You read the group but can't post, invite people, etc.
	REMOVED          (false, true,  false), // 3 - Was removed (probably by themself), can choose to rejoin
	INVITED          (true,  true,  true),  // 4 - Invited to group, hasn't indicated acceptance
	ACTIVE           (true,  true,  true);  // 5 - Normal member
	
	private boolean receivesPosts;
	private boolean canSeeSecretGroup;
	private boolean canSeeSecretMembers;
	
	private MembershipStatus(boolean receivesPosts, boolean canSeeSecretGroup, boolean canSeeSecretMembers) {
		this.receivesPosts = receivesPosts;  
		this.canSeeSecretGroup = canSeeSecretGroup;
		this.canSeeSecretMembers = canSeeSecretMembers;
	}
	
	/**
	 * 
	 * @return true if this status should get new posts to the group
	 */
	public boolean getReceivesPosts() {
		return receivesPosts;
	}
	
	/**
	 * @return true if a member of this status can see that a SECRET group exists and join it.
	 */
	public boolean getCanSeeSecretGroup() {
		return canSeeSecretGroup;
	}
	
	/**
	 * @return true if a member of this status can see the members and posts to a SECRET group.
	 * The idea of separating this from canSeeSecretGroup is that we don't want people to
	 * be able to spy on a secret group without being in it (for social, not security reasons,
	 * you can always join-look-leave), but if we made the group vanish when you left it,
	 * it would make it hard to undo the operation and join it again.
	 */
	public boolean getCanSeeSecretMembers() {
		return canSeeSecretMembers;
	}
	
	/**
	 * Can a member of this status change the group photo, etc.
	 * @return true if the group member can change stuff
	 */
	public boolean getCanModify() {
		return ordinal() >= ACTIVE.ordinal();
	}

	/**
	 * Can a member of this status share this group with others through an invite.
	 * @ return true if group member can share group with others
	 */
	public boolean getCanShare() {
		return (ordinal() == ACTIVE.ordinal() || ordinal() == FOLLOWER.ordinal());
	}
	
	// REMOVED is unordered for the purposes of better/worse
	
	public boolean betterThan(MembershipStatus oldStatus) {
		if (oldStatus == MembershipStatus.REMOVED)
			return false;
		else if (this == MembershipStatus.REMOVED)
			return false;
		else
			return ordinal() > oldStatus.ordinal();
	}

	public boolean worseThan(MembershipStatus oldStatus) {
		if (oldStatus == MembershipStatus.REMOVED)
			return false;
		else if (this == MembershipStatus.REMOVED)
			return false;
		else
			return ordinal() < oldStatus.ordinal();
	}
}
