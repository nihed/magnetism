package com.dumbhippo.persistence;

import javax.persistence.Entity;
import javax.persistence.Transient;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewBean;


@Entity
public class Person extends GuidPersistable {
	public Person() { super(); }

	public Person(Guid guid) {
		super(guid);
	}
	
	@Transient
	public PersonView getSystemView() {
		return new PersonViewBean(null, this);
	}
}
