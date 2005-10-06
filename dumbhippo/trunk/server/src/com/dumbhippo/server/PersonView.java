package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;

@Local
public interface PersonView {

	public EmailResource getEmail();

	public String getHumanReadableName();

	public Person getPerson();

	public Person getViewpoint();

}