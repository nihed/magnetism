package com.dumbhippo.server;

import java.net.URL;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.live.LiveUser;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.ValidationException;

/*
 * This class represents the interface to the "Identity Spider",
 * which includes the social network plus
 * an aggregation of user/group relations to resources like
 * email addresses and blogs. This is a public interface 
 * conceptually between the server model and any views
 * such as a web view or client view.
 * 
 * @author walters
 */
@Local
public interface IdentitySpider {
	
	
	/**
	 * Gets a Resource object for the given email address, creating
	 * it if necessary. Note that the result is a detached entity.
	 * 
	 * @param email the address
	 * @return a resource for the email
	 */
	public EmailResource getEmail(String email) throws ValidationException;
	
	/**
	 * Gets a Resource object for the given email address, only if it 
	 * already exists.
	 * 
	 * @param email the address
	 * @return a resource for the email or null
	 */
	public EmailResource lookupEmail(String email);	
	
	/**
	 * Gets a Resource object for the given AIM address, creating
	 * it if necessary. Note that the result is a detached entity.
	 * 
	 * Note that often if you do:
	 *    resource = identitySpider.getAim(screenName);
	 * then resource.getScreenName().equals(screenName) 
	 * will return false. This is because resources always
	 * have the normalized form for AIM addresses.
	 * 
	 * @param screenName the address
	 * @return a resource for the address
	 * @throws ValidationException if the AIM address is bogus
	 */
	public AimResource getAim(String screenName) throws ValidationException;
	
	/**
	 * Returns the AimResource for a given screen name, or null if there is none.
	 * Does not create a new AimResource if it's not there already.
	 * 
	 * @param screenName
	 * @return AimResource object, or null if it doesn't exist in the database
	 */
	public AimResource lookupAim(String screenName);
	
	/**
	 * Gets a Resource object for the given URL, creating
	 * it if necessary. Note that the result is a detached entity.
	 * 
	 * @param url the url
	 * @return a resource for the url
	 */
	public LinkResource getLink(URL url);
	
	/**
	 * Finds the unique person which owns an email address
	 * according to our system. i.e. this person has proved
	 * they own it.
	 * 
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */
	public User lookupUserByEmail(Viewpoint viewpoint, String email);

	public User lookupUserByAim(Viewpoint viewpoint, String aim);

	/**
	 * Finds the unique person which owns a resource
	 * according to our system. i.e. this person has proved
	 * they own it.
	 * 
	 * @param resource the possibly-owned resource
	 * @return the owning person, or null if none
	 */
	public User lookupUserByResource(Viewpoint viewpoint, Resource resource);
	
	/**
	 * Return the database User object associated with a LiveUser.
	 */
	public User lookupUser(LiveUser luser);
	
	/**
	 * Return the database User object for a GUID
	 */
	public User lookupUser(Guid guid);
	
	public <T extends GuidPersistable> T lookupGuidString(Class<T> klass, String id) throws ParseException, NotFoundException;
	public <T extends GuidPersistable> T lookupGuid(Class<T> klass, Guid id) throws NotFoundException;
	
	public <T extends GuidPersistable> Set<T> lookupGuidStrings(Class<T> klass, Set<String> ids) throws ParseException, NotFoundException;
	public <T extends GuidPersistable> Set<T> lookupGuids(Class<T> klass, Set<Guid> ids) throws NotFoundException;
	
	/**
	 * Record an assertion that we have (at least weakly) verified the person has control of
	 * a particular resource.  An example might be clicking on a link in an email 
	 * sent to an email address resource, or conversing with the person via IM.
	 * 
	 * @param owner claimed owner
	 * @param resource thing to be owned
	 */	
	public void addVerifiedOwnershipClaim(User owner, Resource res);
	
	/** 
	 * Remove the verification of this ownership claim.
	 * 
	 * @param owner the owner in the claim
	 * @param res the resource they currently may own
	 */
	public void removeVerifiedOwnershipClaim(UserViewpoint viewpoint, User owner, Resource res);
	
	/**
	 * Retrieve the list of accounts which have the given user as a contact.
	 * 
	 * @param user user for whom returned accounts have a contact
	 * @return list of accounts
	 */
	public List<Account> getAccountsWhoHaveUserAsContact(User user);	
	
	
	public Contact findContactByResource(User owner, Resource resource);	
	
	/**
	 * If the person has a Contact with a resource sharing a (system-verified)
	 * owner with resource, adds resource to that contact and returns the
	 * Contact.
	 * 
	 * Otherwise, creates a new Contact, makes it own the given resource, and 
	 * adds the Person to the owner's contact list.
	 * 
	 * @param person person whose contact it is (logged in user usually)
	 * @param contact the contact address
	 * @return the new person in the contact list
	 */
	public Contact createContact(User user, Resource resource);
	
	/**
	 * Add a contact to a person's account. 
	 * 
	 * @param user whose contact it is
	 * @param contactPerson the new person to add to the contact list
	 */
	public void addContactPerson(User user, Person contactPerson);
	
	/**
	 * Remove a contact from a person's account. 
	 * 
	 * @param user whose contact it is
	 * @param contactPerson the person to remove from the contact list
	 */
	public void removeContactPerson(User user, Person contactPerson);

	/** 
	 * Get the contacts of the given person
	 * @param user who to get contacts of
	 * @return their contacts
	 */
	public Set<Contact> getRawContacts(Viewpoint viewpoint, User user);
	
	/** 
	 * Get the contacts of a given person who have an account; including the person themselves
	 * @param user who to get contacts of
	 * @return their friends
	 * 
	 */
	public Set<User> getRawUserContacts(Viewpoint viewpoint, User user);
	
	public Set<User> getUsersWhoHaveUserAsContact(Viewpoint viewpoint, User user);
	
	/**
	 * Checks whether a person has another other as a contact
	 * This function will create a live user for the user, unless the user is same as the viewer,
	 * consider whether this is efficient in terms of performance, and change the behavior of the
	 * function or what information is initialized when a live user is created if not.
	 *  
	 * @param current viewpoint (only the system, the user or one of their contacts can see user's contacts, 
	 *                           so if viewpoint isn't for one of those, the result will be false) 
	 * @param user who to look in the contacts of
	 * @param contact person to look for in the contacts
	 * @return true is we could look at user's contacts and supplied contact is their contact
	 */
	public boolean isContact(Viewpoint viewpoint, User user, User contact);
	
	/**
	 * Check whether viewpoint has the rights to see this users
	 * "friend stuff". (The viewpoint is the system view,point or the
	 * viewpoint of a contact of the viewed user or the viewed user themself.)
	 * @param viewpoint the currently logged-in user
	 * @param user the user we want to see if we're a friend of
	 * @return true if friendly
	 */
	public boolean isViewerSystemOrFriendOf(Viewpoint viewpoint, User user);
	

	/**
	 * Check whether viewpoint user as seen by this user is considered
	 * weird (marked as not desirable)
	 * @param viewpoint the currently logged-in user
	 * @param user the user we want to see if we're weird to
	 * @return true if weird
	 */
	public boolean isViewerWeirdTo(Viewpoint viewpoint, User user);
	
	public boolean isAdministrator(User user);	

	/**
	 * If person is a User, returns person. If it is an Contact
	 * and is associated with a user, return that user. Otherwise, 
	 * returns null.
	 * 
	 * @param the person
	 * @return the user the contact is associated with, or null
	 */
	public User getUser(Person person);
	
	/**
	 * Find the user associated with the resource by the system,
	 * if any.
	 * 
	 * @param the person
	 * @return the user the contact is associated with, or null
	 */
	public User getUser(Resource resource);
	
	/**
	 * Tries to find a good resource to use to represent person.
	 * If person is a User, returns the user's account. If the
	 * person is a Contact associated with a user, returns that
	 * user's account. Otherwise returns the resource that would
	 * be used to contact the user.
	 * 
	 * @param person a User or Contact
	 * @return a resource that can be used to represent person
	 */
	public Resource getBestResource(Person person);
	
	public boolean getAccountDisabled(User user);
	
	public void setAccountDisabled(User user, boolean disabled);

	public boolean getMusicSharingEnabled(User user, Enabled enabled);
	
	public void setMusicSharingEnabled(User user, boolean enabled);

	public boolean getMusicSharingPrimed(User user);
	
	public void setMusicSharingPrimed(User user, boolean primed);
	
	public boolean getNotifyPublicShares(User user);
	
	public void setNotifyPublicShares(User user, boolean notify);	

	/**
	 * Set a user's bio 
	 * 
	 * @param viewpoint who is setting
	 * @param user account user
	 * @param bio the user's bio
	 */
	public void setBio(UserViewpoint viewpoint, User user, String bio);

	/**
	 * Set a user's music bio 
	 * 
	 * @param viewpoint who is setting
	 * @param user account user
	 * @param bio the user's music bio
	 */
	public void setMusicBio(UserViewpoint viewpoint, User user, String bio);
	
	public void setStockPhoto(UserViewpoint viewpoint, User user, String photo);
	
	/**
	 * Return the set of all User contacts (Contacts which represent Users)
	 * that have set a MySpace name.
	 * 
	 * @param viewpoint viewpoint from which we gather contacts
	 * @return set of Users with MySpace names
	 */
	public Set<User> getMySpaceContacts(UserViewpoint viewpoint);
	
	/**
	 * Similar to getMySpaceContacts, but filtered by a particular name.
	 * 
	 * @param mySpaceName name to look for
	 * @return a set of Users claiming mySpaceName
	 */
	public Set<User> getUserContactsWithMySpaceName(UserViewpoint viewpoint, String mySpaceName); 	
	
	/**
	 * Increase the version number of the user; increasing the user version means
	 * any cached resources for the user (currently, just the headshot) are
	 * no longer valid and must be reloaded. 
	 * 
	 * You must call this function after the corresponding changes are committed;
	 * calling it first could result in stale versions of the resources being
	 * received and cached.
	 * 
	 * @param user the user to update
	 */
	public void incrementUserVersion(User user);;
}
