package com.dumbhippo.server.impl;

import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.live.UserChangedEvent;
import com.dumbhippo.live.UserPrefChangedEvent;
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
import com.dumbhippo.persistence.UserBioChangedRevision;
import com.dumbhippo.persistence.ValidationException;
import com.dumbhippo.persistence.Validators;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.IdentitySpiderRemote;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Notifier;
import com.dumbhippo.server.RevisionControl;
import com.dumbhippo.server.TransactionRunner;
import com.dumbhippo.server.util.EJBUtil;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class IdentitySpiderBean implements IdentitySpider, IdentitySpiderRemote {
	static private final Logger logger = GlobalSetup
			.getLogger(IdentitySpider.class);

	private static final boolean DEFAULT_DEFAULT_SHARE_PUBLIC = true;

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private TransactionRunner runner;

	@EJB
	@IgnoreDependency
	private GroupSystem groupSystem;

	@EJB
	@IgnoreDependency
	private ExternalAccountSystem externalAccounts;

	@EJB
	private Configuration config;
	
	@EJB
	private Notifier notifier;

	@EJB
	private RevisionControl revisionControl;
	
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
	
	public <T extends GuidPersistable> T lookupGuidString(Class<T> klass,
			String id) throws ParseException, NotFoundException {
		if (klass.equals(Post.class) || klass.equals(Group.class))
			logger.error("Probable bug: looking up Post/Group should use GroupSystem/PostingBoard to get access controls");
		return EJBUtil.lookupGuidString(em, klass, id);
	}

	public <T extends GuidPersistable> T lookupGuid(Class<T> klass, Guid id)
			throws NotFoundException {
		if (klass.equals(Post.class) || klass.equals(Group.class))
			logger.error("Probable bug: looking up Post/Group should use GroupSystem/PostingBoard to get access controls");
		return EJBUtil.lookupGuid(em, klass, id);
	}

	public <T extends GuidPersistable> Set<T> lookupGuidStrings(Class<T> klass,
			Set<String> ids) throws ParseException, NotFoundException {
		Set<T> ret = new HashSet<T>();
		for (String s : ids) {
			T obj = lookupGuidString(klass, s);
			ret.add(obj);
		}
		return ret;
	}

	public <T extends GuidPersistable> Set<T> lookupGuids(Class<T> klass,
			Set<Guid> ids) throws NotFoundException {
		Set<T> ret = new HashSet<T>();
		for (Guid id : ids) {
			T obj = lookupGuid(klass, id);
			ret.add(obj);
		}
		return ret;
	}

	public EmailResource getEmail(final String emailRaw)
			throws ValidationException {
		// well, we could do a little better here with the validation...
		final String email = EmailResource.canonicalize(emailRaw);

		try {
			return runner
					.runTaskThrowingConstraintViolation(new Callable<EmailResource>() {

						public EmailResource call() throws Exception {
							Query q;

							q = em
									.createQuery("from EmailResource e where e.email = :email");
							q.setParameter("email", email);

							EmailResource res;
							try {
								res = (EmailResource) q.getSingleResult();
							} catch (NoResultException e) {
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

	public AimResource getAim(final String screenNameRaw)
			throws ValidationException {
		final String screenName = AimResource.canonicalize(screenNameRaw);
		try {
			return runner
					.runTaskThrowingConstraintViolation(new Callable<AimResource>() {
						public AimResource call() {
							Query q;

							q = em
									.createQuery("from AimResource a where a.screenName = :name");
							q.setParameter("name", screenName);

							AimResource res;
							try {
								res = (AimResource) q.getSingleResult();
							} catch (NoResultException e) {
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

	private <T extends Resource> T lookupResourceByName(Class<T> klass,
			String identifier, String name) {
		Query q;
		String className = klass.getName();

		q = em.createQuery("SELECT a FROM " + className + " a WHERE a." + identifier
				+ " = :name");
		q.setParameter("name", name);

		T res = null;
		try {
			res = klass.cast(q.getSingleResult());
		} catch (NoResultException e) {
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

	public LinkResource lookupLink(final URL url) {
		return lookupResourceByName(LinkResource.class, "url", url.toExternalForm());
	}
	
	public LinkResource getLink(final URL url) {
		try {
			return runner
					.runTaskThrowingConstraintViolation(new Callable<LinkResource>() {

						public LinkResource call() throws Exception {
							Query q;

							q = em.createQuery("SELECT l FROM LinkResource l WHERE l.url = :url");
							q.setParameter("url", url.toExternalForm());

							LinkResource res;
							try {
								res = (LinkResource) q.getSingleResult();
							} catch (NoResultException e) {
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
	
	private User getUserForContact(Contact contact) {
		for (ContactClaim cc : contact.getResources()) {
			Resource resource = cc.getResource();
			if (resource instanceof Account)
				return ((Account) resource).getOwner();
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
		if (person == null)
			throw new IllegalArgumentException("null person in getUser()");
		
		if (person instanceof User)
			return (User) person;
		else {
			// logger.debug("getUser: contact = {}", person);

			User user = getUserForContact((Contact) person);

			// logger.debug("getUserForContact: user = {}", user);

			return user;
		}
	}

	public User getUser(Resource resource) {
		if (resource instanceof Account)
			return ((Account) resource).getOwner();
		else {
			AccountClaim ac = resource.getAccountClaim();
			if (ac != null)
				return ac.getOwner();

			return null;
		}
	}

	public void addVerifiedOwnershipClaim(User claimedOwner, Resource res) {

		// first be sure it isn't a dup - the db constraints check this too,
		// but it's more user friendly to no-op here than to throw a db
		// exception
		Set<AccountClaim> claims = claimedOwner.getAccountClaims();
		for (AccountClaim claim : claims) {
			if (claim.getResource().equals(res)) {
				logger.debug(
						"Found existing claim for {} on {}, not adding again",
						claimedOwner, res);
				return;
			}
		}

		// Now create the new db claim
		
		res.prepareToSetAccountClaim();

		AccountClaim ac = new AccountClaim(claimedOwner, res);
		em.persist(ac);

		// Update inverse mappings
		res.setAccountClaim(ac);
		claimedOwner.getAccountClaims().add(ac);

		// fix up group memberships
		groupSystem.fixupGroupMemberships(claimedOwner);
		
		// People may have listed the newly claimed resource as a contact
		LiveState.getInstance().invalidateContacters(claimedOwner.getGuid());
	}

	public void removeVerifiedOwnershipClaim(UserViewpoint viewpoint,
			User owner, Resource res) {
		if (!viewpoint.isOfUser(owner)) {
			throw new RuntimeException(
					"can only remove your own ownership claims");
		}
		Set<AccountClaim> claims = owner.getAccountClaims();
		if (claims.size() < 2) {
			// UI shouldn't let this happen, but we double-check here
			throw new RuntimeException(
					"you have to keep at least one address on an account");
		}
		for (AccountClaim claim : claims) {
			if (claim.getResource().equals(res)) {
				logger.debug("Found claim for {} on {}, removing it", owner,
						res);
				res.setAccountClaim(null);
				claims.remove(claim);
				em.remove(claim);
				
				// People may have listed the old resource as a contact
				LiveState.getInstance().invalidateContacters(owner.getGuid());
				
				return;
			}
		}
		logger
				.warn("Tried but failed to remove claim for {} on {}", owner,
						res);
	}

	private Contact findContactByUser(User owner, User contactUser) throws NotFoundException {
		Query q = em.createQuery("SELECT cc.contact " +
				 				 "  FROM ContactClaim cc, AccountClaim ac " +
				 				 "  WHERE cc.resource = ac.resource " +
				 				 "    AND cc.account = :account " +
				 				 "    AND ac.owner = :contact");
		q.setParameter("account", owner.getAccount());
		q.setParameter("contact", contactUser);
		q.setMaxResults(1);
	
		try {
			return (Contact)q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("No contact for user");
		}
	}
	
	public Contact findContactByResource(User owner, Resource resource) throws NotFoundException {
		Query q = em.createQuery("SELECT cc.contact " +
				 				 "  FROM ContactClaim cc " +
				 				 "  WHERE cc.account = :account" +
				 				 "    AND cc.resource = :contact");
		q.setParameter("account", owner.getAccount());
		q.setParameter("contact", resource);
		q.setMaxResults(1);

		try {
			return (Contact)q.getSingleResult();
		} catch (NoResultException e) {
			throw new NotFoundException("No contact for user");
		}
	}

	private Contact doCreateContact(User user, Resource resource) {

		logger.debug("Creating contact for user {} with resource {}", user,
				resource);

		Account account = user.getAccount();
		Contact contact = new Contact(account);

		// FIXME we don't want contacts to have nicknames, so leave it null for
		// now, but eventually we should change the db schema
		// contact.setNickname(resource.getHumanReadableString());
		em.persist(contact);

		// Updating the inverse mappings is essential since they are cached;
		// if we don't update them the second-level cache will contain stale
		// data.
		// Updating them won't actually update the data in the second-level
		// cache for a non-transactional cache; rather it will flag the data to be
		// reloaded from the database.
		//
		ContactClaim cc = new ContactClaim(contact, resource);
		em.persist(cc);

		contact.getResources().add(cc);
		
		User contactUser = getUser(resource);

		LiveState liveState = LiveState.getInstance();
		liveState.invalidateContacts(user.getGuid());
		if (contactUser != null)
			liveState.invalidateContacters(contactUser.getGuid());

		return contact;
	}

	public Contact createContact(User user, Resource resource) {
		if (user == null)
			throw new IllegalArgumentException("null contact owner");
		if (resource == null)
			throw new IllegalArgumentException("null contact resource");

		Contact contact;
		
		try {
			contact = findContactByResource(user, resource);
		} catch (NotFoundException e) {
			contact = doCreateContact(user, resource);

			// Things work better (especially for now, when we don't fully
			// implement spidering) if contacts own the account resource for
			// users, and not just the EmailResource
			if (!(resource instanceof Account)) {
				User contactUser = lookupUserByResource(SystemViewpoint
						.getInstance(), resource);

				if (contactUser != null) {
					ContactClaim cc = new ContactClaim(contact, contactUser
							.getAccount());
					em.persist(cc);
					contact.getResources().add(cc);
					logger.debug(
							"Added contact resource {} pointing to account {}",
							cc.getContact(), cc.getAccount());
				}
			}
		}

		return contact;
	}

	public void addContactPerson(User user, Person contactPerson) {
		logger.debug("adding contact " + contactPerson + " to account "
				+ user.getAccount());

		if (contactPerson instanceof Contact) {
			// Must be a contact of user, so nothing to do
			assert ((Contact) contactPerson).getAccount() == user.getAccount();
		} else {
			User contactUser = (User) contactPerson;

			try {
				findContactByUser(user, contactUser);
			} catch (NotFoundException e) { 
				doCreateContact(user, contactUser.getAccount());
			}
		}
	}
	
	public void removeContactPerson(User user, Person contactPerson) {
		logger.debug("removing contact {} from account {}", contactPerson, user.getAccount());
		
		Contact contact;
		if (contactPerson instanceof Contact) {
			contact = (Contact) contactPerson;
		} else {
			try {
				contact = findContactByUser(user, (User)contactPerson);
			} catch (NotFoundException e) {
				logger.debug("User {} not found as a contact", contactPerson);
				return;
			}
		}

		removeContact(user, contact);
	}

	public void removeContactResource(User user, Resource contactResource) {
		logger.debug("removing contact {} from account {}", contactResource, user.getAccount());
		
		Contact contact;
		try {
		    contact = findContactByResource(user, contactResource);
		    removeContact(user, contact);
		} catch (NotFoundException e) {
			logger.debug("Resource {} not found as a contact", contactResource);
			return;
		}
	}
	
	public void removeContact(User user, Contact contact) {	
		Set<User> removedUsers = new HashSet<User>();
		
		for (ContactClaim cc : contact.getResources()) {
			User resourceUser = getUser(cc.getResource());
			if (resourceUser != null)
				removedUsers.add(resourceUser);
		}

		em.remove(contact);
		logger.debug("contact deleted");

		LiveState liveState = LiveState.getInstance();
		liveState.invalidateContacts(user.getGuid());
		for (User removedUser : removedUsers) {
			liveState.invalidateContacters(removedUser.getGuid());
			
		}
	}
	
	public Set<Guid> computeContacts(Guid userId) {
		User user = em.find(User.class, userId.toString());
		
		Query q = em.createQuery("SELECT ac.owner.id " +
                 				 "  FROM ContactClaim cc, AccountClaim ac " +
                 				 "  WHERE cc.resource = ac.resource " +
				                 "    AND cc.account = :account");
		q.setParameter("account", user.getAccount());
		
		Set<Guid> result = new HashSet<Guid>();
		for (String s : TypeUtils.castList(String.class, q.getResultList())) {
			try {
				result.add(new Guid(s));
			} catch (ParseException e) {
				throw new RuntimeException("Bad GUID in database");
			}
		}
		
		return result;
	}
	
	public Set<Guid> computeContacters(Guid userId) {
		Query q = em.createQuery("SELECT cc.account.owner.id " +
                                 "  FROM ContactClaim cc, AccountClaim ac " +
                 				 "  WHERE cc.resource = ac.resource " +
				                 "    AND ac.owner.id = :userId");
		q.setParameter("userId", userId.toString());

		Set<Guid> result = new HashSet<Guid>();
		for (String s : TypeUtils.castList(String.class, q.getResultList())) {
			try {
				result.add(new Guid(s));
			} catch (ParseException e) {
				throw new RuntimeException("Bad GUID in database");
			}
		}

		return result;
	}
	
	public int computeContactsCount(User user) {
		Query q = em.createQuery("SELECT COUNT(*) FROM Contact c WHERE c.account = :account");
		q.setParameter("account", user.getAccount());
		
		return ((Number)q.getSingleResult()).intValue();
	}

	public Set<User> getRawUserContacts(Viewpoint viewpoint, User user, boolean includeSelf) {
		if (!isViewerSystemOrFriendOf(viewpoint, user))
			return Collections.emptySet();

		Guid viewedUserId = user.getGuid();
		Set<User> ret = new HashSet<User>();
		for (Guid guid : LiveState.getInstance().getContacts(viewedUserId)) {
			if (!includeSelf && viewedUserId.equals(guid))
				continue;
			ret.add(em.find(User.class, guid.toString()));
		}
		return ret;
	}

	static final private String GET_ACCOUNTS_WITH_ACCOUNT_AS_CONTACT_QUERY = "SELECT cc.account FROM ContactClaim cc WHERE cc.resource = :account";

	public List<Account> getAccountsWhoHaveUserAsContact(User user) {
		Query q = em.createQuery(GET_ACCOUNTS_WITH_ACCOUNT_AS_CONTACT_QUERY);
		q.setParameter("account", user.getAccount());
		List<Account> accounts = TypeUtils.castList(Account.class, q
				.getResultList());
		return accounts;
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
	 * @param user
	 *            the user we're looking at the contacts of
	 * @param contactUser
	 *            is this person a contact of user?
	 * @return true if contactUser is a contact of user
	 */
	private boolean userHasContact(Viewpoint viewpoint, User user, User contactUser) {
		LiveState liveState = LiveState.getInstance();

		// See if we have either contacts or contacters cached for the relationship
		Set<Guid> contacters = liveState.peekContacters(contactUser.getGuid());
		if (contacters != null) {
			return contacters.contains(user.getGuid());
		}

		Set<Guid> contacts = liveState.peekContacts(user.getGuid());
		if (contacts != null) {
			return contacts.contains(contactUser.getGuid());
		}
		
		// If neither is cached, we compute one of them; we prefer to cache
		// information for the viewer; when neither the user or contactUser is
		// the viewer we prefer the contact side of the relationship (somewhat
		// abitrarily)
		if (viewpoint.isOfUser(contactUser)) {
			contacters = liveState.getContacters(contactUser.getGuid());
			return contacters.contains(user.getGuid());
		} else {
			contacts = liveState.getContacts(user.getGuid());
			return contacts.contains(contactUser.getGuid());
		}
	}
	
	public boolean isContact(Viewpoint viewpoint, User user, User contactUser) {
		// see if we're allowed to look at who user's contacts are
		if (!isViewerSystemOrFriendOf(viewpoint, user))
			return false;

		// if we can see their contacts, return whether this person is one of them
		return userHasContact(viewpoint, user, contactUser);
	}

	public boolean isViewerSystemOrFriendOf(Viewpoint viewpoint, User user) {
		if (viewpoint instanceof SystemViewpoint) {
			return true;
		} else if (viewpoint instanceof UserViewpoint) {
			UserViewpoint userViewpoint = (UserViewpoint) viewpoint;
			if (user.equals(userViewpoint.getViewer()))
				return true;
			
			return userHasContact(viewpoint, user, userViewpoint.getViewer());
		} else {
			return false;
		}
	}
    
	public boolean isViewerWeirdTo(Viewpoint viewpoint, User user) {
		// FIXME haven't implemented this feature yet
		return false;
	}

	static final String GET_CONTACT_RESOURCES_QUERY = "SELECT cc.resource FROM ContactClaim cc WHERE cc.contact = :contact";

	private Resource getFirstContactResource(Contact contact) {
		// An invariant we retain in the database is that every contact
		// has at least one resource, so we don't need to check for
		// NoResultException
		return (Resource) em.createQuery(GET_CONTACT_RESOURCES_QUERY)
				.setParameter("contact", contact).setMaxResults(1)
				.getSingleResult();
	}

	public Resource getBestResource(Person person) {
		User user = getUser(person);
		if (user != null)
			return user.getAccount();

		return getFirstContactResource((Contact) person);
	}

	private Account getAttachedAccount(User user) {
		if (!em.contains(user))
			user = lookupUser(user.getGuid());
		return user.getAccount();
	}

	public boolean getAccountDisabled(User user) {
		return user.getAccount().isDisabled();
	}

	public void setAccountDisabled(User user, boolean disabled) {
		Account account = getAttachedAccount(user);
		if (account.isDisabled() != disabled) {
			account.setDisabled(disabled);
			logger.debug("Disabled flag toggled to {} on account {}", disabled,
					account);
			notifier.onAccountDisabledToggled(account);
		}
	}

	public boolean getAccountAdminDisabled(User user) {
		return user.getAccount().isAdminDisabled();
	}
	
	public void setAccountAdminDisabled(User user, boolean disabled) {
		Account account = getAttachedAccount(user);
		if (account.isAdminDisabled() != disabled) {
			account.setAdminDisabled(disabled);
			logger.debug("adminDisabled flag toggled to {} on account {}", disabled,
					account);
			notifier.onAccountAdminDisabledToggled(account);
		}
	}
	
	static final String GET_ADMIN_QUERY = "SELECT adm FROM Administrator adm WHERE adm.account = :acct";

	public boolean isAdministrator(User user) {
		Account acct = getAttachedAccount(user);
		if (acct == null)
			return false;

		boolean noAuthentication = config.getProperty(
				HippoProperty.DISABLE_AUTHENTICATION).equals("true");
		if (noAuthentication) {
			logger
					.debug("auth disabled - everyone gets to be an administrator!");
			return true;
		}

		try {
			Administrator adm = (Administrator) em.createQuery(GET_ADMIN_QUERY)
					.setParameter("acct", acct).getSingleResult();
			return adm != null;
		} catch (NoResultException e) {
			return false;
		}
	}

	public boolean getMusicSharingEnabled(User user, Enabled enabled) {
		// we only share your music if your account is enabled, AND music
		// sharing is enabled.
		// but we return only the music sharing flag here since the two settings
		// are distinct
		// in the UI. The pref we send to the client is a composite of the two.
		Account account = user.getAccount();
		Boolean musicSharingEnabled = account.isMusicSharingEnabled();
		if (musicSharingEnabled == null)
			musicSharingEnabled = AccountSystem.DEFAULT_ENABLE_MUSIC_SHARING;

		switch (enabled) {
		case RAW_PREFERENCE_ONLY:
			return musicSharingEnabled;
		case AND_ACCOUNT_IS_ACTIVE:
			return account.isActive() && musicSharingEnabled;
		}
		throw new IllegalArgumentException(
				"invalid value for enabled param to getMusicSharingEnabled");
	}

	public void setMusicSharingEnabled(User user, boolean enabled) {
		Account account = getAttachedAccount(user);
		if (account.isMusicSharingEnabled() == null || account.isMusicSharingEnabled() != enabled) {
			account.setMusicSharingEnabled(enabled);
			notifier.onMusicSharingToggled(account);
			LiveState.getInstance().queueUpdate(new UserPrefChangedEvent(user.getGuid(), "musicSharingEnabled", Boolean.toString(enabled)));
		}
	}

	public boolean getMusicSharingPrimed(User user) {
		Account account = getAttachedAccount(user);
		return account.isMusicSharingPrimed();
	}

	public void setMusicSharingPrimed(User user, boolean primed) {
		Account account = getAttachedAccount(user);
		if (account.isMusicSharingPrimed() != primed) {
			account.setMusicSharingPrimed(primed);
			LiveState.getInstance().queueUpdate(new UserPrefChangedEvent(user.getGuid(), "musicSharingPrimed", Boolean.toString(primed)));			
		}
	}

	public boolean getNotifyPublicShares(User user) {
		Account account = user.getAccount();
		Boolean defaultSharePublic = account.isNotifyPublicShares();
		if (defaultSharePublic == null)
			return DEFAULT_DEFAULT_SHARE_PUBLIC;
		return defaultSharePublic;
	}

	public void setNotifyPublicShares(User user, boolean defaultPublic) {
		Account account = getAttachedAccount(user);
		if (account.isNotifyPublicShares() == null
				|| account.isNotifyPublicShares() != defaultPublic) {
			account.setNotifyPublicShares(defaultPublic);
		}
	}

	public void incrementUserVersion(final User user) {
		// While it isn't a big deal in practice, the implementation below is
		// slightly
		// racy. The following would be better, but triggers a hibernate bug.
		//				
		// em.createQuery("UPDATE User u set u.version = u.version + 1 WHERE
		// u.id = :id")
		// .setParameter("id", userId)
		// .executeUpdate();
		//			
		// em.refresh(user);
		user.setVersion(user.getVersion() + 1);
	}

	public void setBio(UserViewpoint viewpoint, User user, String bio) {
		if (!viewpoint.isOfUser(user))
			throw new RuntimeException("can only set one's own bio");
		if (!em.contains(user))
			throw new RuntimeException("user not attached");
		Account acct = user.getAccount();
		acct.setBio(bio);
		revisionControl.persistRevision(new UserBioChangedRevision(viewpoint.getViewer(), new Date(), bio));
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
	 * Note that the photo CAN be null, which means to use the uploaded photo
	 * for the user instead of a photo filename.
	 */
	public void setStockPhoto(UserViewpoint viewpoint, User user, String photo) {
		if (!viewpoint.isOfUser(user))
			throw new RuntimeException("can only set one's own photo");
		if (photo != null && !Validators.validateStockPhoto(photo))
			throw new RuntimeException("invalid stock photo name");

		user.setStockPhoto(photo);
		LiveState.getInstance().queueUpdate(new UserChangedEvent(user.getGuid(), UserChangedEvent.Detail.PHOTO)); 		
	}

	public Set<User> getMySpaceContacts(UserViewpoint viewpoint) {
		Set<User> contacts = getRawUserContacts(viewpoint, viewpoint
				.getViewer(), true);

		// filter out ourselves and anyone with no myspace
		Iterator<User> iterator = contacts.iterator();
		while (iterator.hasNext()) {
			User user = iterator.next();

			if (!user.equals(viewpoint.getViewer())) {
				ExternalAccount external;
				try {
					// not using externalAccounts.getMySpaceName() because we
					// also want to check we have the friend id
					external = externalAccounts.lookupExternalAccount(
							viewpoint, user, ExternalAccountType.MYSPACE);
					if (external.getSentiment() == Sentiment.LOVE
							&& external.getHandle() != null
							&& external.getExtra() != null) {
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
}
