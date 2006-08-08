package com.dumbhippo.server.impl;

import java.net.URL;
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
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.ContactsChangedEvent;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.Administrator;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.ExternalAccountType;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.Sentiment;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.persistence.Validators;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonView;
import com.dumbhippo.server.PersonViewExtra;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.UserViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.server.util.EJBUtil;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class IdentitySpiderBean implements IdentitySpider, IdentitySpiderRemote {
	static private final Logger logger = GlobalSetup.getLogger(IdentitySpider.class);
	
	private static final boolean DEFAULT_DEFAULT_SHARE_PUBLIC = true;
	private static final boolean DEFAULT_ENABLE_MUSIC_SHARING = true;		
	
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
	private GroupSystem groupSystem;
	
	@EJB
	private ExternalAccountSystem externalAccounts;
	
	@EJB
	private Configuration config;
	
	public User lookupUserByEmail(Viewpoint viewpoint, String email) {
		EmailResource res = lookupEmail(email);
		if (res == null)
			return null;
		return lookupUserByResource(viewpoint, res);
	}

	public User lookupUserByAim(Viewpoint viewpoint, String aim) {
		AimResource res = lookupAim(aim);
		if (res == null)
			return null;
		return lookupUserByResource(viewpoint, res);
	}	
	
	public User lookupUserByResource(Viewpoint viewpoint, Resource resource) {
		return getUser(resource);
	}

	public User lookupUser(LiveUser luser) {
		return em.find(User.class, luser.getGuid().toString());
	}	
	
	public User lookupUser(Guid guid) {
		return em.find(User.class, guid.toString());
	}	
	
	public <T extends GuidPersistable> T lookupGuidString(Class<T> klass, String id) throws ParseException, NotFoundException {
		if (klass.equals(Post.class) || klass.equals(Group.class))
			logger.error("Probable bug: looking up Post/Group should use GroupSystem/PostingBoard to get access controls");
		return EJBUtil.lookupGuidString(em, klass, id);
	}

	public <T extends GuidPersistable> T lookupGuid(Class<T> klass, Guid id) throws NotFoundException {
		if (klass.equals(Post.class) || klass.equals(Group.class))
			logger.error("Probable bug: looking up Post/Group should use GroupSystem/PostingBoard to get access controls");
		return EJBUtil.lookupGuid(em, klass, id);
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
		
	public EmailResource getEmail(final String emailRaw) throws ValidationException {
		// well, we could do a little better here with the validation...
		final String email = EmailResource.canonicalize(emailRaw);
		
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<EmailResource>() {
				
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
	}
	
	public AimResource getAim(final String screenNameRaw) throws ValidationException {
		final String screenName = AimResource.canonicalize(screenNameRaw);
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<AimResource>() {
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
		try {
			screenName = AimResource.canonicalize(screenName);
		} catch (ValidationException e) {
			return null;
		}
		return lookupResourceByName(AimResource.class, "screenName", screenName);
	}
	
	public EmailResource lookupEmail(String email) {
		try {
			email = EmailResource.canonicalize(email);
		} catch (ValidationException e) {
			return null;
		}
		return lookupResourceByName(EmailResource.class, "email", email);
	}	

	public LinkResource getLink(final URL url) {
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<LinkResource>() {
	
				public LinkResource call() throws Exception {
					Query q;
					
					q = em.createQuery("from LinkResource l where l.url = :url");
					q.setParameter("url", url.toExternalForm());
					
					LinkResource res;
					try {
						res = (LinkResource) q.getSingleResult();
					} catch (EntityNotFoundException e) {
						res = new LinkResource(url.toExternalForm());
						em.persist(res);
					}
					
					return res;					
				}
				
			});
			
		} catch (Exception e) {
			ExceptionUtils.throwAsRuntimeException(e);
			return null; // not reached
		}
	}
	
	public User getCharacter(final Character whichOne) {
		try {
			return runner.runTaskThrowingConstraintViolation(new Callable<User>() {
				public User call() {
					EmailResource email;
					try {
						email = getEmail(whichOne.getEmail());
					} catch (ValidationException e) {
						throw new RuntimeException("Character has invalid email address!");
					}
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
	
	private Set<Resource> getResourcesForPerson(Person person) {
		Set<Resource> resources = new HashSet<Resource>();
		if (person instanceof User) {
			for (AccountClaim ac : ((User)person).getAccountClaims()) {
				Resource r = ac.getResource();
				if (r instanceof EmailResource)
					resources.add(r);
				else if (r instanceof AimResource)
					resources.add(r);
				// we filter out any non-"primary" resources for now
			}
		} else if (person instanceof Contact) {
			for (ContactClaim cc : ((Contact)person).getResources()) {
				Resource r = cc.getResource();
				if (r instanceof EmailResource)
					resources.add(r);
				else if (r instanceof AimResource)
					resources.add(r);
				// we filter out any non-"primary" resources for now
			}
		} else {
			throw new IllegalArgumentException("person is not User or Contact: " + person);
		}
		
		return resources;
	}
	
	private User getUserForContact(Contact contact) {
		for (ContactClaim cc : contact.getResources()) {
			Resource resource = cc.getResource();
			if (resource instanceof Account)
				return ((Account)resource).getOwner();
			else {
				AccountClaim ac = resource.getAccountClaim();
				if (ac != null) {
					return ac.getOwner();
				}
			}
		}
		
		return null;
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
	
	public User getUser(Resource resource) {
		if (resource instanceof Account)
			return ((Account)resource).getOwner();
		else {
			AccountClaim ac = resource.getAccountClaim();
			if (ac != null)
				return ac.getOwner();
			
			return null;
		}
	}
	
	
    private void initializePersonViewFallbacks(PersonView pv, Set<Resource> fallbackResources) {
        // we want to set a fallback name from an email resource if we 
        // have one, since we won't disclose the resources and PersonView
        // ideally doesn't show as <Unknown> in the UI
    	// we need to set a Guid for the PersonView as well, because if it doesn't have a user set,
    	// it relies on the id of the primary resource
        Guid emailGuid = null; // we prefer an email resource guid as the identifying guid
        Guid anyGuid = null;		
        for (Resource r : fallbackResources) {
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
        else
            logger.warn("No fallback identifying guid for {}", pv);				
    }
	
	private void addPersonViewExtras(Viewpoint viewpoint, PersonView pv, Resource fromResource, PersonViewExtra... extras) {		
		// given the viewpoint, set whether the view is of self
		if (viewpoint.isOfUser(pv.getUser())) {
			pv.setViewOfSelf(true);
		} else {
			pv.setViewOfSelf(false);
		}
		
		// we implement this in kind of a lame way right now where we always do 
		// all the database work, even though we only return the requested information to keep 
		// other code honest		
		Set<Resource> contactResources = null;
		Set<Resource> userResources = null;
		Set<Resource> resources = null;
		if (pv.getContact() != null) {
			Contact contact = pv.getContact();
		
			// contact should point to something...
			if (contact.getResources().isEmpty())
				logger.warn("Problem in database: contact has no resources: {}", contact);
			
			// this returns only email/aim resources
			contactResources = getResourcesForPerson(contact);
						
			//logger.debug("Contact has owner {} viewpoint is {}", contact.getAccount().getOwner(),
			//		viewpoint != null ? viewpoint.getViewer() : null);
			
			// can only see resources if we're the system view or this is our own
			// Contact
			boolean ownContact = viewpoint instanceof SystemViewpoint || viewpoint.isOfUser(contact.getAccount().getOwner());

			if (!ownContact) {
				initializePersonViewFallbacks(pv, contactResources);				
				// don't disclose
				contactResources = null;
			}
		}
		
		// can only get user resources if we are a contact of the user
		if (pv.getUser() != null && isViewerSystemOrFriendOf(viewpoint, pv.getUser())) {
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
				// fromResource is not reflected in the PersonView only if the viewer is 
				// not a contact of the resource (findContactByResource did not return any 
				// results), so if we are resorting to fromResource here, we must not 
				// disclose the resource, and can only supply the fallbacks to the 
				// PersonView
				initializePersonViewFallbacks(pv, Collections.singleton(fromResource));
			}	
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
					if (viewpoint instanceof UserViewpoint) {
						boolean invited = false;
						for (Resource r : resources) {
							invited = invitationSystem.hasInvited((UserViewpoint)viewpoint, r);
							if (invited)
								break;
						}
						pv.addInvitedStatus(invited);
					}
				}
			} else if (e == PersonViewExtra.ALL_RESOURCES) {
				pv.addAllResources(resources);
			} else if (e == PersonViewExtra.ALL_EMAILS) {
				pv.addAllEmails(new TypeFilteredCollection<Resource,EmailResource>(resources, EmailResource.class));
			} else if (e == PersonViewExtra.ALL_AIMS) {
				pv.addAllAims(new TypeFilteredCollection<Resource,AimResource>(resources, AimResource.class));
			} else if (e == PersonViewExtra.EXTERNAL_ACCOUNTS) {
				if (pv.getUser() != null) {
					Set<ExternalAccount> externals = externalAccounts.getExternalAccounts(viewpoint, pv.getUser()); 
					externalAccounts.loadThumbnails(viewpoint, externals);
					pv.addExternalAccounts(externals);
				} else {
					pv.addExternalAccounts(new HashSet<ExternalAccount>());
				}
				if (pv.getExternalAccounts() == null)
					throw new IllegalStateException("Somehow set null external accounts on PersonView");
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
					if (email != null) {
						pv.addPrimaryResource(email);
					} else if (aim != null) {
						pv.addPrimaryResource(aim);
					} else {
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
		
		if (viewpoint instanceof UserViewpoint)
			contact = findContactByResource(((UserViewpoint)viewpoint).getViewer(), r);
		else
			contact = null;
		
		if (r instanceof Account) {
			user = ((Account)r).getOwner();
		} else {
			user = lookupUserByResource(viewpoint, r);
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
		addPersonViewExtras(SystemViewpoint.getInstance(), pv, null, extras);
		return pv;
	}
	
	public void addVerifiedOwnershipClaim(User claimedOwner, Resource res) {
		
		// first be sure it isn't a dup - the db constraints check this too, 
		// but it's more user friendly to no-op here than to throw a db exception
		Set<AccountClaim> claims = claimedOwner.getAccountClaims();
		for (AccountClaim claim : claims) {
			if (claim.getResource().equals(res)) {
				logger.debug("Found existing claim for {} on {}, not adding again", claimedOwner, res);
				return;
			}
		}
		
		// Now create the new db claim
		
		AccountClaim ac = new AccountClaim(claimedOwner, res);
		em.persist(ac);
		
		// Update inverse mappings
		res.setAccountClaim(ac);
		claimedOwner.getAccountClaims().add(ac);
		
		// fix up group memberships
		groupSystem.fixupGroupMemberships(claimedOwner);
	}

	public void removeVerifiedOwnershipClaim(UserViewpoint viewpoint, User owner, Resource res) {
		if (!viewpoint.isOfUser(owner)) {
			throw new RuntimeException("can only remove your own ownership claims");
		}
		Set<AccountClaim> claims = owner.getAccountClaims();
		if (claims.size() < 2) {
			// UI shouldn't let this happen, but we double-check here
			throw new RuntimeException("you have to keep at least one address on an account");
		}
		for (AccountClaim claim : claims) {
			if (claim.getResource().equals(res)) {
				logger.debug("Found claim for {} on {}, removing it", owner, res);
				res.setAccountClaim(null);
				claims.remove(claim);
				em.remove(claim);
				return;
			}
		}
		logger.warn("Tried but failed to remove claim for {} on {}", owner, res);
	}
	
	private Contact findContactByUser(User owner, User contactUser) {
		for (Contact contact : owner.getAccount().getContacts()) {
			for (ContactClaim cc : contact.getResources()) {
				AccountClaim ac = cc.getResource().getAccountClaim();
				if (ac != null && ac.getOwner().equals(contactUser))
					return contact;
			}
		}
		
		return null;
	}

	private Contact findContactByResource(User owner, Resource resource) {
		Account account = owner.getAccount();
		Set<Contact> contacts = account.getContacts();
		for (Contact contact : contacts) {
			for (ContactClaim cc : contact.getResources()) {
				Resource r = cc.getResource();
				if (r != null && r.equals(resource))
					return contact;
			}
		}
		
		return null;
	}
	
	private Contact doCreateContact(User user, Resource resource) {
		
		logger.debug("Creating contact for user {} with resource {}", user, resource);
		
		Account account = user.getAccount();
		Contact contact = new Contact(account);

		// FIXME we don't want contacts to have nicknames, so leave it null for 
		// now, but eventually we should change the db schema 
		//contact.setNickname(resource.getHumanReadableString());
		em.persist(contact);
		

		// Updating the inverse mappings is essential since they are cached; 
		// if we don't update them the second-level cache will contain stale data. 
		// Updating them won't actually update the data in the second-level cache for
		// a non-transactional cache; rather it will flag the data to be reloaded
		// from the database.
		//
		// In order to add the contact to the account's list of contacts
		// or to add the contact claim to the account's list of resources
		// we need to load the set of existing values; the second-level
		// cache makes that cheap. 
		
		account.addContact(contact);

		ContactClaim cc = new ContactClaim(contact, resource);
		em.persist(cc);
		
		contact.getResources().add(cc);

		// After the transaction commits succesfully, update the list of contact resources for this user
		LiveState.getInstance().queuePostTransactionUpdate(em, new ContactsChangedEvent(user.getGuid()));
		
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
			// users, and not just the EmailResource
			if (!(resource instanceof Account)) {
				User contactUser = lookupUserByResource(SystemViewpoint.getInstance(), resource);
				
				if (contactUser != null) {
					ContactClaim cc = new ContactClaim(contact, contactUser.getAccount());
					em.persist(cc);					
					contact.getResources().add(cc);
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
		if (contactPerson instanceof Contact) {
			contact = (Contact)contactPerson;
		} else {
			contact = findContactByUser(user, (User)contactPerson);
			if (contact == null) {
				logger.debug("User {} not found as a contact", contactPerson);
				return;
			}
		}
		
		user.getAccount().removeContact(contact);
		em.remove(contact);
		logger.debug("contact deleted");
		
		// After the transaction commits succesfully, update the list of contact resources for this user
		LiveState.getInstance().queuePostTransactionUpdate(em, new ContactsChangedEvent(user.getGuid()));
	}

	public Set<Contact> getRawContacts(Viewpoint viewpoint, User user) {
		if (!isViewerSystemOrFriendOf(viewpoint, user))
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
	
	static final private String GET_ACCOUNTS_WITH_ACCOUNT_AS_CONTACT_QUERY = 
		"SELECT cc.account FROM ContactClaim cc WHERE cc.resource = :account";

	private List<Account> getAccountsWhoHaveUserAsContact(User user) {
		Query q = em.createQuery(GET_ACCOUNTS_WITH_ACCOUNT_AS_CONTACT_QUERY);
		q.setParameter("account", user.getAccount());
	    List<Account> accounts = TypeUtils.castList(Account.class, q.getResultList());
	    return accounts;
	}
	
	public Set<PersonView> getFollowers(Viewpoint viewpoint, User user, PersonViewExtra... extras) {
		
		Set<PersonView> followers = new HashSet<PersonView>();
	    if (!(viewpoint.isOfUser(user) || viewpoint instanceof SystemViewpoint)) {
	        // can only see followers if self
	    	return followers;
	    }
	    
	    // we are only interested in user contacts of self
	    Set<User> rawContactsOfSelf = getRawUserContacts(viewpoint, user);
	    List<Account> accounts = getAccountsWhoHaveUserAsContact(user);
	    
		for (Account a : accounts) {		
			User selfAContactOf = a.getOwner();
		    if (!rawContactsOfSelf.contains(selfAContactOf)) {
		    	// this person is our follower, because we don't have them as a contact!
				PersonView pv = getPersonView(viewpoint, selfAContactOf, extras);
				followers.add(pv);
		    }
		}
	    
		return followers;
	}
	
	public Set<User> getUsersWhoHaveUserAsContact(Viewpoint viewpoint, User user) {
	    if (!(viewpoint.isOfUser(user) || viewpoint instanceof SystemViewpoint)) {
	    	return Collections.emptySet();
	    }
	    List<Account> accounts = getAccountsWhoHaveUserAsContact(user);
	    Set<User> users = new HashSet<User>();
	    for (Account a : accounts) {
	    	users.add(a.getOwner());
	    }
	    return users;
	}
	
	/**
	 * 
	 * @param user the user we're looking at the contacts of
	 * @param contactUser is this person a contact of user?
	 * @return true if contactUser is a contact of user
	 */
	private boolean isContactNoViewpoint(User user, User contactUser) {
		LiveUser liveUser = LiveState.getInstance().getLiveUser(user.getGuid());
		for (AccountClaim ac : contactUser.getAccountClaims()) {
			if (liveUser.hasContactResource(ac.getResource().getGuid()))
				return true;
		}
		
		return false;
	}
	
	// We don't want to use the general isContactNoViewpoint for this, because
	// there could be race conditions with the async-updated LiveUser cache.
	// 
	// We don't mind reconstituting *the viewer's* contact database from the
	// hibernate second level cache, so we just do the check directly
	private boolean viewerHasContact(UserViewpoint viewpoint, User contactUser) {
		for (Contact contact : viewpoint.getViewer().getAccount().getContacts()) {
			for (ContactClaim cc : contact.getResources()) {
				AccountClaim ac = cc.getResource().getAccountClaim();
				if (ac != null && ac.getOwner().equals(contactUser))
					return true;
			}
		}
		
		return false;
	}
	
	public boolean isContact(Viewpoint viewpoint, User user, User contactUser) {
		// see if we're allowed to look at who user's contacts are
		if (!isViewerSystemOrFriendOf(viewpoint, user))
			return false;

		if (viewpoint.isOfUser(user))
			return viewerHasContact((UserViewpoint)viewpoint, contactUser);
		
		// if we can see their contacts, return whether this person is one of them
		return isContactNoViewpoint(user, contactUser);
	}
	
	public boolean isViewerSystemOrFriendOf(Viewpoint viewpoint, User user) {
		if (viewpoint instanceof SystemViewpoint) {
			return true;
		} else if (viewpoint instanceof UserViewpoint) {
			UserViewpoint userViewpoint = (UserViewpoint)viewpoint;
			if (user.equals(userViewpoint.getViewer()))
				return true;
			if (userViewpoint.isFriendOfStatusCached(user))
				return userViewpoint.getCachedFriendOfStatus(user);
			
			boolean isFriendOf = isContactNoViewpoint(user, userViewpoint.getViewer());
			userViewpoint.setCachedFriendOfStatus(user, isFriendOf);
			return isFriendOf;
		} else {
			return false;
		}
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
		Account account = null;
		if (em.contains(user))
			account = user.getAccount();
		if (account == null || !em.contains(account))
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
		
		boolean noAuthentication = config.getProperty(HippoProperty.DISABLE_AUTHENTICATION).equals("true");
		if (noAuthentication) {
			logger.debug("auth disabled - everyone gets to be an administrator!");
			return true;
		}
		
		try {
			Administrator adm = (Administrator)em.createQuery(GET_ADMIN_QUERY)
			.setParameter("acct", acct)
			.getSingleResult();
			return adm != null;
		} catch (EntityNotFoundException e) {
			return false;
		}
	}	
	
	public boolean getMusicSharingEnabled(User user, Enabled enabled) {
		// we only share your music if your account is enabled, AND music sharing is enabled.
		// but we return only the music sharing flag here since the two settings are distinct
		// in the UI. The pref we send to the client is a composite of the two.
		Account account = getMaybeDetachedAccount(user);
		Boolean musicSharingEnabled = account.isMusicSharingEnabled();
		if (musicSharingEnabled == null)
			musicSharingEnabled = DEFAULT_ENABLE_MUSIC_SHARING;
		
		switch (enabled) {
		case RAW_PREFERENCE_ONLY:
			return musicSharingEnabled;
		case AND_ACCOUNT_IS_ACTIVE:
			return !(account.isDisabled() || account.isAdminDisabled()) && musicSharingEnabled;
		}
		throw new IllegalArgumentException("invalid value for enabled param to getMusicSharingEnabled");
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
	
	public boolean getNotifyPublicShares(User user) {
		Account account = getMaybeDetachedAccount(user);
		Boolean defaultSharePublic = account.isNotifyPublicShares();
		if (defaultSharePublic == null)
			return DEFAULT_DEFAULT_SHARE_PUBLIC;		
		return defaultSharePublic;
	}
	
	public void setNotifyPublicShares(User user, boolean defaultPublic) {
		Account account = getAttachedAccount(user);
		if (account.isNotifyPublicShares() == null ||
			account.isNotifyPublicShares() != defaultPublic) {
			account.setNotifyPublicShares(defaultPublic);
		}		
	}
	
	public void incrementUserVersion(final User user) {
    //		While it isn't a big deal in practice, the implementation below is slightly
	//		racy. The following would be better, but triggers a hibernate bug.
	//				
	//		em.createQuery("UPDATE User u set u.version = u.version + 1 WHERE u.id = :id")
	//			.setParameter("id", userId)
	//			.executeUpdate();
	//			
	//			em.refresh(user);
		user.setVersion(user.getVersion() + 1);
	}

	public void setBio(UserViewpoint viewpoint, User user, String bio) {
		if (!viewpoint.isOfUser(user))
			throw new RuntimeException("can only set one's own bio");
		if (!em.contains(user))
			throw new RuntimeException("user not attached");
		Account acct = user.getAccount();
		acct.setBio(bio);
	}

	public void setMusicBio(UserViewpoint viewpoint, User user, String bio) {
		if (!viewpoint.isOfUser(user))
			throw new RuntimeException("can only set one's own music bio");
		if (!em.contains(user))
			throw new RuntimeException("user not attached");
		Account acct = user.getAccount();
		acct.setMusicBio(bio);
	}
	
	/**
	 * Note that the photo CAN be null, which means to use the uploaded 
	 * photo for the user instead of a photo filename.
	 */
	public void setStockPhoto(UserViewpoint viewpoint, User user, String photo) {
		if (!viewpoint.isOfUser(user))
			throw new RuntimeException("can only set one's own photo");
		if (photo != null && !Validators.validateStockPhoto(photo))
			throw new RuntimeException("invalid stock photo name");

		user.setStockPhoto(photo);
	}
	
	public Set<User> getMySpaceContacts(UserViewpoint viewpoint) {
		Set<User> contacts = getRawUserContacts(viewpoint, viewpoint.getViewer());
		
		// filter out ourselves and anyone with no myspace
		Iterator<User> iterator = contacts.iterator();
		while (iterator.hasNext()) {
			User user = iterator.next();
			
			if (!user.equals(viewpoint.getViewer())) {	
				ExternalAccount external;
				try {
					// not using externalAccounts.getMySpaceName() because we also want to check we have the friend id
					external = externalAccounts.lookupExternalAccount(viewpoint, user, ExternalAccountType.MYSPACE);
					if (external.getSentiment() == Sentiment.LOVE &&
							external.getHandle() != null &&
							external.getExtra() != null) {
						// we have myspace name AND friend ID
						continue;
					}				
				} catch (NotFoundException e) {
					// nothing
				} 
			}
			
			// remove - did not have a myspace name
			iterator.remove();
		}
		return contacts;
	}
	
	public Set<User> getUserContactsWithMySpaceName(UserViewpoint viewpoint, String mySpaceName) {	
		Set<User> users = getMySpaceContacts(viewpoint);
		Set<User> ret = new HashSet<User>();
		for (User u : users) {
			try {
				String name = externalAccounts.getMySpaceName(viewpoint, u);
				if (name.equals(mySpaceName)) {
					ret.add(u);
				}
			} catch (NotFoundException e) {
				// nothing
			}
		}
		return ret;
	}
	
	public long getNumberOfActiveAccounts(UserViewpoint viewpoint) {
		if (!isAdministrator(viewpoint.getViewer()))
			throw new RuntimeException("can't do this if you aren't an admin");
		return accountSystem.getNumberOfActiveAccounts();
	}
	
	public Set<PersonView> getAllUsers(UserViewpoint viewpoint) {
		if (!isAdministrator(viewpoint.getViewer()))
			throw new RuntimeException("getAllUsers is admin-only");
		
		@SuppressWarnings("unchecked")
		List<User> users = em.createQuery("SELECT u FROM User u").getResultList();
		Set<PersonView> result = new HashSet<PersonView>();
		for (User user : users) {
			result.add(getPersonView(SystemViewpoint.getInstance(), user, PersonViewExtra.ALL_RESOURCES));
		}
		
		return result;
	}
}
