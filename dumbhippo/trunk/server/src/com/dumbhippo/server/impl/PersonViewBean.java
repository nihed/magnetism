package com.dumbhippo.server.impl;

import java.io.Serializable;

import javax.annotation.EJB;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.FullName;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewRemote;



/**
 * 
 * Stateful beans have a lifecycle equal to the life of 
 * the session, unless we add an @Remove-annotated method in which 
 * case that method kills the bean.
 * 
 * @author hp
 *
 */
@Stateful
public class PersonViewBean
	implements Serializable, PersonView, PersonViewRemote {
	
	private static final long serialVersionUID = 0L;
	
	private Person viewpoint;
	private Person person;
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private IdentitySpider spider;
	
	public void init(Person viewpoint, Person person) {
		this.viewpoint = viewpoint;
		this.person = person;
	}
	
	private static final String BASE_LOOKUP_EMAIL_QUERY 
		= "select e from EmailResource e, ResourceOwnershipClaim c where e.id = c.resource and c.claimedOwner = :personid and (c.assertedBy = :theman ";
	
	/* (non-Javadoc)
	 * @see com.dumbhippo.persistence.PersonView#getEmail()
	 */
	public EmailResource getEmail() {	
		Query q;
		if (viewpoint == null) {
			q = em.createQuery(BASE_LOOKUP_EMAIL_QUERY + ")");
		} else {
			q = em.createQuery(BASE_LOOKUP_EMAIL_QUERY + "or c.assertedBy = :viewpoint)")
			.setParameter("viewpoint", viewpoint);
		}
		q.setParameter("theman", spider.getTheMan());
		q.setParameter("personid", person.getId());
		
		try {
			return (EmailResource) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see com.dumbhippo.persistence.PersonView#getHumanReadableName()
	 */
	public String getHumanReadableName() {
		FullName name = person.getName();
		if (name != null && !name.isEmpty()) {
			return name.toString();
		}
		EmailResource email = getEmail();
		if (email != null) 
			return email.getEmail();
		
		return "<Unknown>";
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
