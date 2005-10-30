package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;

@Local
public interface PersonView {

	/**
	 * Initialize the PersonView object with a particular viewer and viewee.
	 * 
	 * @param viewpoint the person doing the viewing; may be null for an anymous view
	 * @param p the person being viewed
	 */
	public void init(Person viewpoint, Person p);
	
	public EmailResource getEmail();

	public String getHumanReadableName();

	public Person getPerson();

	public Person getViewpoint();

}