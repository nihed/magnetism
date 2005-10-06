package com.dumbhippo.server.impl;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.dumbhippo.FullName;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceOwnershipClaim;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.PersonView;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class IdentitySpiderBean implements IdentitySpider, IdentitySpiderRemote {
	
	private static final String BASE_LOOKUP_PERSON_EMAIL_QUERY = "select p from Person p, ResourceOwnershipClaim c where p.id = c.claimedOwner and c.resource = :email ";

	@PersistenceContext(unitName = "dumbhippo")
	private transient EntityManager em;
	
	public Person lookupPersonByEmail(EmailResource email) {		
		return (Person) em.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and c.assertedBy is null").setParameter("email", email).getSingleResult();
	}
	
	public Person lookupPersonByEmail(Person viewpoint, EmailResource email) {		
		return (Person) em.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and (c.assertedBy.id = :viewpointguid or c.assertedBy.id is null)")
		.setParameter("viewpointguid", viewpoint.getId()).setParameter("email", email).getSingleResult();
	}

	private void addProvenOwnershipClaim(Person claimedOwner, EmailResource email) {
		ResourceOwnershipClaim claim = new ResourceOwnershipClaim(claimedOwner, email, getTheMan());
		em.persist(claim);
	}
	
	public EmailResource getEmail(String email) {
		Query q;
	
		q = em.createQuery("from EmailResource e where e.email = :email");
		q.setParameter("email", email);
		
		EmailResource res;
		try {
			res = (EmailResource) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			res = new EmailResource(email);
			em.persist(res);
		}
		
		return res;	
	}
	
	private static final String theManGuid = "8716baa63bef600797fbc59e06010000a35ad1637e6a7f87";
	private static final String theManEmail = "theman@dumbhippo.com";

	public Person getTheMan() {
		Person ret;
		
		try {
			ret = (Person) em.createQuery("from Person p where p.id = :id").setParameter("id", theManGuid).getSingleResult();
		} catch (EntityNotFoundException e) {
			EmailResource res = getEmail(theManEmail);
			ret = new Person(new Guid(theManGuid));
			em.persist(ret);
			ResourceOwnershipClaim claim = new ResourceOwnershipClaim(ret, res, ret);
			em.persist(claim);
		}
		return ret;
	}

	public PersonView getViewpoint(Person viewpoint, Person p) {
		return new PersonViewBean(viewpoint, p);
	}

	public PersonView getSystemViewpoint(Person p) {
		return new PersonViewBean(null, p);
	}

	public HippoAccount lookupAccountByPerson(Person person) {
		// TODO Auto-generated method stub
		return null;
	}

	public HippoAccount lookupAccountByUsername(String username) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void setName(Person person, FullName name) {
		person.setName(name);
		em.persist(person);
	}
	
	/* FIXME this is fucked up. Basically you need to be able to only 
	 * do assertedBy == current authorized user. This whole interface
	 * is screwy.
	 * 
	 * (non-Javadoc)
	 * @see com.dumbhippo.server.IdentitySpider#addOwnershipClaim(com.dumbhippo.persistence.Person, com.dumbhippo.persistence.Resource, com.dumbhippo.persistence.Person)
	 */
	public void addOwnershipClaim(Person owner, Resource resource, Person assertedBy) {
		if (assertedBy == null || assertedBy.equals(getTheMan())) {
			throw new IllegalArgumentException("Can't add this ownership claim");
		}
		ResourceOwnershipClaim claim = new ResourceOwnershipClaim(owner, resource, assertedBy);
		em.persist(claim);
	}
}

