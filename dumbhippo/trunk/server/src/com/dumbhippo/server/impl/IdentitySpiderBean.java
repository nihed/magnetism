package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeFilteredCollection;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Administrator;
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
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.MySpaceTracker;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.Viewpoint;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class IdentitySpiderBean implements IdentitySpider, IdentitySpiderRemote {
	static private final Logger logger = GlobalSetup.getLogger(IdentitySpider.class);
	
	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;
	
	@EJB
	private TransactionRunner runner;
	
	@EJB
	private AccountSystem accountSystem;
	
	@EJB
	private InvitationSystem invitationSystem;
	
	@EJB
	private MessageSender messageSender;
	
	@EJB
	private MySpaceTracker mySpaceTracker;
	
	private static class GuidNotFoundException extends NotFoundException {
		private static final long serialVersionUID = 0L;
		private String guid;
		public GuidNotFoundException(Guid guid) {
			super("Guid " + guid + " was not in the database");
			this.guid = guid.toString();
		}
		public GuidNotFoundException(String guidString) {
			super("Guid " + guidString + " was not in the database");
			this.guid = guidString;
		}
		public String getGuid() {
			return guid;
		}
	}
	
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

	public User lookupUser(LiveUser luser) {
		return em.find(User.class, luser.getGuid().toString());
	}	
	
	public <T extends GuidPersistable> T lookupGuidString(Class<T> klass, String id) throws ParseException, NotFoundException {
		Guid.validate(id); // so we throw Parse instead of GuidNotFound if invalid
		T obj = em.find(klass, id);
		if (obj == null)
			throw new GuidNotFoundException(id);
		return obj;
	}

	public <T extends GuidPersistable> T lookupGuid(Class<T> klass, Guid id) throws NotFoundException {
		T obj = em.find(klass, id.toString());
		if (obj == null)
			throw new GuidNotFoundException(id);
		return obj;
	}

	public <T extends GuidPersistable> Set<T> lookupGuidStrings(Class<T> klass, Set<String> ids) throws ParseException, NotFoundException {
		Set<T> ret = new HashSet<T>();
		for (String s : ids) {
			T obj = lookupGuidString(klass, s);
			ret.add(obj);
		}
		return ret;
	}

	public <T extends GuidPersistable> Set<T> lookupGuids(Class<T> klass, Set<Guid> ids) throws NotFoundException {
		Set<T> ret = new HashSet<T>();
		for (Guid id : ids) {
			T obj = lookupGuid(klass, id);
			ret.add(obj);
		}
		return ret;
	}
		
	public EmailResource getEmail(final String email) {
		EmailResource ret;
		try {
			ret = runner.runTaskRetryingOnConstraintViolation(new Callable<EmailResource>() {
				
				public EmailResource call() throws Exception {
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
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
		
		return ret;
	}
	
	public AimResource getAim(final String screenName) throws ValidationException {
		try {
			AimResource ret = runner.runTaskRetryingOnConstraintViolation(new Callable<AimResource>() {
				public AimResource call() {
					Query q;
					
					q = em.createQuery("from AimResource a where a.screenName = :name");
					q.setParameter("name", screenName);
					
					AimResource res;
					try {
						res = (AimResource) q.getSingleResult();
					} catch (EntityNotFoundException e) {
						try {
							res = new AimResource(screenName);
						} catch (ValidationException v) {
							throw new RuntimeException(v);
						}
						em.persist(res);
					}
					
					return res;			
				}
			});
			
			return ret;
		} catch (ValidationException e) {
			throw e;
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}	

	private <T extends Resource> T lookupResourceByName(Class<T> klass, String identifier, String name) {
		Query q;
		String className = klass.getName();
		
		q = em.createQuery("from " + className + " a where a." + identifier + " = :name");
		q.setParameter("name", name);
		
		T res = null;
		try {		
			res = klass.cast(q.getSingleResult());
		} catch (EntityNotFoundException e) {
			;
		}
		return res;
	}
	
	public AimResource lookupAim(String screenName) {
		return lookupResourceByName(AimResource.class, "screenName", screenName);
	}
	
	public LinkResource getLink(final String link) {
		try {
			LinkResource ret = runner.runTaskRetryingOnConstraintViolation(new Callable<LinkResource>() {
	
				public LinkResource call() throws Exception {
					Query q;
					
					q = em.createQuery("from LinkResource l where l.url = :url");
					q.setParameter("url", link);
					
					LinkResource res;
					try {
						res = (LinkResource) q.getSingleResult();
					} catch (EntityNotFoundException e) {
						res = new LinkResource(link);
						em.persist(res);
					}
					
					return res;					
				}
				
			});
			return ret;
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}

	public User getCharacter(final Character whichOne) {
		try {
			return runner.runTaskRetryingOnConstraintViolation(new Callable<User>() {
				public User call() {
					EmailResource email = getEmail(whichOne.getEmail());
					User user = getUser(email);
					if (user == null) {
						// don't add any special handling in here - it should be OK if 
						// someone just creates the character accounts manually without running
						// this code. We don't want to start doing "if (character) ; else ;" all
						// over the place.
						logger.info("Creating special user " + whichOne);
						Account account = accountSystem.createAccountFromResource(email);
						user = account.getOwner();
						user.setNickname(whichOne.getDefaultNickname());
					}
					return user;
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}
	
	private static final String GET_RESOURCES_FOR_USER_QUERY = 
		"SELECT r FROM Resource r, AccountClaim ac " +
		    "WHERE r.id = ac.resource and ac.owner = :person ";
	private static final String GET_RESOURCES_FOR_CONTACT_QUERY = 
		"SELECT r FROM Resource r, ContactClaim cc " +
		    "WHERE r.id = cc.resource and cc.contact = :person ";
	
	private Set<Resource> getResourcesForPerson(Person person) {
		Set<Resource> resources = new HashSet<Resource>();
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
			// we filter out any non-"primary" resources for now
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
			//logger.debug("getUser: contact = {}", person);

			User user = getUserForContact((Contact)person);
			
			//logger.debug("getUserForContact: user = {}", user);
			
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
		if (pv.getContact() != null) {
			Contact contact = pv.getContact();
			contactResources = getResourcesForPerson(contact);
			
			//logger.debug("Contact has owner {} viewpoint is {}", contact.getAccount().getOwner(),
			//		viewpoint != null ? viewpoint.getViewer() : null);
			
			// can only see resources if we're the system view or this is our own
			// Contact
			boolean ownContact = viewpoint == null ||
				contact.getAccount().getOwner().equals(viewpoint.getViewer());

			if (!ownContact) {
				// we want to set a fallback name from an email resource if we 
				// have one, since we won't disclose the resources and PersonView
				// ideally doesn't show as <Unknown> in the UI
				Guid emailGuid = null; // we want to prefer email as the identifying guid
				Guid anyGuid = null;
				for (Resource r : contactResources) {
					if (r instanceof EmailResource) {
						pv.setFallbackName(r.getDerivedNickname());
						emailGuid = r.getGuid();
					} else {
						anyGuid = r.getGuid();
					}
				}
				if (emailGuid != null)
					pv.setFallbackIdentifyingGuid(emailGuid);
				else if (anyGuid != null)
					pv.setFallbackIdentifyingGuid(anyGuid);
				
				// don't disclose
				contactResources = null;
			}
		}
		
		// can only get user resources if we are a contact of the user
		if (pv.getUser() != null && isViewerFriendOf(viewpoint, pv.getUser())) {
			userResources = getResourcesForPerson(pv.getUser());
		}
		
		// If it's not our own contact, contactResources should be null here
		if (contactResources != null) {
			resources = contactResources;
			if (userResources != null) {
				resources.addAll(userResources); // contactResources won't be overwritten in case of conflict
			}
		} else if (userResources != null) {
			resources = userResources;
		} else {
			if (fromResource != null) {
				resources = Collections.singleton(fromResource);
			} else {
				resources = Collections.emptySet();
			}
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

		// FIXME we don't want contacts to have nicknames, so leave it null for 
		// now, but eventually we should change the db schema 
		//contact.setNickname(resource.getHumanReadableString());
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
					ContactClaim cc = new ContactClaim(contact, contactUser.getAccount());
					em.persist(cc);
					logger.debug("Added contact resource {} pointing to account {}",
							cc.getContact(), cc.getAccount());
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
		logger.debug("removing contact {} from account {}", contactPerson, user.getAccount());

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

	public Set<Contact> getRawContacts(Viewpoint viewpoint, User user) {
		if (!isViewerFriendOf(viewpoint, user))
			return Collections.emptySet();

		// We call lookupAccountByPerson to deal with possibly detached users
		Account account = accountSystem.lookupAccountByUser(user);
		return account.getContacts();
	}
	
	public Set<User> getRawUserContacts(Viewpoint viewpoint, User user) {
		Set<Contact> contacts = getRawContacts(viewpoint, user);
		Set<User> ret = new HashSet<User>();
		for (Contact c : contacts) {
			User u = getUserForContact(c);
			if (u != null)
				ret.add(u);
		}
		return ret;
	}
	
	public Set<PersonView> getContacts(Viewpoint viewpoint, User user, boolean includeSelf, PersonViewExtra... extras) {
		
		// there are various ways to get yourself in your own contact list;
		// we make includeSelf work in both cases (where you are or are not in there)
		
		boolean sawSelf = false;
		Set<PersonView> result = new HashSet<PersonView>();
		for (Person p : getRawContacts(viewpoint, user)) {
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
		"SELECT COUNT(cc.contact) FROM Account contactAccount, AccountClaim ac, ContactClaim cc " +
		"WHERE contactAccount.owner = :contactUser " +
		"AND ac.owner = :contactUser " +
		"AND cc.account = :contactOfAccount " + 
		"AND cc.resource = ac.resource";
	
	private boolean isContactNoViewpoint(User user, User contactUser) {
		// According to the spec, a count query should return a Long, Hibernate
		// returns an Integer. We use (Number) to be robust
		try {
			Number count = (Number)em.createQuery(IS_CONTACT_QUERY)
				.setParameter("contactUser", contactUser)
				.setParameter("contactOfAccount", user.getAccount())
				.getSingleResult();
			
			return count.longValue() > 0;
		} catch (EntityNotFoundException e) {
			throw new RuntimeException("count query should never fail in isContactNoViewpoint", e);
		}
	}
	
	public boolean isContact(Viewpoint viewpoint, User user, User contactUser) {
			// see if we're allowed to look at who user's contacts are
			if (!isViewerFriendOf(viewpoint, user))
				return false;

			// if we can see their contacts, return whether this person is one of them
			return isContactNoViewpoint(user, contactUser);
	}
	
	public boolean isViewerFriendOf(Viewpoint viewpoint, User user) {
		// You can see someone's "friends only" stuff if you are them, or you are a contact of them
		// viewpoint == null means omniscient
		if (viewpoint == null)
			return true;
		if (user.equals(viewpoint.getViewer()))
			return true;
		if (viewpoint.isFriendOfStatusCached(user))
			return viewpoint.getCachedFriendOfStatus(user);
		
		boolean isFriendOf = isContactNoViewpoint(user, viewpoint.getViewer());
		viewpoint.setCachedFriendOfStatus(user, isFriendOf);
		return isFriendOf;
	}
	
	public boolean isViewerWeirdTo(Viewpoint viewpoint, User user) {
		// FIXME haven't implemented this feature yet
		return false;
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

	private Account getMaybeDetachedAccount(User user) {
		Account account = user.getAccount();
		if (account == null) // must have been detached
			account = accountSystem.lookupAccountByUser(user);
		return account;
	}
	
	private Account getAttachedAccount(User user) {
		Account account = user.getAccount();
		if (account == null || !em.contains(account))
			account = accountSystem.lookupAccountByUser(user);
		return account;
	}
	
	public boolean getAccountDisabled(User user) {
		return getMaybeDetachedAccount(user).isDisabled();
	}
	
	public void setAccountDisabled(User user, boolean disabled) {
		Account account = getAttachedAccount(user);
		account.setDisabled(disabled);
		logger.debug("Disabled flag toggled to {} on account {}", disabled, account);
	}
	static final String GET_ADMIN_QUERY = 
		"SELECT adm FROM Administrator adm WHERE adm.account = :acct";

	public boolean isAdministrator(User user) {
		Account acct = getAttachedAccount(user);
		if (acct == null)
			return false;
		try {
			Administrator adm = (Administrator)em.createQuery(GET_ADMIN_QUERY)
			.setParameter("acct", acct)
			.getSingleResult();
			return adm != null;
		} catch (EntityNotFoundException e) {
			return false;
		}
	}	
	
	public boolean getMusicSharingEnabled(User user) {
		// we only share your music if your account is enabled, AND music sharing is enabled.
		// but we return only the music sharing flag here since the two settings are distinct
		// in the UI. The pref we send to the client is a composite of the two.
		Account account = getMaybeDetachedAccount(user); 
		return account.isMusicSharingEnabled();
	}
	
	public void setMusicSharingEnabled(User user, boolean enabled) {
		Account account = getAttachedAccount(user);
		if (account.isMusicSharingEnabled() != enabled) {
			account.setMusicSharingEnabled(enabled);
			messageSender.sendPrefChanged(user, "musicSharingEnabled", Boolean.toString(enabled));
		}
	}

	public boolean getMusicSharingPrimed(User user) {
		Account account = getMaybeDetachedAccount(user); 
		return account.isMusicSharingPrimed();
	}
	
	public void setMusicSharingPrimed(User user, boolean primed) {
		Account account = getAttachedAccount(user);
		if (account.isMusicSharingPrimed() != primed) {
			account.setMusicSharingPrimed(primed);
			messageSender.sendPrefChanged(user, "musicSharingPrimed", Boolean.toString(primed));
		}
	}
	
	public int incrementUserVersion(final String userId) {
		try {
			return runner.runTaskInNewTransaction(new Callable<Integer>() {
				public Integer call() {
	//				While it isn't a big deal in practice, the implementation below is slightly
	//				racy. The following would be better, but triggers a hibernate bug.
	//				
	//				em.createQuery("UPDATE User u set u.version = u.version + 1 WHERE u.id = :id")
	//				.setParameter("id", userId)
	//				.executeUpdate();
	//				
	//				return em.find(User.class, userId).getVersion();
					User user = em.find(User.class, userId);
					int newVersion = user.getVersion() + 1;
					
					user.setVersion(newVersion);
					
					return newVersion;			
				}
			});
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return 0; // not reached
		}
	}

	public void setMySpaceName(User user, String name) {
		// Refresh
		try {
			user = lookupGuid(User.class, user.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		Account acct = user.getAccount();
		acct.setMySpaceName(name);
		mySpaceTracker.updateFriendId(user);
		messageSender.sendMySpaceNameChangedNotification(user);
	}

	public Set<User> getMySpaceContacts(Viewpoint viewpoint) {
		Set<User> contacts = getRawUserContacts(viewpoint, viewpoint.getViewer());
		
		// filter out ourselves and anyone with no myspace
		Iterator<User> iterator = contacts.iterator();
		while (iterator.hasNext()) {
			User user = iterator.next();
			
			if (user.equals(viewpoint.getViewer()))
				continue;
			
			Account acct = user.getAccount();
			if (acct != null && acct.getMySpaceName() != null && acct.getMySpaceFriendId() != null)
				continue;
			
			
			iterator.remove();
		}
		return contacts;
	}
	
	public Set<User> getUserContactsWithMySpaceName(Viewpoint viewpoint, String mySpaceName) {	
		Set<User> users = getMySpaceContacts(viewpoint);
		Set<User> ret = new HashSet<User>();
		for (User u : users) {
			if (u.getAccount().getMySpaceName().equals(mySpaceName)) {
				ret.add(u);
			}
		}
		return ret;
	}
}
