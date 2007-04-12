package com.dumbhippo.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class GroupMembershipPolicyRevision extends GroupRevision {
	private boolean open;
	private int followers;
	private int invitedFollowers;
	
	public GroupMembershipPolicyRevision(User revisor, Group target, Date time, boolean open, int followers, int invitedFollowers) {
		super(RevisionType.GROUP_MEMBERSHIP_POLICY_CHANGED, revisor, target, time);
		this.open = open;
		this.followers = followers;
		this.invitedFollowers = invitedFollowers;
	}

	protected GroupMembershipPolicyRevision() {	
	}
	
	@Column(nullable=false)
	public boolean isOpen() {
		return open;
	}

	protected void setOpen(boolean open) {
		this.open = open;
	}

	@Column(nullable=false)
	public int getFollowers() {
		return followers;
	}

	protected void setFollowers(int followers) {
		this.followers = followers;
	}
	
	@Column(nullable=false)
	public int getInvitedFollowers() {
		return invitedFollowers;
	}

	protected void setInvitedFollowers(int invitedFollowers) {
		this.invitedFollowers = invitedFollowers;
	}
}
