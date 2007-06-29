package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.GroupMember;

public interface GroupMembershipListener {
	public void onGroupMemberCreated(GroupMember member, long when, boolean notifyGroupMembers);
	public void onGroupMemberStatusChanged(GroupMember member, long when, boolean notifyGroupMembers);
}
