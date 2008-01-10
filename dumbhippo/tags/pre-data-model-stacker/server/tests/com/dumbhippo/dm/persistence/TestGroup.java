package com.dumbhippo.dm.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

@Entity
public class TestGroup extends TestGuidPersistable {
	private String name;
	private Set<TestGroupMember> members;
	private boolean secret;
	
	protected TestGroup() {
	}
	
	public TestGroup(String name) {
		this.name = name;
		this.secret = false;
	}

	@Column(nullable = false)
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@OneToMany(mappedBy="group")
	public Set<TestGroupMember> getMembers() {
		if (members == null) {
			members = new HashSet<TestGroupMember>();
		}
		
		return members;
	}
	
	public void setMembers(Set<TestGroupMember> members) {
		this.members = members;
	}
	
	@Column(nullable = false)
	public boolean isSecret() {
		return secret;
	}
	
	public void setSecret(boolean secret) {
		this.secret = secret;
	}
}
