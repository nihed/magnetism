package com.dumbhippo.server;

import com.dumbhippo.persistence.Person;

/**
 * The Viewpoint class is simply a wrapper for Person to give us
 * type checking in functions that take both a Person that is viewing
 * and a person that is being viewed.
 * 
 * @author otaylor
 */
public class Viewpoint {
	final Person viewer;
	
	public Viewpoint(Person p) {
		viewer = p;
	}
	
	public Person getViewer() {
		return viewer;
	}
}
