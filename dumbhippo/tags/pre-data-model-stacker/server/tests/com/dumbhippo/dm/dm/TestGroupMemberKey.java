package com.dumbhippo.dm.dm;

import com.dumbhippo.dm.BadIdException;
import com.dumbhippo.dm.DMKey;
import com.dumbhippo.dm.persistence.TestGroupMember;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

public class TestGroupMemberKey implements DMKey {
	private static final long serialVersionUID = -5724889995159821821L;

	private Guid groupId;
	private Guid memberId;
	
	public TestGroupMemberKey(Guid groupId, Guid memberId) {
		this.groupId = groupId;
		this.memberId = memberId;
	}
	
	public TestGroupMemberKey(String keyString) throws BadIdException {
		String[] strings = keyString.split("\\.");
		if (strings.length != 2)
			throw new BadIdException("Invalid group member key: " + keyString);
		
		try {
			this.groupId = new Guid(strings[0]);
			this.memberId = new Guid(strings[1]);
		} catch (ParseException e) {
			throw new BadIdException("Invalid GUID in group member key");
		}
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
