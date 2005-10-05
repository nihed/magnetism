package com.dumbhippo.server;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.PersonView;
import com.dumbhippo.persistence.ResourceOwnershipClaim;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class IdentitySpiderBean implements IdentitySpider {
	
	private static final String BASE_LOOKUP_PERSON_EMAIL_QUERY = "select p from Person p, ResourceOwnershipClaim c where p.id = c.claimedOwner and c.resource = :email ";

	@PersistenceContext(unitName = "dumbhippo")
	protected EntityManager em;
	
	public Person lookupPersonByEmail(EmailResource email) {		
		return (Person) em.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and c.assertedBy is null").setParameter("email", email).getSingleResult();
	}
	
	public Person lookupPersonByEmail(Person viewpoint, EmailResource email) {		
		return (Person) em.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and (c.assertedBy.id = :viewpointguid or c.assertedBy.id is null)")
		.setParameter("viewpointguid", viewpoint.getId()).setParameter("email", email).getSingleResult();
	}
	
	public Person addPersonWithEmail(EmailResource email) {		
		Person p = new Person();
		em.persist(p);
		addOwnershipClaim(email, p);
		return p;
	}

	private void addOwnershipClaim(EmailResource email, Person p) {
		ResourceOwnershipClaim claim = new ResourceOwnershipClaim(p, email);
		em.persist(claim);
	}
	
	public EmailResource getEmail(String email) {
		Query q;
	
		q = em.createQuery("from EmailResource e where e.email = :email");
		q.setParameter("email", email);
		
		EmailResource res = (EmailResource) q.getSingleResult();
		if (res == null) {
			res = new EmailResource(email);
			em.persist(res);
		}
		
		return res;	
	}
	
	private static final String theManGuid = "8716baa63bef600797fbc59e06010000a35ad1637e6a7f87";
	private static final String theManEmail = "theman@dumbhippo.com";

	public Person getTheMan() {
		Person ret = (Person) em.createQuery("from Person p where p.id = :id").setParameter("id", theManGuid).getSingleResult();
		if (ret == null) {
			EmailResource res = getEmail(theManEmail);
			ret = new Person(new Guid(theManGuid));
			em.persist(ret);
			addOwnershipClaim(res, ret);
		}
		return ret;
	}

	public PersonView getViewpoint(Person viewpoint, Person p) {
		return new PersonView(viewpoint, p);
	}
}

