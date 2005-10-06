package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.EmailResource;
import com.dumbhippo.persistence.HippoAccount;
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
	
	
	/**
	 * Gets a Resource object for the given email address, creating
	 * it if necessary.
	 * 
	 * @param email the address
	 * @return a resource for the email
	 */
	public EmailResource getEmail(String email);
	
	/**
	 * Finds the unique person which owns an email address
	 * according to our system. i.e. this person has proved
	 * they own it.
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
	 * TODO I don't think anything that adds globally-visible stuff
	 * should be in these exported session interfaces; the exported
	 * session interface might be more like authLinkClicked(cookie),
	 * or something like that. Then we do the validation and 
	 * so forth and decide to set a globally-proven value.
	 * 
	 * @param email
	 * @return a new Person
	 */
//	public Person addPersonWithEmail(EmailResource email);
	
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
	 * Looks up an account by the Person it's associated with. 
	 * If this function returns non-null, then a Person is 
	 * registered with our system. If it returns null, then 
	 * a person is an implicit person we think is out there,
	 * but hasn't signed up.
	 * 
	 * @param person the person
	 * @return their account or null if they don't have one
	 */
	public HippoAccount lookupAccountByPerson(Person person);
	
	/** 
	 * Gets the number of active accounts.
	 * 
	 * @return number of active accounts
	 */
	public long getNumberOfActiveAccounts();
	
	/**
	 * Note that usernames change over time! i.e. the user can 
	 * modify their username. The persistent identity is the GUID 
	 * of the Person associated with an account. If you need a never-changing
	 * handle to someone, use their GUID, not their username.
	 * 
	 * @param username the username
	 * @return account for this username, or null
	 */
	public HippoAccount lookupAccountByUsername(String username);
		
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
	 * The Man is an internal person who we use for various nefarious purposes.
	 *
	 * (More helpfully: The Man is the system user; his opinions 
	 * are taken as true for everyone, e.g. in ResourceOwnershipClaim)
	 * 
	 * @return The Man
	 */
	public Person getTheMan();
	
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
