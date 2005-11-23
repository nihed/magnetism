package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.AimResource;
import com.dumbhippo.persistence.Contact;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;
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
	
	static class GuidNotFoundException extends Exception {
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
	
	/**
	 * Gets a Resource object for the given email address, creating
	 * it if necessary. Note that the result is a detached entity.
	 * 
	 * @param email the address
	 * @return a resource for the email
	 */
	public EmailResource getEmail(String email);
	
	/**
	 * This is an internal detail used in implementing getEmail(); getEmail
	 * adds retry. 
	 * 
	 * @param email the address
	 * @return a resource for the email
	 */
	public EmailResource findOrCreateEmail(String email);
	
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
	 * This is an internal detail used in implementing getAim(); getAim
	 * adds retry. 
	 * 
	 * @param screenName the address
	 * @return a resource for the address
	 */
	public AimResource findOrCreateAim(String screenName) throws ValidationException;
	
	/**
	 * Gets a Resource object for the given URL, creating
	 * it if necessary. Note that the result is a detached entity.
	 * 
	 * @param url the url
	 * @return a resource for the url
	 */
	public LinkResource getLink(String url);
	
	/**
	 * This is an internal detail used in implementing getLink(); getLink
	 * adds retry. 
	 * 
	 * @param url the rl
	 * @return a resource for the url
	 */
	public LinkResource findOrCreateLink(String url);
	
	/**
	 * Finds the unique person which owns an email address
	 * according to our system. i.e. this person has proved
	 * they own it.
	 * 
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */
	public User lookupUserByEmail(String email);
	
	//public Person lookupPersonByAim(String aim);
	//public Person lookupPersonByAim(Person viewpoint, String aim);

	/**
	 * Finds the unique person which owns a resource
	 * according to our system. i.e. this person has proved
	 * they own it.
	 * 
	 * @param resource the possibly-owned resource
	 * @return the owning person, or null if none
	 */
	public User lookupUserByResource(Resource resource);
	
	public <T extends GuidPersistable> T lookupGuidString(Class<T> klass, String id) throws ParseException, GuidNotFoundException;
	public <T extends GuidPersistable> T lookupGuid(Class<T> klass, Guid id) throws GuidNotFoundException;
	
	public <T extends GuidPersistable> Set<T> lookupGuidStrings(Class<T> klass, Set<String> ids) throws ParseException, GuidNotFoundException;
	public <T extends GuidPersistable> Set<T> lookupGuids(Class<T> klass, Set<Guid> ids) throws GuidNotFoundException;
	
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
	public Set<Contact> getRawContacts(User user);
	
	/** 
	 * Get the contacts of the given person as a list of PersonView
	 * @param viewpoint viewpoint person viewing the contacts (only
	 *          a user can see their contacts, so if viewpoint.getviewer()
	 *          doesn't match user, the result will be empty)    
	 * @param user who to get contacts of
	 * @param extras info to stuff into the PersonView objects
	 * @return their contacts
	 */
	public Set<PersonView> getContacts(Viewpoint viewpoint, User user, PersonViewExtra... extras);
	
	/**
	 * Checks whether a person has another other as a contact
	 * @param current viewpoint (only a user can see their contacts, 
	 *          so if viewpoint.getviewer() doesn't match user, the result will 
	 *          be false) 
	 * @param user who to look in the contacts of
	 * @param contact person to look for in the contacts
	 */
	public boolean isContact(Viewpoint viewpoint, User user, Person contact);
	
	/**
	 * The Man is an internal person who we use for various nefarious purposes.
	 *
	 * (More helpfully: The Man is the system user; his opinions 
	 * are taken as true for everyone)
	 * 
	 * Note that the result is a detached entity.
	 * 
	 * This function is going away, along with theMan. 
	 *
	 * @return The Man
	 */
	public User getTheMan();
	
	/**
	 * This is an internal detail used in implementing getTheMan(); getTheMan
	 * adds caching and retry. 
	 * 
	 * @return The Man
	 */
	public User findOrCreateTheMan();
	
	/**
	 * Returns an object describing a person from the viewpoint of another person.
	 * 
	 * @param viewpoint the viewpoint of the person who is viewing
	 * @param p the person being viewed
	 * @param extras information to stuff into the PersonView, more = more database work
	 * @return a new PersonView object
	 */
	public PersonView getPersonView(Viewpoint viewpoint, Person p, PersonViewExtra... extras);
	
	/**
	 * Returns an object describing a person from the viewpoint of another person,
	 *  given a resource owned by the person. Note that the returned person view 
	 *  does NOT treat this resource specially and may not even contain the resource
	 *  you passed in; the resource is just a "search handle"
	 * 
	 * @param viewpoint the viewpoint of the person who is viewing
	 * @param resource the person being viewed
	 * @param extras information to stuff into the PersonView, more = more database work
	 * @return a new PersonView object
	 */
	public PersonView getPersonView(Viewpoint viewpoint, Resource resource, PersonViewExtra... extras);
	
	/**
	 * 
	 * Returns an object describing a person from the global viewpoint.
	 * 
	 * @param user the person being viewed. Always an account holder
	 * @param extras information to stuff into the PersonView, more = more database work
	 * @return new PersonView object
	 */
	public PersonView getSystemView(User user, PersonViewExtra... extras);
	
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
}
