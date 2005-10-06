package com.dumbhippo.persistence;

import javax.persistence.Entity;

import com.dumbhippo.identity20.Guid;


@Entity
public class Person extends GuidPersistable {

	private static final long serialVersionUID = 0L;

	public Person() { super(); }

	public Person(Guid guid) {
		super(guid);
	}
}
