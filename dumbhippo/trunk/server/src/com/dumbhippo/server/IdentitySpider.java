package com.dumbhippo.server;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.Person;

/*
 * This class represents the interface to the "Identity Spider",
 * an aggregation of user/group relations to resources like
 * email addresses and blogs. This is a public interface 
 * conceptually between the server model and any views
 * such as a web view or client view.
 * 
 * @author walters
 */
public interface IdentitySpider {
	
	
	/**
	 * Gets a Resource object for the given email address, creating
	 * it if necessary.
	 * 
	 * @param email the address
	 * @return a resource for the email
	 */
	public EmailResource getEmail(String email);
	
	/**
	 * Finds the unique person which owns an email address.
	 * 
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */
	public Person lookupPersonByEmail(EmailResource email);

	/**
	 * Adds a new person to the identity spider, associated
	 * with the specified email address.  This relationship
	 * will be globally visible, and should have been weakly-verified
	 * by some means (e.g. the person clicked a link in an
	 * email address sent to them)
	 * 
	 * @param email
	 * @return a new Person
	 */
	public Person addPersonWithEmail(EmailResource email);
	
	/**
	 * Finds the person which owns an email address from a
	 * particular person's viewpoint.  
	 * 
	 * @param viewpoint
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */	
	public Person lookupPersonByEmail(Person viewpoint, EmailResource email);
	
	//public Person lookupPersonByAim(EmailResource email);
	//public Person lookupPersonByAim(Person viewpoint, EmailResource email);

	
	/**
	 * The Man is an internal person who we use for various nefarious purposes.
	 * 
	 * @return The Man
	 */
	public Person getTheMan();
	
	/**
	 * Returns an object describing a person from the viewpoint of another person.
	 * 
	 * @param viewpoint the person who is viewing
	 * @param p the person being viewed
	 * @return a new PersonViewBean object
	 */
	public PersonView getViewpoint(Person viewpoint, Person p);	
}
