package com.dumbhippo.server;

import com.dumbhippo.persistence.User;

/**
 * The Viewpoint class is simply a wrapper for Person to give us
 * type checking in functions that take both a Person that is viewing
 * and a person that is being viewed.
 * 
 * @author otaylor
 */
public class Viewpoint {
	final User viewer;
	
	public Viewpoint(User viewer) {
		this.viewer = viewer;
	}
	
	public User getViewer() {
		return viewer;
	}	
}
