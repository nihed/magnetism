package com.dumbhippo.server.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.annotation.IgnoreDependency;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.TypeFilteredCollection;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.live.PresenceService;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AccountClaim;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.ContactClaim;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountSystem;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.PersonViewer;
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
	static private final Logger logger = GlobalSetup.getLogger(PersonViewer.class);
	
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
			throw new IllegalArgumentException("person is not User or Contact: " + person);
		}

		return resources;
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
		if (pv.getUser() != null && identitySpider.isViewerSystemOrFriendOf(viewpoint, pv.getUser())) {
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
	
	// Should the online status be an "extra"? it's relatively cheap to compute,
	// since it only involves in-memory data, but not completely free.
	private void initOnline(PersonView personView) {
		User user = personView.getUser();
		if (user != null && PresenceService.getInstance().getPresence("/users", user.getGuid()) > 0)
			personView.setOnline(true);
	}
	
	public PersonView getPersonView(Viewpoint viewpoint, Person p, PersonViewExtra... extras) {
		if (viewpoint == null)
			throw new IllegalArgumentException("null viewpoint");
				
		Contact contact = p instanceof Contact ? (Contact) p : null;
		User user = identitySpider.getUser(p); // user for contact, or p itself if it's already a user
		
		PersonView pv = new PersonView(contact, user);
				
		addPersonViewExtras(viewpoint, pv, null, extras);
		initOnline(pv);
		
		return pv;
	}

	public PersonView getPersonView(Viewpoint viewpoint, Resource r, PersonViewExtra firstExtra, PersonViewExtra... extras) {
		User user;
		Contact contact;
		
		if (viewpoint instanceof UserViewpoint)
			contact = identitySpider.findContactByResource(((UserViewpoint)viewpoint).getViewer(), r);
		else
			contact = null;
		
		if (r instanceof Account) {
			user = ((Account)r).getOwner();
		} else {
			user = identitySpider.lookupUserByResource(viewpoint, r);
		}
		
		PersonView pv = new PersonView(contact, user);
		initOnline(pv);
		
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
		initOnline(pv);
		
		return pv;
	}

	public Set<PersonView> getContacts(Viewpoint viewpoint, User user,
			boolean includeSelf, PersonViewExtra... extras) {

		// there are various ways to get yourself in your own contact list;
		// we make includeSelf work in both cases (where you are or are not in
		// there)

		boolean sawSelf = false;
		Set<PersonView> result = new HashSet<PersonView>();

		if (viewpoint instanceof UserViewpoint) {
			UserViewpoint userViewpoint = (UserViewpoint)viewpoint;
		    userViewpoint.cacheAllFriendOfStatus(identitySpider.getUsersWhoHaveUserAsContact(viewpoint, userViewpoint.getViewer()));
		}

		for (Person p : identitySpider.getRawContacts(viewpoint, user)) {
			PersonView pv = getPersonView(viewpoint, p, extras);

			if (pv.getUser() != null && pv.getUser().equals(user)) {
				// FIXME the concept here (one contact displayed per User) could
				// be generalized, i.e. we should probably nuke all but one
				// PersonView
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

	public Set<PersonView> getFollowers(Viewpoint viewpoint, User user,
			PersonViewExtra... extras) {

		Set<PersonView> followers = new HashSet<PersonView>();
		if (!(viewpoint.isOfUser(user) || viewpoint instanceof SystemViewpoint)) {
			// can only see followers if self
			return followers;
		}

		// we are only interested in user contacts of self
		Set<User> rawContactsOfSelf = identitySpider.getRawUserContacts(viewpoint, user);
		List<Account> accounts = identitySpider.getAccountsWhoHaveUserAsContact(user);

		for (Account a : accounts) {
			User selfAContactOf = a.getOwner();
			if (!rawContactsOfSelf.contains(selfAContactOf)) {
				// this person is our follower, because we don't have them as a
				// contact!
				PersonView pv = getPersonView(viewpoint, selfAContactOf, extras);
				followers.add(pv);
			}
		}

		return followers;
	}	
	
	public Set<PersonView> getAllUsers(UserViewpoint viewpoint) {
		if (!identitySpider.isAdministrator(viewpoint.getViewer()))
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
