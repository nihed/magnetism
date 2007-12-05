package com.dumbhippo.dm.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class TestSuperUser extends TestUser {
	private String superPower;

	protected TestSuperUser() {
	}
	
	public TestSuperUser(String name, String superPower) {
		super(name);
		this.superPower = superPower;
	}

	@Column
	public String getSuperPower() {
		return superPower;
	}

	public void setSuperPower(String superPower) {
		this.superPower = superPower;
	}
}
