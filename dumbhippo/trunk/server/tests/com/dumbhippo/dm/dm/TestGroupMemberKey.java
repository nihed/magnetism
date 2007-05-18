package com.dumbhippo.dm.dm;

import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.identity20.Guid;

public class TestGroupMemberKey implements DMKey {
	private Guid groupId;
	private Guid memberId;

	public TestGroupMemberKey(Guid groupId, Guid memberId) {
		this.groupId = groupId;
		this.memberId = memberId;
	}
	
	public TestGroupMemberKey(TestGroupMember groupMember) {
		this.groupId = groupMember.getGroup().getGuid();
		this.memberId = groupMember.getMember().getGuid();
	}
	

	public Guid getGroupId() {
		return groupId;
	}

	public Guid getMemberId() {
		return memberId;
	}

	@Override
	public TestGroupMemberKey clone() {
		return this; // Immutable, nothing session-specific
	}
	
	@Override
	public int hashCode() {
		return groupId.hashCode() * 11 + memberId.hashCode() * 17;  
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TestGroupMemberKey))
			return false;
		
		TestGroupMemberKey other = (TestGroupMemberKey)o;
		return other.groupId.equals(groupId) && other.memberId.equals(memberId);
		
	}

	@Override
	public String toString() {
		return groupId.toString() + "." + memberId.toString();
	}
}
