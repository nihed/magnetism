package com.dumbhippo.server;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.dumbhippo.FullName;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;

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
	private EmailResource email;
	private String humanReadableName;
	
	/**
	 * Construct a new PersonInfo object representing a view of a particular
	 * person by another object. Use IdentitySpider.getViewpoint() rather than
	 * this function.
	 * 
	 * @param p The Person object 
	 * @param e The 
	 */
	public PersonInfo(Person p, EmailResource e) {
		person = p;
		email = e;
		
		FullName name = person.getName();
		if (name != null && !name.isEmpty())
			humanReadableName = name.toString();
		else if (email != null) 
			humanReadableName = email.getEmail();
		else
			humanReadableName = "<Unknown>";
	}
		
	public Person getPerson() {
		return person;
	}
	
	public String getHumanReadableName() {
		return humanReadableName;
	}
	
	public EmailResource getEmail() {
		return email;
	}
	
	/**
	 * Convert an (unordered) set of PersonInfo into a a list and
	 * sort alphabetically with the default collator. You generally
	 * want to do this before displaying things to user, since
	 * iteration through Set will be in hash table order.
	 * 
	 * @param groups a set of Person objects
	 * @return a newly created List containing the sorted groups
	 */
	static public List<PersonInfo> sortedList(Set<PersonInfo> infos) {
		ArrayList<PersonInfo> list = new ArrayList<PersonInfo>();
		list.addAll(infos);

		final Collator collator = Collator.getInstance();
		Collections.sort(list, new Comparator<PersonInfo>() {
			public int compare (PersonInfo i1, PersonInfo i2) {
				return collator.compare(i1.getHumanReadableName(), i2.getHumanReadableName());
			}
		});
		
		return list;
	}
}
