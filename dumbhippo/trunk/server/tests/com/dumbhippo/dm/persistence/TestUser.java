package com.dumbhippo.dm.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class TestUser extends TestGuidPersistable {
	private String name;
	
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
}
