package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;

import com.dumbhippo.FullName;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class IdentitySpiderBean implements IdentitySpider, IdentitySpiderRemote {
	static private final Log logger = GlobalSetup.getLog(IdentitySpider.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@javax.annotation.Resource
	private EJBContext ejbContext;
	
	@EJB
	private AccountSystem accountSystem;
	
	public User lookupPersonByEmail(String email) {
		EmailResource res = getEmail(email);
		return lookupPersonByResource(res);
	}

	private static final String LOOKUP_PERSON_BY_RESOURCE_QUERY =
		"SELECT ac.owner FROM AccountClaim ac WHERE ac.resource = :resource";
	
	public User lookupPersonByResource(Resource resource) {
		try {
			return (User) em.createQuery(LOOKUP_PERSON_BY_RESOURCE_QUERY)
			.setParameter("resource", resource)
			.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
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
		
	public EmailResource getEmail(String email) {
		IdentitySpider proxy = (IdentitySpider) ejbContext.lookup(IdentitySpider.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateEmail(email);
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating email resource, retrying");
					retries--;
				} else {
					logger.error("Couldn't create email resource", e);						
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
	
	public AimResource getAim(String screenName) {
		IdentitySpider proxy = (IdentitySpider) ejbContext.lookup(IdentitySpider.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateAim(screenName);
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating aim resource, retrying");
					retries--;
				} else {
					logger.error("Couldn't create AIM resource", e);					
					throw new RuntimeException("Unexpected error creating aim resource", e);
				}
			}
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public AimResource findOrCreateAim(String screenName) {
		Query q;
		
		q = em.createQuery("from AimResource a where a.screenName = :name");
		q.setParameter("name", screenName);
		
		AimResource res;
		try {
			res = (AimResource) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			res = new AimResource(screenName);
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
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
					logger.debug("Race condition creating link resource, retrying");
					retries--;
				} else {
					logger.error("Couldn't create link resource", e);
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
	
	private transient User theMan;
	
	private static final String theManGuid = "8716baa63bef600797fbc59e06010000a35ad1637e6a7f87";
	private static final String theManEmail = "theman@dumbhippo.com";

	// This needs to be synchronized since the stateless session bean might be shared
	// between threads.
	public synchronized User getTheMan() {
		IdentitySpider proxy = (IdentitySpider) ejbContext.lookup(IdentitySpider.class.getCanonicalName());
		int retries = 1;
		
		while (theMan == null) {
			try {
				theMan = proxy.findOrCreateTheMan();
			} catch (Exception e) {
				if (retries > 0 && EJBUtil.isDuplicateException(e)) {
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
	public User findOrCreateTheMan() {
		User result;
		Guid guid;
		try {
			guid = new Guid(theManGuid);
		} catch (ParseException e1) {
			throw new RuntimeException("Guid could not parse theManGuid, should never happen", e1);
		}
		try {
			result = lookupGuid(User.class, guid);
		} catch (GuidNotFoundException e) {
			logger.debug("Creating theman@dumbhippo.com");
			EmailResource resource = getEmail(theManEmail);
			result = new User(guid);		
			em.persist(result);
			addVerifiedOwnershipClaim(result, resource);
		}
		
		return result;
	}
	
	private static final String GET_EMAIL_FOR_USER_QUERY = 
		"SELECT e FROM EmailResource e, AccountClaim ac " +
		    "WHERE e.id = ac.resource and ac.owner = :user ";
	
	private EmailResource getEmailForUser(User user) {
		try {
			return (EmailResource)em.createQuery(GET_EMAIL_FOR_USER_QUERY)
				.setParameter("user", user)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	private static final String GET_EMAIL_FOR_CONTACT_QUERY = 
		"SELECT e FROM EmailResource e, ContactClaim cc " +
		    "WHERE e.id = cc.resource and cc.contact = :contact ";
	
	private EmailResource getEmailForContact(Contact contact) {
		try {
			return (EmailResource)em.createQuery(GET_EMAIL_FOR_CONTACT_QUERY)
				.setParameter("contact", contact)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}		
	}
	
	private EmailResource getEmail(Viewpoint viewpoint, Person person) {
		if (person instanceof User)
			return getEmailForUser((User)person);
		else {
			return getEmailForContact((Contact)person);
		}
	}
	
	private static final String GET_USER_FOR_CONTACT_QUERY = 
		"SELECT a.owner FROM Account a, ContactClaim cc " +
		    "WHERE a = cc.resource and cc.contact = :contact ";
	
	private User getUserForContact(Contact contact) {
		try {
			return (User)em.createQuery(GET_USER_FOR_CONTACT_QUERY)
				.setParameter("contact", contact)
				.setMaxResults(1)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}		
	}
	
	public User getUser(Person person) {
		if (person instanceof User)
			return (User)person;
		else {
			User user = getUserForContact((Contact)person);
			
			logger.debug("getUserForContact: contact = " + person.getId() + "user = " + user);
			
			return user;
		}
	}
	
	private static final String GET_USER_FOR_RESOURCE_QUERY = 
		"SELECT ac.owner FROM AccountClaim ac WHERE ac.resource = :resource";
	
	public User getUser(Resource resource) {
		if (resource instanceof Account)
			return ((Account)resource).getOwner();
		else {
			try {
				return (User)em.createQuery(GET_USER_FOR_RESOURCE_QUERY)
					.setParameter("resource", resource)
					.getSingleResult();
			} catch (EntityNotFoundException e) {
				return null;
			}
		}
	}
	
	public PersonView getPersonView(Viewpoint viewpoint, Person p) {
		if (viewpoint == null)
			throw new IllegalArgumentException("null viewpoint");
		
		return new PersonView(p, getUser(p), getEmail(viewpoint, p));
	}

	public PersonView getPersonView(Viewpoint viewpoint, Resource r) {
		User user;
		Contact contact;
		EmailResource email = null;
		
		contact = findContactByResource(viewpoint.getViewer(), r);

		if (r instanceof Account) {
			user = ((Account)r).getOwner();
		} else {
			user = lookupPersonByResource(r);
		}
		
		if (r instanceof EmailResource)
			email = (EmailResource)r;
		else {
			if (contact != null)
				email = getEmailForContact(contact);
			if (email == null && user != null)
				email = getEmailForUser(user);
		}
			
		return new PersonView(contact != null ? contact : user, user, email); 
					          
	}

	public PersonView getSystemView(User user) {
		return new PersonView(user, user, getEmailForUser(user));
	}
	
	public void setName(Person person, FullName name) {
		person.setName(name);
		em.persist(person);
	}
	
	public void addVerifiedOwnershipClaim(User claimedOwner, Resource res) {
		AccountClaim ac = new AccountClaim(claimedOwner, res);
		em.persist(ac);
	}

	static final String FIND_CONTACT_BY_USER_QUERY =
		"SELECT cc.contact FROM Account contactAccount, ContactClaim cc " +
		"WHERE contactAccount.owner = :contactUser " +
		  "AND cc.account = :account AND cc.resource = contactAccount";
		
	private Contact findContactByUser(User owner, User contactUser) {
		try {
			return (Contact)em.createQuery(FIND_CONTACT_BY_USER_QUERY)
				.setParameter("account", owner.getAccount())
				.setParameter("contactUser", contactUser)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	static final String FIND_CONTACT_BY_RESOURCE_QUERY =
		"SELECT cc.contact FROM ContactClaim cc " +
		"WHERE cc.account = :account AND cc.resource = :resource";
		
	private Contact findContactByResource(User owner, Resource resource) {
		try {
			return (Contact)em.createQuery(FIND_CONTACT_BY_RESOURCE_QUERY)
				.setParameter("account", owner.getAccount())
				.setParameter("resource", resource)
				.getSingleResult();
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	private Contact doCreateContact(User user, Resource resource) {
		// FIXME: add to contact.getResources(). I'm not sure how
		// to do that without causing a database query, however.
		// I suppose we could have contact.addResource() that bypassed
		// the interception on getResources().
		
		Contact contact = new Contact(user.getAccount());
		em.persist(contact);
		
		ContactClaim cc = new ContactClaim(contact, resource);
		em.persist(cc);
		
		return contact;
	}
	
	public Contact createContact(User user, Resource resource) {
		if (user == null)
			throw new IllegalArgumentException("null contact owner");
		if (resource == null)
			throw new IllegalArgumentException("null contact resource");
		
		Contact contact = findContactByResource(user, resource);
		if (contact == null) {
			contact = doCreateContact(user, resource);
			
			// Things work better (especially for now, when we don't fully
			// implement spidering) if contacts own the account resource for
			// users.
			if (!(resource instanceof Account)) {
				logger.debug("Adding contact resource pointing to account");
				User contactUser = lookupPersonByResource(resource);
				
				ContactClaim cc = new ContactClaim(contact, contactUser.getAccount());
				em.persist(cc);
			}
		}
			
		
		return contact;
	}
	
	public void addContactPerson(User user, Person contactPerson) {
		logger.debug("adding contact " + contactPerson + " to account " + user.getAccount());
		
		if (contactPerson instanceof Contact) {
			// Must be a contact of user, so nothing to do
			assert ((Contact)contactPerson).getAccount() == user.getAccount();
		} else {
			User contactUser = (User)contactPerson;
			
			if (findContactByUser(user, contactUser) != null)
				return;
			
			doCreateContact(user, contactUser.getAccount());
		}
	}

	public void removeContactPerson(User user, Person contactPerson) {
		logger.debug("removing contact " + contactPerson + " from account " + user.getAccount());

		Contact contact;
		if (contactPerson instanceof Contact)
			contact = (Contact)contactPerson;
		else {
			contact = findContactByUser(user, (User)contactPerson);
			if (contact == null) // Nothing to do
				return;
		}

		em.remove(contact);		
	}

	public Set<Contact> getRawContacts(User user) {
		// We call lookupAccountByPerson to deal with possibly detached users
		Account account = accountSystem.lookupAccountByPerson(user);
		return account.getContacts();
	}
	
	public Set<PersonView> getContacts(Viewpoint viewpoint, User user) {
		if (!user.equals(viewpoint.getViewer()))
				return Collections.emptySet();
		
		Set<PersonView> result = new HashSet<PersonView>();
		for (Person p : getRawContacts(user)) 
			result.add(getPersonView(viewpoint, p));
		
		return result;
	}
	
	static final String IS_CONTACT_QUERY =
		"SELECT COUNT(cc.contact) FROM Account contactAccount, ContactClaim cc " +
		"WHERE contactAccount.owner = :contactUser " +
		  "AND cc.account = :account AND cc.resource = contactAccount";
		
	public boolean isContact(Viewpoint viewpoint, User user, Person contactPerson) {
		if (!user.equals(viewpoint.getViewer()))
				return false;
		
		if (contactPerson instanceof Contact) {
			// Must be a contact of user, so trivial
			assert ((Contact)contactPerson).getAccount() == user.getAccount();
			return true;
		} else {
			User contactUser = (User)contactPerson;
			
			// According to the spec, a count query should return a Long, Hibernate
			// returns an Integer. We use (Number) to be robust
			Number count = (Number)em.createQuery(IS_CONTACT_QUERY)
				.setParameter("contactUser", contactUser)
				.setParameter("account", user.getAccount())
				.getSingleResult();
			
			return count.longValue() > 0;
		}
	}
	
	static final String GET_CONTACT_RESOURCES_QUERY = 
		"SELECT cc.resource FROM ContactClaim cc WHERE cc.contact = :contact";
	
	private Resource getFirstContactResource(Contact contact) {
		// An invariant we retain in the database is that every contact
		// has at least one resource, so we don't need to check for 
		// EntityNotFoundException
		return (Resource)em.createQuery(GET_CONTACT_RESOURCES_QUERY)
			.setParameter("contact", contact)
			.setMaxResults(1)
			.getSingleResult();
	}
	
	public Resource getBestResource(Person person) {
		User user = getUser(person);
		if (user != null)
			return user.getAccount();
		
		return getFirstContactResource ((Contact)person);
	}
}
