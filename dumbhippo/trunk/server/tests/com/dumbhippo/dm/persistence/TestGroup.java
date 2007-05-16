package com.dumbhippo.dm.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;


@Entity
public class TestGroup extends TestGuidPersistable {
	private String name;
	
	protected TestGroup() {
	}
	
	public TestGroup (String name) {
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
