package com.dumbhippo.server.impl;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.exception.ConstraintViolationException;

import com.dumbhippo.FullName;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.ResourceOwnershipClaim;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.IdentitySpider.GuidNotFoundException;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class IdentitySpiderBean implements IdentitySpider, IdentitySpiderRemote {
	static private final Log logger = GlobalSetup.getLog(IdentitySpider.class);
	
	private static final String BASE_LOOKUP_PERSON_EMAIL_QUERY = "select p from Person p, ResourceOwnershipClaim c where p.id = c.claimedOwner and c.resource = :email ";

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	public Person lookupPersonByEmail(EmailResource email) {
		Person person;
		try {
			person = (Person) em.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and c.assertedBy = :theman")
			.setParameter("email", email)
			.setParameter("theman", getTheMan())
			.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
		return person;
	}
	
	public Person lookupPersonByEmail(Person viewpoint, EmailResource email) {
		Person person;
		try {
			// FIXME: this query could return multiple results if there is both a 
			//   personal ownership assertion and a system-wide ownership assertion.
			//   It might be better to just do two queries.
			person = (Person) em.createQuery(BASE_LOOKUP_PERSON_EMAIL_QUERY + "and (c.assertedBy = :viewpoint or c.assertedBy = :theman)")
			.setParameter("email", email)
			.setParameter("viewpoint", viewpoint)
			.setParameter("theman", getTheMan())
			.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
		return person;
	}

	public <T extends GuidPersistable> T lookupGuidString(Class<T> klass, String id) throws ParseException, GuidNotFoundException {
		Guid.validate(id); // so we throw Parse instead of GuidNotFound if invalid
		T obj = em.find(klass, id);
		if (obj == null)
			throw new GuidNotFoundException(id);
		return obj;
	}

	public <T extends GuidPersistable> T lookupGuid(Class<T> klass, Guid id) throws GuidNotFoundException {
		T obj = em.find(klass, id.toString());
		if (obj == null)
			throw new GuidNotFoundException(id);
		return obj;
	}

	public <T extends GuidPersistable> Set<T> lookupGuidStrings(Class<T> klass, Set<String> ids) throws ParseException, GuidNotFoundException {
		Set<T> ret = new HashSet<T>();
		for (String s : ids) {
			T obj = lookupGuidString(klass, s);
			ret.add(obj);
		}
		return ret;
	}

	public <T extends GuidPersistable> Set<T> lookupGuids(Class<T> klass, Set<Guid> ids) throws GuidNotFoundException {
		Set<T> ret = new HashSet<T>();
		for (Guid id : ids) {
			T obj = lookupGuid(klass, id);
			ret.add(obj);
		}
		return ret;
	}
	
	// Returns true if this is an exception we would get with a race condition
	// between two people trying to create the same object at once
	private boolean isDuplicateException(Exception e) {
		return ((e instanceof EJBException &&
				 ((EJBException)e).getCausedByException() instanceof ConstraintViolationException) ||
	            e instanceof NonUniqueObjectException);
	}
	
	public EmailResource getEmail(String email) {
		IdentitySpider proxy = (IdentitySpider) ejbContext.lookup(IdentitySpider.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateEmail(email);
			} catch (Exception e) {
				if (retries > 0 && isDuplicateException(e)) {
					logger.debug("Race condition creating email resource, retrying");
					retries--;
				} else {
					throw new RuntimeException("Unexpected error creating email resource", e);
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public EmailResource findOrCreateEmail(String email) {
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
	
	public LinkResource getLink(String link) {
		IdentitySpider proxy = (IdentitySpider) ejbContext.lookup(IdentitySpider.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateLink(link);
			} catch (Exception e) {
				if (retries > 0 && isDuplicateException(e)) {
					logger.debug("Race condition creating link resource, retrying");
					retries--;
				} else {
					throw new RuntimeException("Unexpected error creating link resource", e);
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public LinkResource findOrCreateLink(String url) {
		Query q;
	
		q = em.createQuery("from LinkResource l where l.url = :url");
		q.setParameter("url", url);
		
		LinkResource res;
		try {
			res = (LinkResource) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			res = new LinkResource(url);
			em.persist(res);
		}
		
		return res;	
	}
	
	private transient Person theMan;
	
	private static final String theManGuid = "8716baa63bef600797fbc59e06010000a35ad1637e6a7f87";
	private static final String theManEmail = "theman@dumbhippo.com";

	// This needs to be synchronized since the stateless session bean might be shared
	// between threads.
	public synchronized Person getTheMan() {
		IdentitySpider proxy = (IdentitySpider) ejbContext.lookup(IdentitySpider.class.getCanonicalName());
		int retries = 1;
		
		while (theMan == null) {
			try {
				theMan = proxy.findOrCreateTheMan();
			} catch (Exception e) {
				if (retries > 0 && isDuplicateException(e)) {
					logger.debug("Race condition creating theMan, retrying");
					retries--;
				} else {
					throw new RuntimeException("Unexpected error looking up theMan", e);
				}
			}
		}
		return theMan;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public Person findOrCreateTheMan() {
		Person result;
		Guid guid;
		try {
			guid = new Guid(theManGuid);
		} catch (ParseException e1) {
			throw new RuntimeException("Guid could not parse theManGuid, should never happen", e1);
		}
		try {
			result = lookupGuid(Person.class, guid);
		} catch (GuidNotFoundException e) {
			logger.debug("Creating theman@dumbhippo.com");
			EmailResource resource = getEmail(theManEmail);
			result = new Person(guid);		
			em.persist(result);
			ResourceOwnershipClaim claim = new ResourceOwnershipClaim(result, resource, result);
			em.persist(claim);
		}
		
		return result;
	}
	
	private PersonView constructPersonView(Person viewpoint, Person p) {
		PersonView view = (PersonView) ejbContext.lookup(PersonView.class.getCanonicalName());
		view.init(viewpoint, p);
		return view;
	}
	
	public PersonView getViewpoint(Person viewpoint, Person p) {
		return constructPersonView(viewpoint, p);
	}

	public PersonView getSystemViewpoint(Person p) {
		return constructPersonView(getTheMan(), p);
	}
	
	public void setName(Person person, FullName name) {
		person.setName(name);
		em.persist(person);
	}
	
	private void internalAddOwnershipClaim(Person owner, Resource resource, Person assertedBy) {
		ResourceOwnershipClaim claim = new ResourceOwnershipClaim(owner, resource, assertedBy);
		em.persist(claim);		
	}
	
	/* FIXME You need to be able to only 
	 * do assertedBy == current authorized user. This whole interface
	 * is screwy.
	 */
	public void addOwnershipClaim(Person owner, Resource resource, Person assertedBy) {
		if (assertedBy.getId() == getTheMan().getId()) {
			throw new IllegalArgumentException("Can't add this ownership claim");
		}
		internalAddOwnershipClaim(owner, resource, assertedBy);
	}
	
	public void addVerifiedOwnershipClaim(Person claimedOwner, Resource res) {
		internalAddOwnershipClaim(claimedOwner, res, getTheMan());
	}

	public Person createContact(HippoAccount owner, Resource contact) {
		Person ret;
		
		ret = new Person();
		em.persist(ret);
		addOwnershipClaim(ret, contact, owner.getOwner());
		owner.addContact(ret);
		return ret;
	}
}

