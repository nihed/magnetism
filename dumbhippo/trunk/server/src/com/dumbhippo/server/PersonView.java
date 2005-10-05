package com.dumbhippo.server;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;

public interface PersonView {

	public EmailResource getEmail();

	public String getHumanReadableName();

	public Person getPerson();

	public Person getViewpoint();

}