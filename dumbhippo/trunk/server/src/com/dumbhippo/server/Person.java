package com.dumbhippo.server;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.GuidPersistable;


public class Person extends GuidPersistable {
	public Person() { super(); }

	public Person(Guid guid) {
		super(guid);
	}
}
