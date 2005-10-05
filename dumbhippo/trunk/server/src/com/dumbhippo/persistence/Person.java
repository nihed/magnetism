package com.dumbhippo.persistence;

import javax.persistence.Entity;

import com.dumbhippo.identity20.Guid;


@Entity
public class Person extends GuidPersistable {
	public Person() { super(); }

	public Person(Guid guid) {
		super(guid);
	}
}
