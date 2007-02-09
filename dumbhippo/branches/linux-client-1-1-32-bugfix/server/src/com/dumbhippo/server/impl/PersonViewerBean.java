package com.dumbhippo.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeUtils;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.Configuration.PropertyNotFoundException;
import com.dumbhippo.server.views.ExternalAccountView;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.PersonViewExtra;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.Viewpoint;

/*
 * An implementation of the Identity Spider.  It sucks your blood.
 * @author walters
 */
@Stateless
public class PersonViewerBean implements PersonViewer {
	static private final Logger logger = GlobalSetup
			.getLogger(PersonViewer.class);

	@PersistenceContext(unitName = "dumbhippo")
	private EntityManager em;

	@EJB
	private IdentitySpider identitySpider;

	@EJB
	@IgnoreDependency
	private ExternalAccountSystem externalAccounts;

	@EJB
	@IgnoreDependency
	private InvitationSystem invitationSystem;

	@EJB
	private Configuration config;

	private Set<Resource> getResourcesForPerson(Person person) {
		Set<Resource> resources = new HashSet<Resource>();
		if (person instanceof User) {
			for (AccountClaim ac : ((User) person).getAccountClaims()) {
				Resource r = ac.getResource();
				if (r instanceof EmailResource)
					resources.add(r);
				else if (r instanceof AimResource)
					resources.add(r);
				// we filter out any non-"primary" resources for now
			}
		} else if (person instanceof Contact) {
			for (ContactClaim cc : ((Contact) person).getResources()) {
				Resource r = cc.getResource();
				if (r instanceof EmailResource)
					resources.add(r);
				else if (r instanceof AimResource)
					resources.add(r);
				// we filter out any non-"primary" resources for now
			}
		} else {
			throw new IllegalArgumentException(
					"person is not User or Contact: " + person);
		}

		return resources;
	}

	private void initializePersonViewFallbacks(PersonView pv,
			Set<Resource> fallbackResources) {
		// we want to set a fallback name from an email resource if we
		// have one, since we won't disclose the resources and PersonView
		// ideally doesn't show as <Unknown> in the UI
		// we need to set a Guid for the PersonView as well, because if it
		// doesn't have a user set,
		// it relies on the id of the primary resource
		Guid emailGuid = null; // we prefer an email resource guid as the
								// identifying guid
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

	private String getAimPresenceKey() {
		try {
			return config.getPropertyNoDefault(HippoProperty.AIM_PRESENCE_KEY);
		} catch (PropertyNotFoundException pnfe) {
			logger.warn("Could not find HippoProperty.AIM_PRESENCE_KEY");
		}
		return null;
	}

	private void addPersonViewExtra(Viewpoint viewpoint, PersonView pv,
			Set<Resource> resources, PersonViewExtra e) {
		switch (e) {
		case INVITED_STATUS:
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
						invited = invitationSystem.hasInvited(
								(UserViewpoint) viewpoint, r);
						if (invited)
							break;
					}
					pv.addInvitedStatus(invited);
				}
			}
			break;
		case EXTERNAL_ACCOUNTS:
			if (pv.getUser() != null) {
				Set<ExternalAccountView> externals = externalAccounts
						.getExternalAccountViews(viewpoint, pv.getUser());
				externalAccounts.loadThumbnails(viewpoint, externals);
				pv.addExternalAccountViews(externals);
			} else {
				pv.addExternalAccountViews(new HashSet<ExternalAccountView>());
			}
			if (pv.getExternalAccountViews() == null)
				throw new IllegalStateException(
						"Somehow set null external accounts on PersonView");
			break;
		case CONTACT_STATUS:
			boolean isContact = false;

			if (pv.getContact() != null
					&& viewpoint.isOfUser(pv.getContact().getAccount()
							.getOwner())) {
				isContact = true;
			} else if (viewpoint instanceof UserViewpoint
					&& pv.getUser() != null) {
				isContact = identitySpider.isContact(viewpoint,
						((UserViewpoint) viewpoint).getViewer(), pv.getUser());
			}

			pv.setIsContactOfViewer(isContact);
			break;
		}
	}

	private void addPersonViewExtras(Viewpoint viewpoint, PersonView pv,
			Resource fromResource, PersonViewExtra... extras) {
		// we implement this in kind of a lame way right now where we always do
		// all the database work, even though we only return the requested
		// information to keep
		// other code honest
		Set<Resource> contactResources = null;
		Set<Resource> userResources = null;
		Set<Resource> resources = null;
		if (pv.getContact() != null) {
			Contact contact = pv.getContact();

			// contact should point to something...
			if (contact.getResources().isEmpty())
				logger.warn(
						"Problem in database: contact has no resources: {}",
						contact);

			// this returns only email/aim resources
			contactResources = getResourcesForPerson(contact);

			// logger.debug("Contact has owner {} viewpoint is {}",
			// contact.getAccount().getOwner(),
			// viewpoint != null ? viewpoint.getViewer() : null);

			// can only see resources if we're the system view or this is our
			// own
			// Contact
			boolean ownContact = viewpoint instanceof SystemViewpoint
					|| viewpoint.isOfUser(contact.getAccount().getOwner());

			if (!ownContact) {
				initializePersonViewFallbacks(pv, contactResources);
				// don't disclose
				contactResources = null;
			}
		}

		// can only get user resources if we are a contact of the user
		if (pv.getUser() != null
				&& identitySpider.isViewerSystemOrFriendOf(viewpoint, pv
						.getUser())) {
			userResources = getResourcesForPerson(pv.getUser());
			pv.setViewerIsContact(true);
		}

		// If it's not our own contact, contactResources should be null here
		if (contactResources != null) {
			resources = contactResources;
			if (userResources != null) {
				resources.addAll(userResources); // contactResources won't be
													// overwritten in case of
													// conflict
			}
		} else if (userResources != null) {
			resources = userResources;
		} else {
			if (fromResource != null) {
				// fromResource is not reflected in the PersonView only if the
				// viewer is
				// not a contact of the resource (findContactByResource did not
				// return any
				// results), so if we are resorting to fromResource here, we
				// must not
				// disclose the resource, and can only supply the fallbacks to
				// the
				// PersonView
				initializePersonViewFallbacks(pv, Collections
						.singleton(fromResource));
			}
			resources = Collections.emptySet();
		}

		pv.addAllResources(resources);

		for (PersonViewExtra e : extras) {
			addPersonViewExtra(viewpoint, pv, resources, e);
		}

		if (pv.getAim() != null) {
			pv.setAimPresenceKey(getAimPresenceKey());
		}
	}

	// Should the online status be an "extra"? it's relatively cheap to compute,
	// since it only involves in-memory data, but not completely free.
	private void initOnline(PersonView personView) {
		User user = personView.getUser();
		if (user != null
				&& PresenceService.getInstance().getPresence("/users",
						user.getGuid()) > 0)
			personView.setOnline(true);
	}

	/*
	 * Creates a simple PersonView object. Should not inititate any database
	 * queries.
	 */
	private PersonView constructPersonView(Viewpoint viewpoint,
			Contact contact, User user) {
		PersonView pv = new PersonView(contact, user);
		initOnline(pv);
		// given the viewpoint, set whether the view is of self
		if (user != null && viewpoint.isOfUser(pv.getUser())) {
			pv.setViewOfSelf(true);
		} else {
			pv.setViewOfSelf(false);
		}
		return pv;
	}

	public PersonView getPersonView(Viewpoint viewpoint, Person p,
			PersonViewExtra... extras) {
		if (viewpoint == null)
			throw new IllegalArgumentException("null viewpoint");
		if (p == null)
			throw new IllegalArgumentException("null person");

		Contact contact;
		User user;

		if (p instanceof User) {
			contact = null;
			user = (User) p;
		} else {
			contact = (Contact) p;
			user = identitySpider.getUser(p);
		}

		PersonView pv = constructPersonView(viewpoint, contact, user);
		addPersonViewExtras(viewpoint, pv, null, extras);

		return pv;
	}

	public PersonView getPersonView(Viewpoint viewpoint, Resource r,
			PersonViewExtra... extras) {
		User user = null;
		Contact contact = null;

		if (r instanceof Account) {
			user = ((Account) r).getOwner();
		} else {
			user = identitySpider.lookupUserByResource(viewpoint, r);
		}

		if (user == null) {
			if (viewpoint instanceof UserViewpoint) {
				try {
					contact = identitySpider.findContactByResource(
							((UserViewpoint) viewpoint).getViewer(), r);
				} catch (NotFoundException e) {
				}
			}
		}

		PersonView pv = constructPersonView(viewpoint, contact, user);
		addPersonViewExtras(viewpoint, pv, r, extras);

		return pv;
	}

	public PersonView getSystemView(User user, PersonViewExtra... extras) {
		return getPersonView(SystemViewpoint.getInstance(), user, extras);
	}

	private Query getContactQuery(Account account, boolean forCount) {
		return em.createQuery(
				"SELECT " + (forCount ? "count(c)" : "c")
						+ " FROM Contact c WHERE c.account = :account"
						+ (forCount ? "" : " ORDER BY c.id DESC"))
				.setParameter("account", account);
	}

	private Query getUserContactQuery(User user, boolean forCount) {
		return em
				.createQuery(
						"SELECT "
								+ (forCount ? "COUNT(DISTINCT owner.id)"
										: "DISTINCT owner")
								+ " FROM Contact c, ContactClaim cc, AccountClaim acctClaim, User owner WHERE "
								+ " c.account = :startAccount AND cc.contact = c AND cc.resource = acctClaim.resource "
								+ " AND acctClaim.owner = owner AND acctClaim.owner != :startOwner"
								+ (forCount ? ""
										: " ORDER BY upper(owner.nickname) ASC"))
				.setParameter("startOwner", user).setParameter("startAccount",
						user.getAccount());
	}

	private Query getContactsWithoutInvitesQuery(User user) {
		return em.createQuery(
						"SELECT DISTINCT cc.resource "
								+ " FROM Contact c, ContactClaim cc "
								+ " WHERE c.account = :userAccount AND cc.contact = c "
								+ " AND cc.resource NOT IN (SELECT ac.resource FROM AccountClaim ac) "
								+ " AND cc.resource NOT IN (SELECT it.invitee from InvitationToken it, InviterData ivd "
								+ "                         WHERE ivd.inviter = :user AND ivd.deleted = 0 AND ivd.invitation = it)")
				.setParameter("user", user)
				.setParameter("userAccount", user.getAccount());
	}

	public int getContactCount(Viewpoint viewpoint, User user) {
		if (!identitySpider.isViewerSystemOrFriendOf(viewpoint, user))
			return 0;
		return ((Number) getContactQuery(user.getAccount(), true)
				.getSingleResult()).intValue();
	}

	public int getUserContactCount(Viewpoint viewpoint, User user) {
		if (!identitySpider.isViewerSystemOrFriendOf(viewpoint, user))
			return 0;
		return ((Number) getUserContactQuery(user, true).getSingleResult())
				.intValue();
	}

	private interface ContactViewer {
		public PersonView view(Contact c);
	}

	private List<PersonView> viewContacts(User user, List<Contact> contacts,
			int max, ContactViewer viewerFunction) {
		List<PersonView> result = new ArrayList<PersonView>();
		Set<Guid> seen = new HashSet<Guid>();
		for (Contact c : contacts) {
			if (max >= 0 && result.size() == max)
				break;

			PersonView pv = viewerFunction.view(c);

			// Note that this uniquification is only among a single page of the
			// results and we also don't make any effort to get more results to
			// fill up for results removed because uniquification, but hopefully
			// one user having another user in their contact list multiple
			// times will be rare; it can happen because of account claims that
			// are added after the creation of contact entries.
			if (seen.contains(pv.getIdentifyingGuid()))
				continue;
			seen.add(pv.getIdentifyingGuid());

			// there are various ways to get yourself in your own contact list;
			// we strip such things out. The caller can add them back if they
			// need them
			if (pv.getUser() != null && pv.getUser().equals(user))
				continue;

			result.add(pv);
		}
		return result;
	}

	private List<PersonView> viewContacts(final Viewpoint viewpoint, User user,
			List<Contact> contacts, int max, final PersonViewExtra... extras) {
		return viewContacts(user, contacts, max, new ContactViewer() {
			public PersonView view(Contact c) {
				return getPersonView(viewpoint, c, extras);
			}
		});
	}

	public List<PersonView> getContacts(Viewpoint viewpoint, User user,
			int start, int max, PersonViewExtra... extras) {

		if (!identitySpider.isViewerSystemOrFriendOf(viewpoint, user))
			return Collections.emptyList();

		// ORDER BY c.id DESC is an approximation "most recently added contacts
		// first".
		// To replace this ordering by an alphabetical ordering, we'd need to
		// denormalize
		// and store the display name of the contact in the Contact object; this
		// isn't
		// hard to do but it is a little tricky to keep updated.
		Query q = getContactQuery(user.getAccount(), false);
		q.setFirstResult(start);
		if (max >= 0)
			q.setMaxResults(max + 1); // Result might have ourself in it
		return viewContacts(viewpoint, user, TypeUtils.castList(Contact.class,
				q.getResultList()), max, extras);
	}

	public void pageContacts(Viewpoint viewpoint, User user,
			Pageable<PersonView> pageable, PersonViewExtra... extras) {
		pageable.setResults(getContacts(viewpoint, user, pageable.getStart(),
				pageable.getCount(), extras));
		// the total for getContacts is the count of all contacts, not just the
		// user contacts
		pageable.setTotalCount(LiveState.getInstance().getLiveUser(
				user.getGuid()).getContactsCount());
	}

	public List<PersonView> getUserContactsAlphaSorted(Viewpoint viewpoint,
			User user, int start, int max) {
		if (!identitySpider.isViewerSystemOrFriendOf(viewpoint, user))
			return Collections.emptyList();

		Query q = getUserContactQuery(user, false);
		q.setFirstResult(start);
		if (max >= 0)
			q.setMaxResults(max);
		List<PersonView> viewedContacts = new ArrayList<PersonView>();
		for (User result : TypeUtils.castList(User.class, q.getResultList())) {
			viewedContacts.add(getPersonView(viewpoint, result,
					PersonViewExtra.EXTERNAL_ACCOUNTS,
					PersonViewExtra.CONTACT_STATUS));
		}
		return viewedContacts;
	}

	public void pageUserContactsAlphaSorted(Viewpoint viewpoint, User user,
			Pageable<PersonView> pageable) {
		pageable.setResults(getUserContactsAlphaSorted(viewpoint, user,
				pageable.getStart(), pageable.getCount()));
		pageable.setTotalCount(getUserContactCount(viewpoint, user));
	}

	public List<PersonView> getContactsWithoutInvites(Viewpoint viewpoint, User user) {
		if (!viewpoint.isOfUser(user))
			return Collections.emptyList();

		Query q = getContactsWithoutInvitesQuery(user);
		List<PersonView> viewedContacts = new ArrayList<PersonView>();
		for (Resource resource : TypeUtils.castList(Resource.class, q
				.getResultList())) {
			viewedContacts.add(getPersonView(viewpoint, resource));
		}
		return viewedContacts;
	}

	public Set<PersonView> getFollowers(Viewpoint viewpoint, User user,
			PersonViewExtra... extras) {

		Set<PersonView> followers = new HashSet<PersonView>();
		if (!(viewpoint.isOfUser(user) || viewpoint instanceof SystemViewpoint)) {
			// can only see followers if self
			return followers;
		}

		LiveState liveState = LiveState.getInstance();
		Set<Guid> contacts = liveState.getContacts(user.getGuid());
		Set<Guid> contacters = liveState.getContacters(user.getGuid());

		for (Guid guid : contacters) {
			if (!contacts.contains(guid)) {
				User follower = em.find(User.class, guid.toString());

				PersonView pv = getPersonView(viewpoint, follower, extras);
				followers.add(pv);
			}
		}

		return followers;
	}

	public Set<PersonView> getAllUsers(UserViewpoint viewpoint) {
		if (!identitySpider.isAdministrator(viewpoint.getViewer()))
			throw new RuntimeException("getAllUsers is admin-only");

		@SuppressWarnings("unchecked")
		List<User> users = em.createQuery("SELECT u FROM User u")
				.getResultList();
		Set<PersonView> result = new HashSet<PersonView>();
		for (User user : users) {
			result.add(getPersonView(SystemViewpoint.getInstance(), user));
		}

		return result;
	}

	public Set<PersonView> viewUsers(Viewpoint viewpoint, Set<User> users) {
		Set<PersonView> viewedUsers = new HashSet<PersonView>();
		for (User user : users) {
			viewedUsers.add(getPersonView(viewpoint, user));
		}
		return viewedUsers;
	}
}
