package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.identity20.Guid;


@Entity
public class Person extends GuidPersistable {
	public Person() { super(); }

	public Person(Guid guid) {
		super(guid);
	}
	
	@Transient
	public PersonView getSystemView() {
		return new PersonView(null, this);
	}
}
