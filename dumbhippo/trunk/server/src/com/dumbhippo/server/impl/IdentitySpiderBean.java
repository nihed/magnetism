package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeFilteredCollection;
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
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
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
	
	@EJB
	private InvitationSystem invitationSystem;
	
	public User lookupUserByEmail(String email) {
		EmailResource res = getEmail(email);
		return lookupUserByResource(res);
	}

	private static final String LOOKUP_PERSON_BY_RESOURCE_QUERY =
		"SELECT ac.owner FROM AccountClaim ac WHERE ac.resource = :resource";
	
	public User lookupUserByResource(Resource resource) {
		if (resource instanceof Account)
			return ((Account)resource).getOwner();
		
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
	
	public AimResource getAim(String screenName) throws ValidationException {
		IdentitySpider proxy = (IdentitySpider) ejbContext.lookup(IdentitySpider.class.getCanonicalName());
		int retries = 1;
		
		while (true) {
			try {
				return proxy.findOrCreateAim(screenName);
			} catch (ValidationException e) {
				throw e;
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
	public AimResource findOrCreateAim(String screenName) throws ValidationException {
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

	public AimResource lookupAim(String screenName) throws ValidationException {
		Query q;
		
		q = em.createQuery("from AimResource a where a.screenName = :name");
		q.setParameter("name", screenName);
		
		AimResource res = null;
		try {
			res = (AimResource) q.getSingleResult();
		} catch (EntityNotFoundException e) {
			;
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
	
	private static final String theManNickname = "The Man";
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
			result.setNickname(theManNickname);
			em.persist(result);
			addVerifiedOwnershipClaim(result, resource);
		}
		
		return result;
	}
	
	private static final String GET_RESOURCES_FOR_USER_QUERY = 
		"SELECT r FROM Resource r, AccountClaim ac " +
		    "WHERE r.id = ac.resource and ac.owner = :person ";
	private static final String GET_RESOURCES_FOR_CONTACT_QUERY = 
		"SELECT r FROM Resource r, ContactClaim cc " +
		    "WHERE r.id = cc.resource and cc.contact = :person ";
	
	private Set<Resource> getResourcesForPerson(Person person) {
		Set<Resource> resources = new HashSet<Resource>();
		try {
			String query;
			if (person instanceof User) {
				query = GET_RESOURCES_FOR_USER_QUERY;
			} else if (person instanceof Contact){
				query = GET_RESOURCES_FOR_CONTACT_QUERY;
			} else {
				throw new IllegalArgumentException("person is not User or Contact: " + person);
			}
			
			List results = em.createQuery(query).setParameter("person", person).getResultList();
			
			for (Object r : results) {
				if (r instanceof EmailResource)
					resources.add((Resource) r);
				else if (r instanceof AimResource)
					resources.add((Resource) r);
				// we filter out any non-email/aim resources for now
			}
		} catch (EntityNotFoundException e) {
			// this should not happen I don't think ... but I'm not 
			// quite sure enough to throw an exception so we just return an empty set
			logger.error("No resources for person " + person, e);
		}
		
		return resources;		
	}
	
	// this can return multiple accounts, one for each resource 
	// associated with the contact; in that case we pick an account
	// arbitrarily
	private static final String GET_USER_FOR_CONTACT_QUERY = 
		"SELECT ac.owner FROM ContactClaim cc, AccountClaim ac " +
		    "WHERE cc.contact = :contact AND ac.resource = cc.resource";
	
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
			logger.debug("getUser: contact = " + person);

			User user = getUserForContact((Contact)person);
			
			logger.debug("getUserForContact: user = " + user);
			
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
	
	private void addPersonViewExtras(Viewpoint viewpoint, PersonView pv, Resource fromResource, PersonViewExtra... extras) {
		
		// we implement this in kind of a lame way right now where we always do 
		// all the database work, even though we only return the requested information to keep 
		// other code honest
		
		Set<Resource> contactResources = null;
		Set<Resource> userResources = null;
		Set<Resource> resources = null;
		
		if (pv.getContact() != null)
			contactResources = getResourcesForPerson(pv.getContact());
		if (pv.getUser() != null)
			userResources = getResourcesForPerson(pv.getUser());
		if (contactResources != null) {
			resources = contactResources;
			if (userResources != null)
				resources.addAll(userResources); // contactResources won't be overwritten in case of conflict
		} else if (userResources != null) {
			resources = userResources;
		} else {
			if (fromResource != null)
				resources = Collections.singleton(fromResource);
			else
				resources = Collections.emptySet();
		}
		
		// this does extra work right now (adding some things more than once)
		// but just not worth the complexity to avoid since we'll probably change
		// all this anyway and most callers won't be silly (won't ask for both ALL_ and 
		// PRIMARY_ for example)
		for (PersonViewExtra e : extras) {
			if (e == PersonViewExtra.INVITED_STATUS) {
				if (viewpoint == null) {
					// in this case we're doing a system view, invited
					// flag really doesn't make sense ...
					pv.addInvitedStatus(true);
				} else if (pv.getUser() != null) {
					pv.addInvitedStatus(true); // they already have an account
				} else {
					boolean invited = false;
					for (Resource r : resources) {
						invited = invitationSystem.hasInvited(viewpoint.getViewer(), r);
					}
					pv.addInvitedStatus(invited);
				}
			} else if (e == PersonViewExtra.ALL_RESOURCES) {
				pv.addAllResources(resources);
			} else if (e == PersonViewExtra.ALL_EMAILS) {
				pv.addAllEmails(new TypeFilteredCollection<Resource,EmailResource>(resources, EmailResource.class));
			} else if (e == PersonViewExtra.ALL_AIMS) {
				pv.addAllAims(new TypeFilteredCollection<Resource,AimResource>(resources, AimResource.class));
			} else {
				EmailResource email = null;
				AimResource aim = null;
				
				for (Resource r : resources) {
					if (email == null && r instanceof EmailResource) {
						email = (EmailResource) r;
					} else if (aim == null && r instanceof AimResource) {
						aim = (AimResource) r;
					} else if (email != null && aim != null) {
						break;
					}
				}
				
				if (e == PersonViewExtra.PRIMARY_RESOURCE) {
					if (email != null)
						pv.addPrimaryResource(email);
					else if (aim != null)
						pv.addPrimaryResource(aim);
					else {
						pv.addPrimaryResource(null);
					}
				} else if (e == PersonViewExtra.PRIMARY_EMAIL) {
					pv.addPrimaryEmail(email); // can be null
				} else if (e == PersonViewExtra.PRIMARY_AIM) {
					pv.addPrimaryAim(aim); // can be null
				}
			}
		}
	}
	
	public PersonView getPersonView(Viewpoint viewpoint, Person p, PersonViewExtra... extras) {
		if (viewpoint == null)
			throw new IllegalArgumentException("null viewpoint");
		
		Contact contact = p instanceof Contact ? (Contact) p : null;
		User user = getUser(p); // user for contact, or p itself if it's already a user
		
		PersonView pv = new PersonView(contact, user);
		
		// FIXME we need to filter this - resources from the viewed User 
		// should not be offered if viewpoint is not a contact of user
		addPersonViewExtras(viewpoint, pv, null, extras);
		
		return pv;
	}

	public PersonView getPersonView(Viewpoint viewpoint, Resource r, PersonViewExtra firstExtra, PersonViewExtra... extras) {
		User user;
		Contact contact;
		
		contact = findContactByResource(viewpoint.getViewer(), r);

		if (r instanceof Account) {
			user = ((Account)r).getOwner();
		} else {
			user = lookupUserByResource(r);
		}
		
		PersonView pv = new PersonView(contact, user);
		
		PersonViewExtra allExtras[] = new PersonViewExtra[extras.length + 1];
		allExtras[0] = firstExtra;
		for (int i = 0; i < extras.length ; ++i) {
			allExtras[i+1] = extras[i];
		}
		addPersonViewExtras(viewpoint, pv, r, allExtras);
		
		return pv;
	}

	public PersonView getSystemView(User user, PersonViewExtra... extras) {
		PersonView pv = new PersonView(null, user);
		addPersonViewExtras(null, pv, null, extras);
		return pv;
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
		// call the contact whatever resource we used to create it
		// Don't use resource.getDerivedNickname() here, because 
		// only the account holder who typed in the email address will 
		// see it, so no need to munge it - we want what they typed in
		contact.setNickname(resource.getHumanReadableString());
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
				User contactUser = lookupUserByResource(resource);
				
				if (contactUser != null) {
					logger.debug("Adding contact resource pointing to account");
					ContactClaim cc = new ContactClaim(contact, contactUser.getAccount());
					em.persist(cc);
				}
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
		Account account = accountSystem.lookupAccountByUser(user);
		return account.getContacts();
	}
	
	public Set<PersonView> getContacts(Viewpoint viewpoint, User user, boolean includeSelf, PersonViewExtra... extras) {
		if (!user.equals(viewpoint.getViewer()))
				return Collections.emptySet();
		
		// there are various ways to get yourself in your own contact list;
		// we make includeSelf work in both cases (where you are or are not in there)
		
		boolean sawSelf = false;
		Set<PersonView> result = new HashSet<PersonView>();
		for (Person p : getRawContacts(user)) {
			PersonView pv = getPersonView(viewpoint, p, extras);
			
			if (pv.getUser() != null && pv.getUser().equals(user)) {
				// FIXME the concept here (one contact displayed per User) could 
				// be generalized, i.e. we should probably nuke all but one PersonView
				// for each User...
				if (includeSelf && !sawSelf)
					result.add(pv);
				sawSelf = true;
			} else {
				result.add(pv);
			}
		}
		
		if (includeSelf && !sawSelf) {
			result.add(getPersonView(viewpoint, user, extras));
		}
		
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

	public boolean getAccountDisabled(User user) {
		Account account = user.getAccount();
		if (account == null) // must have been detached
			account = accountSystem.lookupAccountByUser(user);
		return account.isDisabled();
	}
	
	public void setAccountDisabled(User user, boolean disabled) {
		Account account = user.getAccount();
		if (account == null || !em.contains(account))
			account = accountSystem.lookupAccountByUser(user); // get attached account
		logger.debug("New disabled = " + disabled + " for account " + account);
		account.setDisabled(disabled);
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)	
	public int incrementUserVersion(String userId) {
//		While it isn't a big deal in practice, the implementation below is slightly
//		racy. The following would be better, but triggers a hibernate bug.
//
//		em.createQuery("UPDATE User u set u.version = u.version + 1 WHERE u.id = :id")
//		.setParameter("id", userId)
//		.executeUpdate();
//		
//		return em.find(User.class, userId).getVersion();
		User user = em.find(User.class, userId);
		int newVersion = user.getVersion() + 1;

		user.setVersion(newVersion);
		
		return newVersion;
	}	
}
