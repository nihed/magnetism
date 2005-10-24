package com.dumbhippo.server;

import java.util.Set;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.GuidPersistable;
import com.dumbhippo.persistence.HippoAccount;
import com.dumbhippo.persistence.LinkResource;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Resource;

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
	public Person lookupPersonByEmail(String email);
	
	/**
	 * Finds the person which owns an email address from a
	 * particular person's viewpoint.  
	 * 
	 * @param viewpoint
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */	
	public Person lookupPersonByEmail(Person viewpoint, String email);
		
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
	public Person lookupPersonByResource(Resource resource);
	
	/**
	 * Finds the person who owns a resource from a
	 * particular person's viewpoint. 
	 * 
	 * @param viewpoint
	 * @param resource the possibly-owned resource
	 * @return the owning person, or null if none
	 */	
	public Person lookupPersonByResource(Person viewpoint, Resource resource);
	
	public <T extends GuidPersistable> T lookupGuidString(Class<T> klass, String id) throws ParseException, GuidNotFoundException;
	public <T extends GuidPersistable> T lookupGuid(Class<T> klass, Guid id) throws GuidNotFoundException;
	
	public <T extends GuidPersistable> Set<T> lookupGuidStrings(Class<T> klass, Set<String> ids) throws ParseException, GuidNotFoundException;
	public <T extends GuidPersistable> Set<T> lookupGuids(Class<T> klass, Set<Guid> ids) throws GuidNotFoundException;
	
	/** 
	 * Add a claim by assertedBy that owner is the owner of the resource.
	 * For this call, the assertedBy can't be null or TheMan, we only 
	 * set those when we prove things ourselves.
	 *
	 * TODO should only permit assertedBy.equals(currentUser)
	 * 
	 * @param owner claimed owner
	 * @param resource thing to be owned
	 * @param assertedBy who is claiming it
	 */
	public void addOwnershipClaim(Person owner, Resource resource, Person assertedBy);
	
	/** 
	 * Record an assertion that we have (at least weakly) verified the person has control of
	 * a particular resource.  An example might be clicking on a link in an email 
	 * sent to an email address resource, or conversing with the person via IM.
	 * 
	 * @param owner claimed owner
	 * @param resource thing to be owned
	 */	
	public void addVerifiedOwnershipClaim(Person owner, Resource res);
	
	/**
	 * If the resource already has an account holder who has a verified
	 * claim on it, returns the Person who owns the resource and adds them
	 * to the contacts of the passed-in person.
	 *
	 * Otherwise, creates a new Person, makes it own the given resource, and 
	 * adds the Person to the owner's contact list.
	 * 
	 * @param owner person whose contact it is (logged in user usually)
	 * @param contact the contact address
	 * @return the new person in the contact list
	 */
	public Person createContact(Person owner, Resource contact);
	
	/** 
	 * Get the contacts of the given person
	 * @param user who to get contacts of
	 * @return their contacts
	 */
	public Set<Person> getContacts(Person user);
	
	/**
	 * The Man is an internal person who we use for various nefarious purposes.
	 *
	 * (More helpfully: The Man is the system user; his opinions 
	 * are taken as true for everyone, e.g. in ResourceOwnershipClaim)
	 * 
	 * Note that the result is a detached entity.
	 *
	 * @return The Man
	 */
	public Person getTheMan();
	
	/**
	 * This is an internal detail used in implementing getTheMan(); getTheMan
	 * adds caching and retry. 
	 * 
	 * @return The Man
	 */
	public Person findOrCreateTheMan();
	
	/**
	 * Returns an object describing a person from the viewpoint of another person.
	 * 
	 * @param viewpoint the person who is viewing
	 * @param p the person being viewed
	 * @return a new PersonView object
	 */
	public PersonView getViewpoint(Person viewpoint, Person p);
	
	
	/**
	 * 
	 * Returns an object describing a person from the global viewpoint.
	 * 
	 * @param p the person being viewed
	 * @return new PersonView object
	 */
	public PersonView getSystemViewpoint(Person p); 
}
