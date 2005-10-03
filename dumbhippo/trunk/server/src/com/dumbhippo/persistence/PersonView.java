package com.dumbhippo.persistence;

import org.hibernate.Query;
import org.hibernate.Session;


public class PersonView {
	private Person viewpoint;
	private Person person;
	
	public PersonView (Person viewpoint, Person person) {
		this.viewpoint = viewpoint;
		this.person = person;
	}
	
	private static final String BASE_LOOKUP_EMAIL_QUERY 
		= "select e from EmailResource e, ResourceOwnershipClaim c where e.id = c.resource and c.claimedOwner = :personid and (c.assertedBy.id is null ";
	
	public EmailResource getEmail() {
		EmailResource res;
		Session hsession = Storage.getGlobalPerThreadSession().getSession();
		Query q;
		if (viewpoint == null) {
			q = hsession.createQuery(BASE_LOOKUP_EMAIL_QUERY + ")");
		} else {
			q = hsession.createQuery(BASE_LOOKUP_EMAIL_QUERY + "or c.assertedBy.id = :viewpointguid)").setString("viewpointguid", viewpoint.getId());
		}
		q.setParameter("personid", person.getId());
		res = (EmailResource) q.uniqueResult();

		return res;		
	}

	public String getHumanReadableName() {
		EmailResource email = getEmail();
		if (email != null) {
			return email.getEmail();
		}
		return null;
	}
	
	public Person getPerson() {
		return person;
	}

	public Person getViewpoint() {
		return viewpoint;
	}
}
