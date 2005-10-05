package com.dumbhippo.server.impl;

import java.io.Serializable;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.PersonView;

@Stateful
public class PersonViewBean
	implements Serializable, PersonView {
	
	private static final long serialVersionUID = 0L;
	
	private Person viewpoint;
	private Person person;
	
	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	public PersonViewBean (Person viewpoint, Person person) {
		this.viewpoint = viewpoint;
		this.person = person;
	}
	
	private static final String BASE_LOOKUP_EMAIL_QUERY 
		= "select e from EmailResource e, ResourceOwnershipClaim c where e.id = c.resource and c.claimedOwner = :personid and (c.assertedBy.id is null ";
	
	/* (non-Javadoc)
	 * @see com.dumbhippo.persistence.PersonView#getEmail()
	 */
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

	/* (non-Javadoc)
	 * @see com.dumbhippo.persistence.PersonView#getHumanReadableName()
	 */
	public String getHumanReadableName() {
		EmailResource email = getEmail();
		if (email != null) {
			return email.getEmail();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.dumbhippo.persistence.PersonView#getPerson()
	 */
	public Person getPerson() {
		return person;
	}

	/* (non-Javadoc)
	 * @see com.dumbhippo.persistence.PersonView#getViewpoint()
	 */
	public Person getViewpoint() {
		return viewpoint;
	}
}
