package com.dumbhippo.persistence;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

public class PersonView 
	implements Serializable {
	
	private static final long serialVersionUID = 0L;
	
	private Person viewpoint;
	private Person person;
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	public PersonView (Person viewpoint, Person person) {
		this.viewpoint = viewpoint;
		this.person = person;
	}
	
	private static final String BASE_LOOKUP_EMAIL_QUERY 
		= "select e from EmailResource e, ResourceOwnershipClaim c where e.id = c.resource and c.claimedOwner = :personid and (c.assertedBy.id is null ";
	
	public EmailResource getEmail() {
		EmailResource res;
		
		Query q;
		if (viewpoint == null) {
			q = em.createQuery(BASE_LOOKUP_EMAIL_QUERY + ")");
		} else {
			q = em.createQuery(BASE_LOOKUP_EMAIL_QUERY + "or c.assertedBy.id = :viewpointguid)").setParameter("viewpointguid", viewpoint.getId());
		}
		q.setParameter("personid", person.getId());
		res = (EmailResource) q.getSingleResult();

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
