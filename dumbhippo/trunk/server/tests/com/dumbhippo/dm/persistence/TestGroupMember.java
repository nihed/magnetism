package com.dumbhippo.dm.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class TestGroupMember extends TestDBUnique {
	private TestGroup group;
	private TestUser member;
	private boolean removed; 

	public TestGroupMember(TestGroup group, TestUser user) {
		this.group = group;
		this.member = user;
		this.removed = false;
	}

	protected TestGroupMember() {
	}

	@ManyToOne
	@JoinColumn(nullable = false)
	public TestGroup getGroup() {
		return group;
	}

	protected void setGroup(TestGroup group) {
		this.group = group;
	}

	@ManyToOne
	@JoinColumn(nullable = false)
	public TestUser getMember() {
		return member;
	}

	protected void setMember(TestUser member) {
		this.member = member;
	}

	public void setRemoved(boolean removed) {
		this.removed = removed;
	}
	
	@Column(nullable = false)
	public boolean isRemoved() {
		return removed;
	}
}
