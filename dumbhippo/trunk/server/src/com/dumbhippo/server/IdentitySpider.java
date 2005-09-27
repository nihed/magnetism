package com.dumbhippo.server;

/*
 * This class represents the interface to the "Identity Spider",
 * an aggregation of user/group relations to resources like
 * email addresses and blogs.
 * @author walters
 */
public interface IdentitySpider {
	
	/**
	 * Finds the unique person which owns an email address.
	 * 
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */
	public Person lookupPersonByEmail(String email);

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
	public Person addPersonWithEmail(String email);
	
	/**
	 * Finds the person which owns an email address from a
	 * particular person's viewpoint.  
	 * 
	 * @param viewpoint
	 * @param email the possibly-owned email address
	 * @return the owning person, or null if none
	 */	
	public Person lookupPersonByEmail(Person viewpoint, String email);
	
	public Person lookupPersonByAim(String email);
	public Person lookupPersonByAim(Person viewpoint, String email);

	/**
	 * Return the best currently known human-readable identifier for a person.
	 * If a full name is known, return that.  Secondary possibilities include
	 * email addresses and AIM screen names.
	 * @param inviter
	 * @return
	 */
	public String getHumanReadableId(Person inviter);
	
	public String getEmailAddress(Person viewpoint, Person p);
	public String getEmailAddress(Person p);
}
