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
import com.dumbhippo.persistence.User;

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
public class PersonView {
	
	public static final int MAX_SHORT_NAME_LENGTH = 15;
	
	private Person person;
	private User user;
	private EmailResource email;
	private String humanReadableName;
	
	/**
	 * Construct a new PersonView object representing a view of a particular
	 * person by another object. Use IdentitySpider.getViewpoint() rather than
	 * this function.
	 * 
	 * @param p The Person object 
	 * @param e The 
	 */
	public PersonView(Person p, User u, EmailResource e) {
		person = p;
		user = u;
		email = e;
		
		FullName name = null;
		
		if (person != null)
			name = person.getName();
		if ((name == null || name.isEmpty()) && user != null)
			name = user.getName();
		
		if (name != null && !name.isEmpty())
			humanReadableName = name.getFullName();
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
	
	public String getHumanReadableShortName() {
		String name= null;
		if (person != null)
			name = person.getNickname();
		if ((name == null || name.length() == 0) && user != null)
			name = user.getNickname();
		if ((name == null || name.length() == 0) && person != null)
			name = person.getName().getFirstName();
		if ((name == null || name.length() == 0) && user != null)
			name = user.getName().getFirstName();
		if (name == null || name.length() == 0)
			name = humanReadableName;

		if (name.length() > MAX_SHORT_NAME_LENGTH) {
			return name.substring(0, MAX_SHORT_NAME_LENGTH);
		} else {
			return name;
		}
	}
	
	public User getUser() {
		return user;
	}
	
	public EmailResource getEmail() {
		return email;
	}
	
	/**
	 * Convert an (unordered) set of PersonView into a a list and
	 * sort alphabetically with the default collator. You generally
	 * want to do this before displaying things to user, since
	 * iteration through Set will be in hash table order.
	 * 
	 * @param groups a set of Person objects
	 * @return a newly created List containing the sorted groups
	 */
	static public List<PersonView> sortedList(Set<PersonView> views) {
		ArrayList<PersonView> list = new ArrayList<PersonView>();
		list.addAll(views);

		final Collator collator = Collator.getInstance();
		Collections.sort(list, new Comparator<PersonView>() {
			public int compare (PersonView v1, PersonView v2) {
				return collator.compare(v1.getHumanReadableName(), v2.getHumanReadableName());
			}
		});
		
		return list;
	}
}
