package com.dumbhippo.server;

import javax.ejb.Local;

import com.dumbhippo.persistence.EmailResource;
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
	public Person lookupPersonByEmail(EmailResource email);
	
	/**
	 * Finds the person which owns an email address from a
	 * particular person's viewpoint.  
	 * 
	 * @param viewpoint
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */	
	public Person lookupPersonByEmail(Person viewpoint, EmailResource email);
	
	/** 
	 * Finds a person by their database ID
	 * @param personId the id
	 * @return person or null if none
	 */
	public Person lookupPersonById(String personId);
	
	//public Person lookupPersonByAim(EmailResource email);
	//public Person lookupPersonByAim(Person viewpoint, EmailResource email);

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
