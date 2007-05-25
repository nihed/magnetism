package com.dumbhippo.dm.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
public class TestUser extends TestGuidPersistable {
	private String name;
	private Set<TestGroupMember> groupMembers;
	
	protected TestUser() {
	}
	
	public TestUser(String name) {
		this.name = name;
	}

	@Column(nullable = false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy="member")
	public Set<TestGroupMember> getGroupMembers() {
		if (groupMembers == null)
			groupMembers = new HashSet<TestGroupMember>();
		
		return groupMembers;
	}
	
	public void setGroupMembers(Set<TestGroupMember> groupMembers) {
		this.groupMembers = groupMembers;
	}
}
