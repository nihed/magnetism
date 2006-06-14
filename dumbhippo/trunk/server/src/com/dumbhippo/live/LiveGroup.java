package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Live state for a Group
 * 
 * @author Havoc Pennington
 *
 */
public class LiveGroup extends LiveObject {

	private int totalReceivedPosts;
	private int memberCount;
	private int followerCount;
	private int invitedMemberCount;
	private int invitedFollowerCount;
	
	public LiveGroup(Guid guid) {
		super(guid);
	}
	
	public void discard() {
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	// note this is only OK because LiveObject is abstract, if 
	// concrete LiveObject existed this would break transitivity
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof LiveGroup))
			return false;
		LiveGroup group = (LiveGroup) arg;
		return super.equals(group);
	}
	
	public int getMemberCount() {
		return memberCount;
	}

	public void setMemberCount(int memberCount) {
		this.memberCount = memberCount;
	}

	public int getInvitedMemberCount() {
		return invitedMemberCount;
	}

	public void setInvitedMemberCount(int invitedCount) {
		this.invitedMemberCount = invitedCount;
	}	

	public int getTotalReceivedPosts() {
		return totalReceivedPosts;
	}

	public void setTotalReceivedPosts(int totalReceivedPosts) {
		this.totalReceivedPosts = totalReceivedPosts;
	}

	public int getFollowerCount() {
		return followerCount;
	}

	public void setFollowerCount(int followerCount) {
		this.followerCount = followerCount;
	}

	public int getInvitedFollowerCount() {
		return invitedFollowerCount;
	}

	public void setInvitedFollowerCount(int invitedFollowerCount) {
		this.invitedFollowerCount = invitedFollowerCount;
	}
}
