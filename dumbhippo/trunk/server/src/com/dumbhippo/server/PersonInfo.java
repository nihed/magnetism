package com.dumbhippo.server;

import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.PersonView;

/**
 * @author otaylor
 *
 * This is a class encapsulating information about a Person that can be
 * returned out of the session tier and used by web pages; only the
 * constructor makes queries into the database; the read-only properties of 
 * this object access pre-computed data.
 * 
 * This class is a person as viewed by another person; it differs from
 * PersonView primarily in not being a session bean.
 */
public class PersonInfo {
	private Person person;
	private String humanReadableName;
	
	public PersonInfo(IdentitySpider spider, Person viewer, Person p) {
		person = p;
		
		PersonView personView = spider.getViewpoint(viewer, person);
		humanReadableName = personView.getHumanReadableName();
	}
	
	public Person getPerson() {
		return person;
	}
	
	public String getHumanReadableName() {
		return humanReadableName;
	}
}
