package com.dumbhippo.persistence;

import com.dumbhippo.identity20.Guid;


public class Person extends GuidPersistable {
	public Person() { super(); }

	public Person(Guid guid) {
		super(guid);
	}
	
	public PersonView getSystemView() {
		return new PersonView(null, this);
	}
}
